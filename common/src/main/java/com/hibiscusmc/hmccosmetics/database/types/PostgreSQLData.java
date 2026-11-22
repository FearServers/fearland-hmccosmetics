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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Shared PostgreSQL storage (HikariCP). Writes go through one single thread, so they reach the database in the order
 * they were requested; the row is serialized on the caller's thread (the Bukkit one) before it is queued.
 * A failed read completes exceptionally (never an empty {@link UserData}), so no user is created and nothing is
 * saved over the stored equipment.
 */
public class PostgreSQLData extends Data {

    private static final int READ_ATTEMPTS = 3;
    private static final long READ_RETRY_MS = 1500;

    private HikariDataSource pool;
    private ExecutorService writer;
    private ExecutorService reader;

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
        pool = new HikariDataSource(config);
        writer = Executors.newSingleThreadExecutor(r -> new Thread(r, "HMCCosmetics-db-write"));
        reader = Executors.newCachedThreadPool(r -> new Thread(r, "HMCCosmetics-db-read"));

        try (Connection c = pool.getConnection(); PreparedStatement ps = c.prepareStatement(
                "CREATE TABLE IF NOT EXISTS cosmeticdatabase (uuid uuid PRIMARY KEY, cosmetics text NOT NULL, updated_at timestamptz NOT NULL DEFAULT now())")) {
            ps.execute();
        } catch (SQLException e) {
            HMCCosmeticsPlugin.getInstance().getLogger().severe("PostgreSQL table setup failed (" + e.getMessage() + "); reads will retry.");
        }
    }

    @Override
    public void save(CosmeticUser user) {
        final UUID id = user.getUniqueId();
        final String serialized = serializeData(user);
        writer.execute(() -> {
            try (Connection c = pool.getConnection(); PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO cosmeticdatabase (uuid, cosmetics, updated_at) VALUES (?, ?, now()) "
                            + "ON CONFLICT (uuid) DO UPDATE SET cosmetics = EXCLUDED.cosmetics, updated_at = now()")) {
                ps.setObject(1, id);
                ps.setString(2, serialized);
                ps.executeUpdate();
            } catch (SQLException e) {
                HMCCosmeticsPlugin.getInstance().getLogger().severe("Could not save cosmetics of " + id + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<UserData> get(UUID uniqueId) {
        return CompletableFuture.supplyAsync(() -> {
            SQLException last = null;
            for (int attempt = 1; attempt <= READ_ATTEMPTS; attempt++) {
                try (Connection c = pool.getConnection(); PreparedStatement ps = c.prepareStatement(
                        "SELECT cosmetics FROM cosmeticdatabase WHERE uuid = ?")) {
                    ps.setObject(1, uniqueId);
                    UserData data = new UserData(uniqueId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) data.setCosmetics(deserializeData(rs.getString("cosmetics")));
                    }
                    return data;
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
        writer.execute(() -> {
            try (Connection c = pool.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM cosmeticdatabase WHERE uuid = ?")) {
                ps.setObject(1, uniqueId);
                ps.executeUpdate();
            } catch (SQLException e) {
                HMCCosmeticsPlugin.getInstance().getLogger().severe("Could not clear cosmetics of " + uniqueId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public void close() {
        if (writer == null) return;
        writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                HMCCosmeticsPlugin.getInstance().getLogger().warning("Some cosmetic saves were still pending on shutdown.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        reader.shutdown();
        pool.close();
    }
}
