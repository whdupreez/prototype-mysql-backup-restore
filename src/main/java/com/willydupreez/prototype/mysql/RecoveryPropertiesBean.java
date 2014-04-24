package com.willydupreez.prototype.mysql;

public class RecoveryPropertiesBean implements RecoveryProperties {

	private String username;
	private String password;
	private String hostname;
	private String schema;

	private String backupPath;
	private String backupCommand;

	private String restoreCommand;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getBackupPath() {
		return backupPath;
	}

	public void setBackupPath(String backupPath) {
		this.backupPath = backupPath;
	}

	public String getBackupCommand() {
		return backupCommand;
	}

	public void setBackupCommand(String backupCommand) {
		this.backupCommand = backupCommand;
	}

	public String getRestoreCommand() {
		return restoreCommand;
	}

	public void setRestoreCommand(String restoreCommand) {
		this.restoreCommand = restoreCommand;
	}

}
