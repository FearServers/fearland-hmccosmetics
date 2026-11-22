package com.hibiscusmc.hmccosmetics.config.section;

import lombok.Getter;
import me.lojosho.shaded.configurate.ConfigurationNode;

public class DatabaseSettings {

    private static final String DATABASE_TYPE_PATH = "type";
    private static final String MYSQL_DATABASE_SETTINGS = "mysql";

    private static final String MYSQL_DATABASE = "database";
    private static final String MYSQL_PASSWORD = "password";
    private static final String MYSQL_HOST = "host";
    private static final String MYSQL_USER = "user";
    private static final String MYSQL_PORT = "port";
    private static final String DELAY_PATH = "delay";
    private static final String ENABLE_DELAY = "enabled";
    private static final String DELAY_LENGTH = "delay";

    @Getter
    private static String databaseType;
    @Getter
    private static String database;
    @Getter
    private static String password;
    @Getter
    private static String host;
    @Getter
    private static String username;
    @Getter
    private static int port;
    @Getter
    private static String postgresHost;
    @Getter
    private static int postgresPort;
    @Getter
    private static String postgresDatabase;
    @Getter
    private static String postgresUsername;
    /** Config value, or the FEARLAND_API_DB_PASSWORD environment variable when the config one is empty. */
    @Getter
    private static String postgresPassword;
    @Getter
    private static int postgresPoolSize;
    @Getter
    private static boolean enabledDelay;
    @Getter
    private static int delayLength;

    public static void load(ConfigurationNode source) {
        databaseType = source.node(DATABASE_TYPE_PATH).getString();

        ConfigurationNode mySql = source.node(MYSQL_DATABASE_SETTINGS);

        database = mySql.node(MYSQL_DATABASE).getString();
        password = mySql.node(MYSQL_PASSWORD).getString();
        host = mySql.node(MYSQL_HOST).getString();
        username = mySql.node(MYSQL_USER).getString();
        port = mySql.node(MYSQL_PORT).getInt();

        ConfigurationNode pg = source.node("postgresql");
        postgresHost = pg.node("host").getString("localhost");
        postgresPort = pg.node("port").getInt(5432);
        postgresDatabase = pg.node("database").getString("fearland");
        postgresUsername = pg.node("user").getString("fearland");
        String pgPassword = pg.node("password").getString("");
        postgresPassword = pgPassword.isEmpty() ? System.getenv().getOrDefault("FEARLAND_API_DB_PASSWORD", "") : pgPassword;
        postgresPoolSize = pg.node("pool-size").getInt(3);

        ConfigurationNode delay = source.node(DELAY_PATH);

        enabledDelay = delay.node(ENABLE_DELAY).getBoolean(false);
        delayLength = delay.node(DELAY_LENGTH).getInt(2);
    }
}
