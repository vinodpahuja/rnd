package rnd.op;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

public class TestDNAP {

	public static void main(String[] args) {

		PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(TestDNAP.class.getClassLoader().getResourceAsStream("pmf.properties"));
		System.out.println(pmf);

		// DNAPObjectPersistor persistor = new DNAPObjectPersistor(pmf);

	}
}
