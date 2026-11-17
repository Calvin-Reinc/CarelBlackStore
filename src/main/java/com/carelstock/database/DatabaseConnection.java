/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.carelstock.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Pooled PostgreSQL connection manager backed by HikariCP.
 *
 * Configuration priority:
 *  1. Environment variables: DB_URL, DB_USER, DB_PASSWORD
 *  2. PaaS DATABASE_URL environment variable (postgres:// or postgresql://)
 *  3. Classpath resource: db.properties (db.url, db.user, db.password)
 *
 * @author squid
 */
public class DatabaseConnection {

    private static volatile HikariDataSource dataSource;

    private record DbCredentials(String url, String user, String password) {}

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private static HikariDataSource getDataSource() throws SQLException {
        HikariDataSource ds = dataSource;
        if (ds == null) {
            synchronized (DatabaseConnection.class) {
                ds = dataSource;
                if (ds == null) {
                    try {
                        ds = createDataSource();
                        dataSource = ds;
                    } catch (RuntimeException e) {
                        throw new SQLException("Failed to initialize database connection pool: " + e.getMessage(), e);
                    }
                }
            }
        }
        return ds;
    }

    private static HikariDataSource createDataSource() {
        DbCredentials creds = resolveCredentials();

        HikariConfig config = new HikariConfig();
        config.setPoolName("CarelStockHikariPool");
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(creds.url());
        config.setUsername(creds.user());
        config.setPassword(creds.password());

        // Pool sizing suited for Tomcat + managed free-tier PostgreSQL
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);   // 10s
        config.setIdleTimeout(300_000);        // 5m
        config.setMaxLifetime(1_500_000);      // 25m
        config.setKeepaliveTime(120_000);      // 2m (keeps cloud DB sockets alive)
        config.addDataSourceProperty("tcpKeepAlive", "true");

        return new HikariDataSource(config);
    }

    private static DbCredentials resolveCredentials() {
        // 1. Explicit environment variables
        String url = trimToNull(System.getenv("DB_URL"));
        String user = trimToNull(System.getenv("DB_USER"));
        String password = System.getenv("DB_PASSWORD");

        if (url != null && user != null && password != null) {
            return new DbCredentials(url, user, password);
        }

        // 2. Standard PaaS DATABASE_URL (Render, Railway, Neon, Heroku, etc.)
        String databaseUrl = trimToNull(System.getenv("DATABASE_URL"));
        if (databaseUrl != null) {
            return parseDatabaseUrl(databaseUrl);
        }

        // 3. Local fallback: src/main/resources/db.properties
        Properties props = loadProperties();
        String propUrl = trimToNull(props.getProperty("db.url"));
        String propUser = trimToNull(props.getProperty("db.user"));
        String propPassword = props.getProperty("db.password");

        if (propUrl != null && propUser != null && propPassword != null) {
            return new DbCredentials(propUrl, propUser, propPassword);
        }

        throw new IllegalStateException(
                "Database credentials not configured. Set DB_URL/DB_USER/DB_PASSWORD, DATABASE_URL, or src/main/resources/db.properties."
        );
    }

    private static DbCredentials parseDatabaseUrl(String rawUrl) {
        if (rawUrl.startsWith("jdbc:")) {
            // Already JDBC formatted without separate user/pass env vars isn't typical,
            // but fallback if credentials are embedded as URL params.
            throw new IllegalStateException("DATABASE_URL should use postgres:// or postgresql:// format, or use DB_URL/DB_USER/DB_PASSWORD.");
        }

        URI uri = URI.create(rawUrl);
        String userInfo = uri.getRawUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            throw new IllegalStateException("DATABASE_URL is missing username or password.");
        }

        String[] parts = userInfo.split(":", 2);
        String user = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
        String password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);

        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String path = uri.getPath() != null ? uri.getPath() : "";
        String query = uri.getRawQuery();

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost())
                .append(":")
                .append(port)
                .append(path);

        if (query != null && !query.isBlank()) {
            jdbcUrl.append("?").append(query);
        } else if (!"localhost".equalsIgnoreCase(uri.getHost()) && !"127.0.0.1".equals(uri.getHost())) {
            // Cloud PostgreSQL providers typically require SSL
            jdbcUrl.append("?sslmode=require");
        }

        return new DbCredentials(jdbcUrl.toString(), user, password);
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Handled by caller's validation
        }
        return props;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Closes the HikariCP pool and deregisters this webapp's JDBC driver on Tomcat shutdown/redeploy.
     */
    public static synchronized void closePool() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }

        ClassLoader webappClassLoader = DatabaseConnection.class.getClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == webappClassLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException ignored) {
                    // Best-effort cleanup on shutdown
                }
            }
        }
    }
}