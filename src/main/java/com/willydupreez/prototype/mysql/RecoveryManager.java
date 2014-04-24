package com.willydupreez.prototype.mysql;

import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

public class RecoveryManager implements BackupManager, RestoreManager {

	private RecoveryProperties properties;

	public RecoveryManager(RecoveryProperties properties) {
		this.properties = properties;
	}

	@Override
	public String backup(String filename) {

		String targetFile = properties.getBackupPath() + filename;

		int exit;
		try {
			CommandLine command = new CommandLine(properties.getBackupCommand())
					.addArgument("-u" + properties.getUsername())
					.addArgument("-p" + properties.getPassword())
					.addArgument("-h" + properties.getHostname())
					.addArgument("--add-drop-table")
					.addArgument("-r")
					.addArgument(targetFile)
					.addArgument(properties.getSchema());

			DefaultExecutor executor = new DefaultExecutor();
			executor.setStreamHandler(new PumpStreamHandler());

			exit = executor.execute(command);

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

			CommandLine command = new CommandLine(properties.getRestoreCommand())
					.addArgument("-u" + properties.getUsername())
					.addArgument("-p" + properties.getPassword())
					.addArgument("-h" + properties.getHostname())
					.addArgument(properties.getSchema())
					.addArgument("-e")
					.addArgument("source " + backupFile);

			exit = new DefaultExecutor().execute(command);

		} catch (IOException e) {
			throw new RecoveryException("Failed to restore from file: " + backupFile, e);
		}

		if (exit != 0) {
			throw new RecoveryException("Failed to backup to file with exit code [" + exit + "]: " + backupFile);
		}

	}

}
