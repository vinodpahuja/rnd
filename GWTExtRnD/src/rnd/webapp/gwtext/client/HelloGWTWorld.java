//package rnd.webapp.gwtext.client;
//
//import rnd.webapp.gwt.client.Language;
//import rnd.webapp.gwt.client.Service;
//
//import com.google.gwt.core.client.EntryPoint;
//import com.google.gwt.user.client.rpc.AsyncCallback;
//import com.google.gwt.user.client.ui.RootPanel;
//import com.gwtext.client.core.EventObject;
//import com.gwtext.client.widgets.Button;
//import com.gwtext.client.widgets.MessageBox;
//import com.gwtext.client.widgets.Panel;
//import com.gwtext.client.widgets.event.ButtonListenerAdapter;
//import com.gwtext.client.widgets.layout.ColumnLayoutData;
//
//public class HelloGWTWorld implements EntryPoint {
//
//	public void onModuleLoad() {
//		RootPanel rootPanel = RootPanel.get();
//
//		final Panel panel = new Panel();
//
//		panel.setTitle("Hello GWT-Ext World");
//
//		panel.setWidth("100%");
//		panel.setHeight("100%");
//
//		Button sayHelloDefaultButton = new Button();
//		sayHelloDefaultButton.setText("Say Hello(Default)");
//
//		final AsyncCallback sayHelloCallback = new AsyncCallback() {
//
//			public void onSuccess(Object result) {
//
//				MessageBox.alert(result.toString() + " GWT-Ext World");
//			}
//
//			public void onFailure(Throwable caught) {
//				caught.printStackTrace();
//			}
//		};
//
//		final AsyncCallback languageCallback = new AsyncCallback() {
//
//			public void onSuccess(Object result) {
//				Service.Util.getInstance().sayHello((Language) result, sayHelloCallback);
//			}
//
//			public void onFailure(Throwable caught) {
//				caught.printStackTrace();
//			}
//		};
//
//		sayHelloDefaultButton.addListener(new ButtonListenerAdapter() {
//			@Override
//			public void onClick(Button button, EventObject e) {
//				Service.Util.getInstance().getLanguage("en", languageCallback);
//			}
//		});
//
//		Button sayHelloHindiButton = new Button();
//
//		sayHelloHindiButton.setText("Say Hello (Hindi)");
//
//		sayHelloHindiButton.addListener(new ButtonListenerAdapter() {
//			@Override
//			public void onClick(Button button, EventObject e) {
//				Service.Util.getInstance().getLanguage("hi", languageCallback);
//			}
//		});
//
//		ColumnLayoutData layoutData = new ColumnLayoutData(100);
//
//		panel.add(sayHelloDefaultButton, layoutData);
//		panel.add(sayHelloHindiButton, layoutData);
//
//		rootPanel.add(panel);
//	}
//}
