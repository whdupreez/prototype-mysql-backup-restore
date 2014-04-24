package com.willydupreez.prototype.mysql;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

public class RecoveryManagerTest {

	@Test
	public void testBackupRestore() {
		String filename = "test.sql";
		new RecoveryManager(properties()).backup(filename);
		new RecoveryManager(properties()).restore(filename);
	}

	private RecoveryProperties properties() {

		boolean isWindows = SystemUtils.IS_OS_WINDOWS;

		RecoveryPropertiesBean props = new RecoveryPropertiesBean();
		props.setUsername("root");
		props.setPassword("Admin123");
		props.setHostname("localhost");
		props.setSchema("test");
		props.setBackupPath("target/");
		props.setBackupCommand(isWindows ? "mysqldump.exe" : "mysqldump");
		props.setRestoreCommand(isWindows ? "mysql.exe" : "mysql");

		return props;
	}

}
