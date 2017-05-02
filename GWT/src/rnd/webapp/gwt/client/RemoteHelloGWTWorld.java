package rnd.webapp.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;

public class RemoteHelloGWTWorld implements EntryPoint {
	private Button sayHelloDefaultButton;

	private Button sayHelloHindiButton;

	public void onModuleLoad() {
		RootPanel rootPanel = RootPanel.get();

		this.sayHelloDefaultButton = new Button();
		rootPanel.add(this.sayHelloDefaultButton);
		this.sayHelloDefaultButton.setText("Say Hello(Default)");

		final AsyncCallback sayHelloCallback = new AsyncCallback() {

			public void onSuccess(Object result) {
				Window.alert(result.toString());
			}

			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}
		};

		final AsyncCallback languageCallback = new AsyncCallback() {

			public void onSuccess(Object result) {
				Service.Util.getInstance().sayHello((Language) result, sayHelloCallback);
			}

			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}
		};

		this.sayHelloDefaultButton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				Service.Util.getInstance().getLanguage("en", languageCallback);
			}
		});

		this.sayHelloHindiButton = new Button();
		rootPanel.add(this.sayHelloHindiButton);
		this.sayHelloHindiButton.setText("Say Hello (Hindi)");

		this.sayHelloHindiButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Service.Util.getInstance().getLanguage("hi", languageCallback);
			}
		});
	}

}
