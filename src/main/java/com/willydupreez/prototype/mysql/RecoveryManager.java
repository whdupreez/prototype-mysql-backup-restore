package com.willydupreez.prototype.mysql;

public interface RecoveryManager {

	void createDatabase();
	void dropDatabase();

	String backup(String filename);
	void restore(String backup);

}
