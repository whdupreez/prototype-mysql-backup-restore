package com.willydupreez.prototype.mysql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;

public class DefaultRecoveryManager implements RecoveryManager {

	private static class Builder {

		private static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
		private static final String MYSQL_BACKUP_COMMAND = "mysqldump";
		private static final String MYSQL_RESTORE_COMMAND = "mysql";

		private static final int DEFAULT_PORT = 3306;

		private static final String DROP_DATABASE = "DROP DATABASE %s;";
		private static final String CREATE_DATABASE = "CREATE DATABASE %s CHARACTER SET utf8 COLLATE utf8_general_ci;";

		private static final String TARGET_KEY = "target";
		private static final String SOURCE_KEY = "source";

		private RecoveryManager build(RecoveryProperties properties) {
			loadDriver();
			validateProperties(properties);
			return createRecoveryManager(properties);
		}

		private void loadDriver() {
			try {
				Class.forName(MYSQL_DRIVER);
			} catch (ClassNotFoundException e) {
				throw new RecoveryException("Failed to load driver: " + MYSQL_DRIVER);
			}
		}

		private void validateProperties(RecoveryProperties properties) {
			if (isNullOrWhitespace(properties.getUsername())) {
				throw new RecoveryException("No username provided");
			}
			if (isNullOrWhitespace(properties.getPassword())) {
				throw new RecoveryException("No password provided");
			}
			if (isNullOrWhitespace(properties.getHostname())) {
				throw new RecoveryException("No hostname provided");
			}
			if (isNullOrWhitespace(properties.getSchema())) {
				throw new RecoveryException("No schema provided");
			}
			if (isNullOrWhitespace(properties.getBackupPath())) {
				throw new RecoveryException("No backup path provided");
			}
		}

		private RecoveryManager createRecoveryManager(RecoveryProperties properties) {
			DefaultRecoveryManager recoveryManager = new DefaultRecoveryManager();

			recoveryManager.statementExecutor = new StatementExecutor();

			recoveryManager.username = properties.getUsername();
			recoveryManager.password = properties.getPassword();

			recoveryManager.backupPath = properties.getBackupPath();
			if (!recoveryManager.backupPath.endsWith("/")) {
				recoveryManager.backupPath = recoveryManager.backupPath + "/";
			}

			recoveryManager.backupCommand = properties.getBackupCommand() == null
					? MYSQL_BACKUP_COMMAND : properties.getBackupCommand();
			recoveryManager.restoreCommand = properties.getRestoreCommand() == null
					? MYSQL_RESTORE_COMMAND : properties.getRestoreCommand();

			recoveryManager.backupArguments = createBackupArguments(properties);
			recoveryManager.restoreArguments = createRestoreArguments(properties);

			int port = properties.getPort() == 0 ? DEFAULT_PORT : properties.getPort();
			recoveryManager.rootJdbcUrl = "jdbc:mysql://" + properties.getHostname() + ":" + port;
			recoveryManager.schemaJdbcUrl = "jdbc:mysql://" + properties.getHostname() + ":" + port + "/" + properties.getSchema();

			recoveryManager.createDatabaseSql = String.format(CREATE_DATABASE, properties.getSchema());
			recoveryManager.dropDatabaseSql = String.format(DROP_DATABASE, properties.getSchema());

			return recoveryManager;
		}

		private String[] createBackupArguments(RecoveryProperties properties) {
			return new String[] {
					"-u" + properties.getUsername(),
					"-p" + properties.getPassword(),
					"-h" + properties.getHostname(),
					"--add-drop-table",
					"-r",
					"${" + TARGET_KEY + "}",
					properties.getSchema()
			};
		}

		private String[] createRestoreArguments(RecoveryProperties properties) {
			return new String[] {
					"-u" + properties.getUsername(),
					"-p" + properties.getPassword(),
					"-h" + properties.getHostname(),
					properties.getSchema(),
					"-e",
					"source " + "${" + SOURCE_KEY + "}"
			};
		}

		private boolean isNullOrWhitespace(String str) {
			return str == null || str.trim().length() == 0;
		}

	}

	public static RecoveryManager create(RecoveryProperties properties) {
		return new Builder().build(properties);
	}

	private String username;
	private String password;

	private String backupPath;
	private String backupCommand;
	private String[] backupArguments;

	private String restoreCommand;
	private String[] restoreArguments;

	private String rootJdbcUrl;
	private String schemaJdbcUrl;

	private String createDatabaseSql;
	private String dropDatabaseSql;

	private StatementExecutor statementExecutor;

	private DefaultRecoveryManager() {
	}

	@Override
	public String backup(String filename) {

		String targetFile = backupPath + filename;

		int exit;
		try {

			CommandLine command = new CommandLine(backupCommand);
			Map<String, String> map = new HashMap<>();
			map.put(Builder.TARGET_KEY, backupPath + filename);
			command.setSubstitutionMap(map);
			command.addArguments(backupArguments);

			System.out.println("Executing backup command: " + command.toString());

			exit = executeCommand(command);

		} catch (IOException e) {
			throw new RecoveryException("Failed to backup to file: " + targetFile, e);
		}

		if (exit != 0) {
			throw new RecoveryException("Failed to backup to file with exit code [" + exit + "]: " + targetFile);
		}

		return targetFile;
	}

	@Override
	public void restore(String backupFile) {

		int exit;
		try {

			CommandLine command = new CommandLine(restoreCommand);
			Map<String, String> map = new HashMap<>();
			map.put(Builder.SOURCE_KEY, backupPath + backupFile);
			command.setSubstitutionMap(map);
			command.addArguments(restoreArguments);

			System.out.println("Executing restore command: " + command.toString());

			exit = executeCommand(command);

		} catch (IOException e) {
			throw new RecoveryException("Failed to restore from file: " + backupFile, e);
		}

		if (exit != 0) {
			throw new RecoveryException("Failed to backup to file with exit code [" + exit + "]: " + backupFile);
		}

	}

	private int executeCommand(CommandLine command) throws ExecuteException, IOException {
		DefaultExecutor executor = new DefaultExecutor();
		executor.setStreamHandler(new PumpStreamHandler());
		return executor.execute(command);
	}

	public void createDatabase() {
		try (Connection connection = DriverManager.getConnection(rootJdbcUrl, username, password)) {
			statementExecutor.executeUpdate(connection, createDatabaseSql);
		} catch (SQLException e) {
			throw new RecoveryException("Failed to create database: " + createDatabaseSql, e);
		}
	}

	public void dropDatabase() {
		try (Connection connection = DriverManager.getConnection(schemaJdbcUrl, username, password)) {
			statementExecutor.executeUpdate(connection, dropDatabaseSql);
		} catch (SQLException e) {
			throw new RecoveryException("Failed to drop database: " + dropDatabaseSql, e);
		}
	}

}
