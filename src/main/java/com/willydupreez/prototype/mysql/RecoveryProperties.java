package com.willydupreez.prototype.mysql;

public interface RecoveryProperties {

	String getUsername();
	String getPassword();
	String getSchema();
	String getHostname();
	int getPort();

	String getBackupPath();
	String getBackupCommand();

	String getRestoreCommand();

}
