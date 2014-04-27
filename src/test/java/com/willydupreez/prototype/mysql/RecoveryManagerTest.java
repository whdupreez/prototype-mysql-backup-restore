package com.willydupreez.prototype.mysql;

import org.junit.Before;
import org.junit.Test;

public class RecoveryManagerTest {

	private RecoveryManager recoveryManager;

	@Before
	public void before() {
		recoveryManager = DefaultRecoveryManager.create(properties());
		try {
			recoveryManager.dropDatabase();
		} catch (RecoveryException e) {
			// Ignore if database does not exist.
		}
		recoveryManager.createDatabase();
	}

	@Test
	public void testBackupRestore() {
		String filename = "test.sql";
		recoveryManager.restore(filename);
		recoveryManager.backup("backup-" + filename);
	}

	private RecoveryProperties properties() {

		RecoveryPropertiesBean props = new RecoveryPropertiesBean();
		props.setUsername("root");
		props.setPassword("Admin123");
		props.setHostname("localhost");
		props.setSchema("test");
		props.setBackupPath("target/test-classes");

		return props;
	}

}
