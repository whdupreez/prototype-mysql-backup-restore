package com.willydupreez.prototype.mysql;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
			} else {
				File file = new File(properties.getBackupPath());
				if (!file.exists()) {
					throw new RecoveryException("Backup path does not exist: " + properties.getBackupPath());
				} else if (!file.isDirectory()) {
					throw new RecoveryException("Backup path is not a directory: " + properties.getBackupPath());
				} else if (!file.canRead() || !file.canWrite()) {
					throw new RecoveryException("Backup path read / write permissions not valid: " + properties.getBackupPath());
				}
			}
		}

		private RecoveryManager createRecoveryManager(RecoveryProperties properties) {
			DefaultRecoveryManager recoveryManager = new DefaultRecoveryManager();

			recoveryManager.statementExecutor = new StatementExecutor();

			recoveryManager.username = properties.getUsername();
			recoveryManager.password = properties.getPassword();
			recoveryManager.schema = properties.getSchema();

			recoveryManager.backupPath = getBackupPath(properties.getBackupPath());
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

		private String getBackupPath(String path) {
			String backupPath = new File(path).getAbsolutePath();
			return backupPath.endsWith("/") ? backupPath : backupPath + "/";
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
	private String schema;

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
	public void createDatabase() {
		try (Connection connection = DriverManager.getConnection(rootJdbcUrl, username, password)) {
			statementExecutor.executeUpdate(connection, createDatabaseSql);
		} catch (SQLException e) {
			throw new RecoveryException("Failed to create database: " + createDatabaseSql, e);
		}
	}

	@Override
	public void dropDatabase() {
		try (Connection connection = DriverManager.getConnection(schemaJdbcUrl, username, password)) {
			statementExecutor.executeUpdate(connection, dropDatabaseSql);
		} catch (SQLException e) {
			throw new RecoveryException("Failed to drop database: " + dropDatabaseSql, e);
		}
	}

	@Override
	public List<String> listBackups() {
		return Arrays.asList(new File(backupPath).listFiles((pathname) -> pathname.isFile() && pathname.getName().endsWith(".sql")))
				.stream()
				.map(file -> file.getName())
				.collect(Collectors.toList());
	}

	@Override
	public String getBackupPath() {
		return backupPath;
	}

	@Override
	public String backup(String tag) {

		String target = generateBackupFilename(tag);

		int exit;
		try {

			CommandLine command = new CommandLine(backupCommand);
			Map<String, String> map = new HashMap<>();
			map.put(Builder.TARGET_KEY, target);
			command.setSubstitutionMap(map);
			command.addArguments(backupArguments);

			System.out.println("Executing backup command: " + command.toString());

			exit = executeCommand(command);

		} catch (IOException e) {
			throw new RecoveryException("Failed to backup to file: " + target, e);
		}

		if (exit != 0) {
			throw new RecoveryException("Failed to backup to file with exit code [" + exit + "]: " + target);
		}

		return target;
	}

	private String generateBackupFilename(String tag) {
		// Format: {backupPath}/{schema}_yyyy-MM-dd_HH-mm-ss_{tag}.sql
		LocalDateTime localDateTime = LocalDateTime.now();
		return backupPath + schema + "_" + localDateTime.format(
				DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + "_" + tag + ".sql";
	}

	@Override
	public void restore(String source) {

		int exit;
		try {

			CommandLine command = new CommandLine(restoreCommand);
			Map<String, String> map = new HashMap<>();
			map.put(Builder.SOURCE_KEY, backupPath + source);
			command.setSubstitutionMap(map);
			command.addArguments(restoreArguments);

			System.out.println("Executing restore command: " + command.toString());

			exit = executeCommand(command);

		} catch (IOException e) {
			throw new RecoveryException("Failed to restore from file: " + source, e);
		}

		if (exit != 0) {
			throw new RecoveryException("Failed to backup to file with exit code [" + exit + "]: " + source);
		}

	}

	private int executeCommand(CommandLine command) throws ExecuteException, IOException {
		DefaultExecutor executor = new DefaultExecutor();
		executor.setStreamHandler(new PumpStreamHandler());
		return executor.execute(command);
	}

}
