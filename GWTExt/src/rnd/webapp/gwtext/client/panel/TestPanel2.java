package rnd.webapp.gwtext.client.panel;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.event.TextFieldListenerAdapter;
import com.gwtext.client.widgets.layout.ColumnLayout;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class TestPanel2 implements EntryPoint {

	public void onModuleLoad() {

		final Panel myPanel = new Panel();

		myPanel.setLayout(new ColumnLayout());

		myPanel.setWidth(300);
		myPanel.setHeight(200);
		myPanel.setFrame(true); // Optional
		// myPanel.setTitle("Panel Example");

		// Label l1 = new Label("First Name");

		// l1.setSize(150, 19);
		TextField tb1 = new TextField() {
		};
		tb1.setSize(150, 19);

		tb1.addListener(new TextFieldListenerAdapter() {
			@Override
			public void onChange(Field field, Object newVal, Object oldVal) {
				System.err.println("chaned");
			}
		});

		// Label l2 = new Label("Middle Name");
		// l2.setSize(150, 19);
		// TextField tb2 = new TextField();
		// tb2.setSize(150, 19);

		// Label l3 = new Label("Last Name");
		// l3.setSize(150, 19);
		// TextField tb3 = new TextField();
		// tb3.setSize(150, 19);

		// myPanel.add(l1);

		// myPanel.add(l2);
		// myPanel.add(tb2);
		// myPanel.add(l3);
		// myPanel.add(tb3);
		
		myPanel.add(tb1);
		
		System.err.println("rende" + tb1.isRendered());
		tb1.setValue(null);
		
		RootPanel.get().add(myPanel);

		
		



	}

}
