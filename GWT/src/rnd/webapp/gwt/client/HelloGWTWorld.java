package rnd.webapp.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class HelloGWTWorld implements EntryPoint {

	public void onModuleLoad() {
		RootPanel rootPanel = RootPanel.get();

		final TextBox textBox = new TextBox();
		rootPanel.add(textBox);

		textBox.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				// System.err.println("onChange");
				Window.alert(textBox.getText());
			}
		});

		Button clickMeButton = new Button();
		rootPanel.add(clickMeButton);

		clickMeButton.setText("Click me!");
		clickMeButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				textBox.setText("Hello");

			}
		});
	}
}
