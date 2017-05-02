package rnd.webapp.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

public interface Service extends RemoteService {
	/**
	 * Utility class for simplifing access to the instance of async service.
	 */
	public static class Util {
		private static ServiceAsync instance;

		public static ServiceAsync getInstance() {
			if (instance == null) {
				instance = (ServiceAsync) GWT.create(Service.class);
				ServiceDefTarget target = (ServiceDefTarget) instance;
				System.err.println(GWT.getModuleBaseURL());
				target.setServiceEntryPoint(GWT.getModuleBaseURL() + "/Service");
			}
			return instance;
		}
	}

	Language getLanguage(String code);

	String sayHello(Language lang);
}
