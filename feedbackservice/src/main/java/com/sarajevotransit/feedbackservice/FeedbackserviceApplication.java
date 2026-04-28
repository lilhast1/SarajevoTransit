package com.sarajevotransit.feedbackservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
public class FeedbackserviceApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(FeedbackserviceApplication.class);
	private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/feedbackdb";
	private static final String DEFAULT_DB_USERNAME = "postgres";
	private static final String DEFAULT_DB_PASSWORD = "mysecretpassword";
	private static final Pattern POSTGRES_JDBC_URL_PATTERN = Pattern.compile(
			"^jdbc:postgresql://(?<host>[^:/?]+)(:(?<port>\\d+))?/(?<database>[^?]+)(\\?.*)?$");

	public static void main(String[] args) {
		ensureDatabaseExists();
		SpringApplication.run(FeedbackserviceApplication.class, args);
	}

	private static void ensureDatabaseExists() {
		String configuredUrl = getEnvOrDefault("DB_URL", DEFAULT_DB_URL);
		if (!configuredUrl.startsWith("jdbc:postgresql://")) {
			LOGGER.debug("Automatic database creation skipped because DB_URL is not PostgreSQL: {}", configuredUrl);
			return;
		}

		Matcher matcher = POSTGRES_JDBC_URL_PATTERN.matcher(configuredUrl);
		if (!matcher.matches()) {
			throw new IllegalStateException("Unsupported DB_URL format: " + configuredUrl);
		}

		String host = matcher.group("host");
		String portGroup = matcher.group("port");
		int port = portGroup == null ? 5432 : Integer.parseInt(portGroup);
		String databaseName = matcher.group("database");

		String adminUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";
		String username = getEnvOrDefault("DB_USERNAME", DEFAULT_DB_USERNAME);
		String password = getEnvOrDefault("DB_PASSWORD", DEFAULT_DB_PASSWORD);

		try (Connection connection = DriverManager.getConnection(adminUrl, username, password)) {
			if (databaseExists(connection, databaseName)) {
				LOGGER.info("Database '{}' already exists.", databaseName);
				return;
			}

			String createSql = "CREATE DATABASE " + quoteIdentifier(databaseName);
			try (Statement statement = connection.createStatement()) {
				statement.execute(createSql);
			}
			LOGGER.info("Database '{}' was created.", databaseName);
		} catch (SQLException ex) {
			throw new IllegalStateException(
					"Unable to verify or create database '" + databaseName
							+ "'. Check PostgreSQL credentials and permissions.",
					ex);
		}
	}

	private static boolean databaseExists(Connection connection, String databaseName) throws SQLException {
		String sql = "SELECT 1 FROM pg_database WHERE datname = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, databaseName);
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next();
			}
		}
	}

	private static String quoteIdentifier(String identifier) {
		return '"' + identifier.replace("\"", "\"\"") + '"';
	}

	private static String getEnvOrDefault(String name, String defaultValue) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		return value;
	}

}
