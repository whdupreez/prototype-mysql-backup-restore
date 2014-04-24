package com.willydupreez.prototype.mysql;

public interface RecoveryProperties {

	String getUsername();
	String getPassword();
	String getSchema();
	String getHostname();

	String getBackupPath();
	String getBackupCommand();

	String getRestoreCommand();

}
