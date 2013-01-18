package rnd.op;

import java.util.logging.Level;

import jpersist.DatabaseManager;
import jpersist.JPersistException;
import aa.server.User;

import com.microsoft.sqlserver.jdbc.SQLServerDriver;

public class TestJPersist2 {

	public static void main(String[] args) throws JPersistException {
		DatabaseManager dbm = null;

		try {
			DatabaseManager.setLogLevel(Level.SEVERE);

			dbm = DatabaseManager
					.getUrlDefinedDatabaseManager("mydb", 10, SQLServerDriver.class.getName(), "jdbc:sqlserver://20.198.56.93:1433;DatabaseName=mydb", null, "dbo", "sqlinst1", "sqlinst1");

			// User me = new User();
			// me.setUserName("Me");
			// dbm.saveObject(me);

			User me2 = new User();
			me2.setUserId(11L);
			dbm.loadObject(me2);
			System.err.println("me2 is " + me2.getUserName());

			User me3 = dbm.loadObject(User.class, "userid = ? ", 11L);
			System.err.println("me3 is " + me3.getUserName());

		} finally {
			// also closes any open jpersist.Database
			dbm.close();
		}
	}

}