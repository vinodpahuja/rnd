package rnd.webapp.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ServiceAsync {
	void getLanguage(String code, AsyncCallback callback);

	void sayHello(Language lang, AsyncCallback callback);
}
