package rnd.dao.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.microsoft.sqlserver.jdbc.SQLServerDriver;

public class TestJDBC {

	public static void main(String[] args) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

		// String className = "org.hsqldb.jdbcDriver";
		// String url =
		// "jdbc:hsqldb:file:/home/vinodp/Data/lib/Perisistence/testdb";

		String className = SQLServerDriver.class.getName();
		String url = "jdbc:sqlserver://20.198.56.93:1433;DatabaseName=mydb;Username=sqlinst1;password=sqlinst1";

		introspectDB(className, url);

	}

	private static void introspectDB(String className, String url) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

		Connection conn = getConnnection(className, url);

		DatabaseMetaData dmd = conn.getMetaData();

		ResultSet schemasRS = dmd.getSchemas(null, "dbo");
		while (schemasRS.next()) {
			String schema = schemasRS.getString(1);
			System.out.print("\t\t" + schema);
			System.out.println();
			ResultSet tablesRS = dmd.getTables(null, schema, "User", new String[] { "TABLE", "VIEW" });
			int cc = printRSMDHeader(tablesRS.getMetaData());
			while (tablesRS.next()) {
				for (int i = 1; i <= cc; i++) {
					Object tableInfo = tablesRS.getObject(i);
					System.out.print("\t\t" + tableInfo);
				}
				System.out.println();
			}
		}

	}

	private static int printRSMDHeader(ResultSetMetaData rsmd) throws SQLException {
		int cc = rsmd.getColumnCount();
		for (int i = 1; i <= cc; i++) {
			System.out.print("\t\t" + rsmd.getColumnLabel(i));
		}
		System.out.println();
		return cc;
	}

	public static Connection getConnnection(String className, String url) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		DriverManager.registerDriver((Driver) Class.forName(className).newInstance());
		Connection conn = DriverManager.getConnection(url);
		return conn;
	}

}
