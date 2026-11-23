package com.hibiscusmc.hmccosmetics.database.types;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.section.DatabaseSettings;
import com.hibiscusmc.hmccosmetics.database.UserData;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Shared PostgreSQL storage (HikariCP). Writes go through one single thread, so they reach the database in the order
 * they were requested; the row is serialized on the caller's thread (the Bukkit one) before it is queued.
 * A failed read completes exceptionally (never an empty {@link UserData}), so no user is created and nothing is
 * saved over the stored equipment.
 * <p>
 * Concurrency between servers: every row has a {@code version}. A load remembers the version it read and a save only
 * replaces that exact version ({@code UPDATE ... WHERE version = ?}), so a late or retried write that was based on an
 * older state is discarded instead of overwriting what another server saved since.
 */
public class PostgreSQLData extends Data {

    private static final int READ_ATTEMPTS = 3;
    private static final long READ_RETRY_MS = 1500;
    private static final int WRITE_ATTEMPTS = 4;
    private static final long WRITE_RETRY_MS = 3000;

    private HikariDataSource pool;
    private ExecutorService writer;
    private ExecutorService reader;
    private Logger log;
    private long retryMs = WRITE_RETRY_MS;
    /** The row of each player as last read or written here: version 0 = no row. Only the writer thread changes a save's entry. */
    private final Map<UUID, Row> known = new ConcurrentHashMap<>();

    /** Players whose in-memory state lost a conflict: nothing more is written for them until their next load, so a queued save of the same stale state cannot overwrite what won. */
    private final Set<UUID> stale = ConcurrentHashMap.newKeySet();

    record Row(long version, String cosmetics) {}

    @Override
    public void setup() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + DatabaseSettings.getPostgresHost() + ":" + DatabaseSettings.getPostgresPort()
                + "/" + DatabaseSettings.getPostgresDatabase());
        config.setUsername(DatabaseSettings.getPostgresUsername());
        config.setPassword(DatabaseSettings.getPostgresPassword());
        config.setMaximumPoolSize(DatabaseSettings.getPostgresPoolSize());
        config.setPoolName("HMCCosmetics");
        config.setDriverClassName("org.postgresql.Driver"); // the driver lives in the plugin library loader, DriverManager cannot see it
        // Do not fail the plugin start when the database is down; reads retry and writes are logged.
        config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(5000);
        // A database that stops answering must fail a statement (logged, retried by the next save), not park the writer thread forever.
        config.addDataSourceProperty("connectTimeout", "5");
        config.addDataSourceProperty("socketTimeout", "15");
        init(new HikariDataSource(config), HMCCosmeticsPlugin.getInstance().getLogger());
    }

    /** Used by {@link #setup()} and by the tests (own pool and logger). */
    void init(HikariDataSource dataSource, Logger logger) {
        pool = dataSource;
        log = logger;
        writer = Executors.newSingleThreadExecutor(r -> new Thread(r, "HMCCosmetics-db-write"));
        reader = Executors.newCachedThreadPool(r -> new Thread(r, "HMCCosmetics-db-read"));

        try (Connection c = pool.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS cosmeticdatabase (uuid uuid PRIMARY KEY, cosmetics text NOT NULL, updated_at timestamptz NOT NULL DEFAULT now(), version bigint NOT NULL DEFAULT 1)");
            // Rows of the first release have no version; they start at 1 like a fresh row.
            st.execute("ALTER TABLE cosmeticdatabase ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 1");
        } catch (SQLException e) {
            log.severe("PostgreSQL table setup failed (" + e.getMessage() + "); reads will retry.");
        }
    }

    void setRetryMs(long ms) {
        retryMs = ms;
    }

    @Override
    public void save(CosmeticUser user) {
        saveSerialized(user.getUniqueId(), serializeData(user));
    }

    /** Queues the write of an already serialized row. Returns a future that completes when the write was applied, found stale, or gave up. */
    CompletableFuture<Void> saveSerialized(UUID id, String serialized) {
        return CompletableFuture.runAsync(() -> {
            // Retries stay on the single writer thread, so later saves of the same player still land after this one.
            for (int attempt = 1; attempt <= WRITE_ATTEMPTS; attempt++) {
                try {
                    write(id, serialized);
                    return;
                } catch (SQLException e) {
                    log.severe("Could not save cosmetics of " + id + " (attempt " + attempt + "/" + WRITE_ATTEMPTS + "): " + e.getMessage());
                    if (attempt == WRITE_ATTEMPTS) return;
                    try {
                        Thread.sleep(retryMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, writer);
    }

    private void write(UUID id, String serialized) throws SQLException {
        if (stale.contains(id)) {
            log.warning("Discarded a cosmetics save of " + id + ": an earlier save lost to a newer stored row and the player was not reloaded since.");
            return;
        }
        Row base = known.get(id);
        // Decided here, not when the save was requested: an earlier queued save may have changed what is known.
        if (base != null && base.cosmetics().equals(serialized)) return;
        try (Connection c = pool.getConnection()) {
            long baseVersion = base == null ? 0 : base.version();
            if (baseVersion == 0) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO cosmeticdatabase (uuid, cosmetics, updated_at, version) VALUES (?, ?, now(), 1) ON CONFLICT (uuid) DO NOTHING")) {
                    ps.setObject(1, id);
                    ps.setString(2, serialized);
                    if (ps.executeUpdate() == 1) {
                        known.put(id, new Row(1, serialized));
                        return;
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE cosmeticdatabase SET cosmetics = ?, updated_at = now(), version = version + 1 WHERE uuid = ? AND version = ?")) {
                    ps.setString(1, serialized);
                    ps.setObject(2, id);
                    ps.setLong(3, baseVersion);
                    if (ps.executeUpdate() == 1) {
                        known.put(id, new Row(baseVersion + 1, serialized));
                        return;
                    }
                }
            }
            // Not applied: the row moved on. Either an earlier attempt of this very write got through (a retry after a
            // lost answer), or another server saved something newer; the latter wins and this write is dropped.
            Row current = read(c, id);
            known.put(id, current);
            if (current.cosmetics().equals(serialized)) return;
            stale.add(id);
            log.warning("Discarded a stale cosmetics save of " + id + ": the stored row changed since it was loaded here (version " + baseVersion + " -> " + current.version() + ").");
        }
    }

    private static Row read(Connection c, UUID id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT cosmetics, version FROM cosmeticdatabase WHERE uuid = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Row(rs.getLong("version"), rs.getString("cosmetics")) : new Row(0, "");
            }
        }
    }

    @Override
    public CompletableFuture<UserData> get(UUID uniqueId) {
        return load(uniqueId).thenApply(row -> {
            UserData data = new UserData(uniqueId);
            data.setCosmetics(deserializeData(row.cosmetics()));
            return data;
        });
    }

    /** Reads the row (retrying), and remembers its version as the base of this server's next save. */
    CompletableFuture<Row> load(UUID uniqueId) {
        return CompletableFuture.supplyAsync(() -> {
            SQLException last = null;
            for (int attempt = 1; attempt <= READ_ATTEMPTS; attempt++) {
                try (Connection c = pool.getConnection()) {
                    Row row = read(c, uniqueId);
                    // Never move the base back: a save may have stored a newer version while this read was in flight.
                    known.merge(uniqueId, row, (old, fresh) -> fresh.version() >= old.version() ? fresh : old);
                    stale.remove(uniqueId);
                    return row;
                } catch (SQLException e) {
                    last = e;
                    if (attempt < READ_ATTEMPTS) {
                        try {
                            Thread.sleep(READ_RETRY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            throw new CompletionException("Could not read cosmetics of " + uniqueId, last);
        }, reader);
    }

    @Override
    public void clear(UUID uniqueId) {
        known.remove(uniqueId);
        stale.remove(uniqueId);
        writer.execute(() -> {
            try (Connection c = pool.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM cosmeticdatabase WHERE uuid = ?")) {
                ps.setObject(1, uniqueId);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.severe("Could not clear cosmetics of " + uniqueId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public void close() {
        if (writer == null) return;
        writer.shutdown();
        try {
            if (!writer.awaitTermination(30, TimeUnit.SECONDS)) {
                // The pool stays open: closing it under a running save would lose that save for sure.
                log.warning("Some cosmetic saves were still pending on shutdown.");
                reader.shutdown();
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        reader.shutdown();
        pool.close();
    }
}
