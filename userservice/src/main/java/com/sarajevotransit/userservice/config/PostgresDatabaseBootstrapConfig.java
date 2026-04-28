package com.sarajevotransit.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
@Slf4j
public class PostgresDatabaseBootstrapConfig {

    @Bean
    @Primary
    public DataSource dataSource(
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.url}") String jdbcUrl,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.username}") String username,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.password}") String password,
            @org.springframework.beans.factory.annotation.Value("${db.admin.username:${spring.datasource.username}}") String adminUsername,
            @org.springframework.beans.factory.annotation.Value("${db.admin.password:${spring.datasource.password}}") String adminPassword,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName) {

        if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql://")) {
            ensureDatabaseExists(
                    jdbcUrl,
                    adminUsername,
                    adminPassword);
        }

        return DataSourceBuilder.create()
                .driverClassName(driverClassName)
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
    }

    private void ensureDatabaseExists(String jdbcUrl, String username, String password) {
        PostgresConnectionInfo info;
        try {
            info = parseJdbcUrl(jdbcUrl);
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping database bootstrap due to unsupported JDBC URL: {}", jdbcUrl);
            return;
        }

        String adminUrl = buildAdminUrl(info);
        String databaseName = info.databaseName();

        try (Connection adminConnection = DriverManager.getConnection(adminUrl, username, password)) {
            adminConnection.setAutoCommit(true);
            if (databaseExists(adminConnection, databaseName)) {
                return;
            }

            String sql = "CREATE DATABASE \"" + databaseName.replace("\"", "\"\"") + "\"";
            try (Statement statement = adminConnection.createStatement()) {
                statement.execute(sql);
                log.info("Created PostgreSQL database '{}' on {}:{}.", databaseName, info.host(), info.port());
            }
        } catch (SQLException ex) {
            log.warn(
                    "Could not verify/create PostgreSQL database '{}'. "
                            + "Continuing with datasource initialization. Root cause: {}",
                    databaseName,
                    ex.getMessage());
        }
    }

    private boolean databaseExists(Connection adminConnection, String databaseName) throws SQLException {
        String sql = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (PreparedStatement statement = adminConnection.prepareStatement(sql)) {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private PostgresConnectionInfo parseJdbcUrl(String jdbcUrl) {
        String uriPart = jdbcUrl.substring("jdbc:".length());
        URI uri = URI.create(uriPart);

        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String path = uri.getPath();
        String dbName = path == null ? "" : path.replaceFirst("^/", "");

        if (host == null || host.isBlank() || dbName.isBlank()) {
            throw new IllegalArgumentException("JDBC URL host or database name is missing.");
        }

        return new PostgresConnectionInfo(host, port, dbName, uri.getQuery());
    }

    private String buildAdminUrl(PostgresConnectionInfo info) {
        String base = "jdbc:postgresql://" + info.host() + ":" + info.port() + "/postgres";
        if (info.query() == null || info.query().isBlank()) {
            return base;
        }
        return base + "?" + info.query();
    }

    private record PostgresConnectionInfo(String host, int port, String databaseName, String query) {
    }
}
