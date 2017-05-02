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

			dbm = DatabaseManager.getUrlDefinedDatabaseManager("mydb", 10, SQLServerDriver.class.getName(), "jdbc:sqlserver://localhost:1433;DatabaseName=mydb", null, "dbo", "sqlinst1", "sqlinst1");

			// User me = new User();
			// me.setUserName("Me");
			// dbm.saveObject(me);

			// User me2 = new User();
			// me2.setUserId(11L);
			// dbm.loadObject(me2);
			// System.err.println("me2 is " + me2.getUserName());
			//
			User me3 = dbm.loadObject(User.class, "userid = ? ", 11L);
			System.err.println("me3 is " + me3.getUserName());

			me3.setUserName("Me2");
			dbm.saveObject(me3.getUserId(),me3);

			me3 = dbm.loadObject(User.class, "userid = ? ", 11L);
			System.err.println("me3 is " + me3.getUserName());

			// User me4 = dbm.loadObject(User.class, "userid = ? ", 13L);
			// System.err.println("me4 is " + me4.getUserName());
			//
			// dbm.deleteObject(new User(), "userid = ? ", 12L);
			// me4 = dbm.loadObject(User.class, "userid = ? ", 12L);
			// System.err.println("me4 is " + (me4 != null ? me4.getUserName() :
			// " deleted"));

		} finally {
			// also closes any open jpersist.Database
			dbm.close();
		}
	}

}