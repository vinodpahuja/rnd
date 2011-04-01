package rnd.op;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import rnd.op.dnap.DNAPJDObjectPersistor;
import aa.server.User;

public class TestJPA {

	public static void main(String[] args) {

		PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(TestJPA.class.getClassLoader().getResourceAsStream("pmf.properties"));
		System.out.println(pmf);

		ObjectPersistor persistor = new DNAPJDObjectPersistor(pmf);

//		User user = new User();
//		user.setUserName("vinod.pahuja");

//		persistor.saveObject(user);
		
		User user = persistor.findObject(1L, User.class);
		System.out.println(user.getUserName());

	}
}
