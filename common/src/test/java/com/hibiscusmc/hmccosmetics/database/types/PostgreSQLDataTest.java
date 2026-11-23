package com.hibiscusmc.hmccosmetics.database.types;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two "servers" (two PostgreSQLData instances) share one table. Needs HMC_TEST_PG_URL (jdbc url of a scratch
 * database), HMC_TEST_PG_USER and HMC_TEST_PG_PASSWORD; skipped without them. Runs in its own schema.
 */
class PostgreSQLDataTest {

    private static final String URL = System.getenv("HMC_TEST_PG_URL");
    private final String schema = "hmc_test_" + UUID.randomUUID().toString().replace("-", "");
    private final UUID player = UUID.randomUUID();
    private PostgreSQLData a;
    private PostgreSQLData b;

    private HikariDataSource pool() {
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl(URL + (URL.contains("?") ? "&" : "?") + "currentSchema=" + schema);
        c.setUsername(System.getenv("HMC_TEST_PG_USER"));
        c.setPassword(System.getenv("HMC_TEST_PG_PASSWORD"));
        c.setMaximumPoolSize(2);
        c.setInitializationFailTimeout(-1);
        return new HikariDataSource(c);
    }

    @BeforeEach
    void setUp() throws SQLException {
        Assumptions.assumeTrue(URL != null, "HMC_TEST_PG_URL not set");
        try (HikariDataSource p = new HikariDataSource(cfgNoSchema()); Connection c = p.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        }
        a = new PostgreSQLData();
        b = new PostgreSQLData();
        a.init(pool(), Logger.getLogger("a"));
        b.init(pool(), Logger.getLogger("b"));
        a.setRetryMs(50);
        b.setRetryMs(50);
    }

    private static HikariConfig cfgNoSchema() {
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl(URL);
        c.setUsername(System.getenv("HMC_TEST_PG_USER"));
        c.setPassword(System.getenv("HMC_TEST_PG_PASSWORD"));
        c.setMaximumPoolSize(1);
        return c;
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (a == null) return;
        a.close();
        b.close();
        try (HikariDataSource p = new HikariDataSource(cfgNoSchema()); Connection c = p.getConnection(); Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private String stored() throws SQLException {
        try (HikariDataSource p = pool(); Connection c = p.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT cosmetics FROM cosmeticdatabase WHERE uuid = '" + player + "'")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private void join(PostgreSQLData s) {
        s.load(player).orTimeout(10, TimeUnit.SECONDS).join();
    }

    private void save(PostgreSQLData s, String value) {
        s.saveSerialized(player, value).orTimeout(10, TimeUnit.SECONDS).join();
    }

    @Test
    void firstSaveCreatesRowAndLaterSavesUpdateIt() throws SQLException {
        join(a);
        save(a, "HELMET=top_hat");
        save(a, "HELMET=top_hat,BACKPACK=beehive");
        assertEquals("HELMET=top_hat,BACKPACK=beehive", stored());
    }

    @Test
    void lateSaveOfAnOlderServerDoesNotOverwriteANewerOne() throws SQLException {
        join(a);
        save(a, "HELMET=top_hat");           // lobby: equips
        join(b);                             // player moves to smp, loads what lobby saved
        save(b, "HELMET=dragon_hat");        // smp: newer change
        save(a, "HELMET=glacial_crown");     // lobby's delayed write, based on the older state
        assertEquals("HELMET=dragon_hat", stored());
    }

    @Test
    void delayedQuitSaveOfAnOldServerDoesNotOverwriteANewLoginsChange() throws SQLException {
        join(a);
        save(a, "HELMET=top_hat");
        join(b);
        save(b, "");                         // smp: player removed everything
        save(a, "HELMET=top_hat,BACKPACK=beehive"); // lobby quit save, written late
        assertEquals("", stored());
    }

    @Test
    void firstSaveOfTwoServersWithoutRowKeepsTheFirst() throws SQLException {
        join(a);
        join(b);                             // nobody has a row yet
        save(a, "HELMET=top_hat");
        save(b, "HELMET=dragon_hat");        // b still thinks there is no row
        assertEquals("HELMET=top_hat", stored());
    }

    @Test
    void savingBackToTheLoadedValueAfterAQueuedChangeIsNotSkipped() throws SQLException {
        join(a);
        save(a, "HELMET=top_hat");
        // change then revert queued back to back: the final row must be the reverted value
        var first = a.saveSerialized(player, "HELMET=dragon_hat");
        var second = a.saveSerialized(player, "HELMET=top_hat");
        first.join();
        second.join();
        assertEquals("HELMET=top_hat", stored());
    }

    @Test
    void retryAfterALostAnswerIsAcceptedNotDiscarded() throws SQLException {
        join(a);
        // The write reached the row but the answer was lost: row is at version+1 with our value, our base is old.
        try (HikariDataSource p = pool(); Connection c = p.getConnection(); Statement st = c.createStatement()) {
            st.execute("INSERT INTO cosmeticdatabase (uuid, cosmetics, version) VALUES ('" + player + "', 'HELMET=top_hat', 1)");
        }
        save(a, "HELMET=top_hat");
        assertEquals("HELMET=top_hat", stored());
        save(a, "HELMET=top_hat,BACKPACK=beehive"); // and the base is now current, so later saves work
        assertEquals("HELMET=top_hat,BACKPACK=beehive", stored());
    }

    @Test
    void rowsWithoutVersionColumnAreMigrated() throws SQLException {
        try (HikariDataSource p = pool(); Connection c = p.getConnection(); Statement st = c.createStatement()) {
            st.execute("DROP TABLE cosmeticdatabase");
            st.execute("CREATE TABLE cosmeticdatabase (uuid uuid PRIMARY KEY, cosmetics text NOT NULL, updated_at timestamptz NOT NULL DEFAULT now())");
            st.execute("INSERT INTO cosmeticdatabase (uuid, cosmetics) VALUES ('" + player + "', 'HELMET=top_hat')");
        }
        PostgreSQLData old = new PostgreSQLData();
        old.init(pool(), Logger.getLogger("old"));
        try {
            old.load(player).join();
            old.saveSerialized(player, "").join();
            assertEquals("", stored());
        } finally {
            old.close();
        }
    }

    @Test
    void queuedSavesAfterALostConflictAreAlsoDiscarded() throws SQLException {
        join(a);
        save(a, "HELMET=top_hat");
        join(b);
        save(b, "HELMET=dragon_hat");
        var first = a.saveSerialized(player, "HELMET=glacial_crown");   // stale: loses
        var second = a.saveSerialized(player, "HELMET=mechanic_hat");   // queued behind it from the same stale state
        first.join();
        second.join();
        assertEquals("HELMET=dragon_hat", stored());
        join(a);                                                         // reload: saving works again
        save(a, "HELMET=mechanic_hat");
        assertEquals("HELMET=mechanic_hat", stored());
    }

    @Test
    void staleServerCannotRecreateAClearedRow() throws SQLException {
        join(a);
        save(a, "HELMET=top_hat");
        join(b);
        b.clear(player);
        b.saveSerialized(player, "HELMET=x").join(); // flush the writer queue (clear ran before it)
        try (HikariDataSource p = pool(); Connection c = p.getConnection(); Statement st = c.createStatement()) {
            st.execute("DELETE FROM cosmeticdatabase WHERE uuid = '" + player + "'");
        }
        save(a, "HELMET=glacial_crown");
        save(a, "HELMET=mechanic_hat");
        assertEquals(null, stored());
    }
}
