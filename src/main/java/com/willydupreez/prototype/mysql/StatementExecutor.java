package com.willydupreez.prototype.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class StatementExecutor {

	public int executeUpdate(Connection connection, String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			return statement.executeUpdate(sql);
		}
	}

}
