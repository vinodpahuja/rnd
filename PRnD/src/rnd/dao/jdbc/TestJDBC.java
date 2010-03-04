package rnd.dao.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class TestJDBC {

	public static void main(String[] args) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

		String className = "org.hsqldb.jdbcDriver";
		// String url = "jdbc:hsqldb:hsql://localhost/xdb";
		String url = "jdbc:hsqldb:file:/home/vinodp/Data/lib/Perisistence/testdb";

		introspectDB(className, url);

	}

	private static void introspectDB(String className, String url) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		
		Connection conn = getConnnection(className, url);

		DatabaseMetaData dmd = conn.getMetaData();
		ResultSet schemas = dmd.getSchemas();
		ResultSetMetaData rsmd = schemas.getMetaData();

		int cc = rsmd.getColumnCount();
		System.out.println("colCnt" + cc);

		for (int i = 1; i <= cc; i++) {
			System.out.print("\t\t" + rsmd.getColumnLabel(i));
		}
		System.out.println();

		while (schemas.next()) {
			for (int i = 1; i <= cc; i++) {
				Object object = schemas.getObject(i);
				System.out.print("\t\t" + object);
			}
			System.out.println();
		}
	}

	public static Connection getConnnection(String className, String url) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		DriverManager.registerDriver((Driver) Class.forName(className).newInstance());
		Connection conn = DriverManager.getConnection(url);
		return conn;
	}

}
