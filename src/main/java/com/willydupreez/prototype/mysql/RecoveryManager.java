package com.willydupreez.prototype.mysql;

import java.util.List;

public interface RecoveryManager {

	void createDatabase();
	void dropDatabase();

	String getBackupPath();
	String backup(String tag);
	List<String> listBackups();

	void restore(String filename);

}
