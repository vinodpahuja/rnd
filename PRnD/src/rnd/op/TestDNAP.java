package rnd.op;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import rnd.op.dnap.DNAPObjectPersistor;
import aa.server.User;

public class TestDNAP {

	public static void main(String[] args) {

		PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(TestDNAP.class.getClassLoader().getResourceAsStream("pmf.properties"));
		System.out.println(pmf);

		ObjectPersistor persistor = new DNAPObjectPersistor(pmf);

		User user = new User();
		user.setUserName("vinod.pahuja");

		persistor.saveObject(user);

		// System.out.println(new User() instanceof PersistenceCapable);

	}
}
