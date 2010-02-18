package rnd.webapp.gwtext.client.panel;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.form.FormPanel;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.TimeField;
import com.gwtext.client.widgets.form.VType;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class TestPanel implements EntryPoint {

	public void onModuleLoad() {

		FormPanel formPanel = new FormPanel();

		formPanel.setPaddings(15);
		formPanel.setFrame(true);
		formPanel.setTitle("Simple Form");
		formPanel.setWidth(350);
		formPanel.setHeight(250);
//		formPanel.setLabelWidth(75);
		formPanel.setUrl("save-form.php");

		TextField firstName = new TextField("First Name", "first");
		firstName.setAllowBlank(false);
		formPanel.add(firstName);

		TextField lastName = new TextField("Last Name", "last");
		formPanel.add(lastName);

		TextField company = new TextField("Company", "company");
		formPanel.add(company);
		//
		TextField email = new TextField("Email", "email");
		email.setVtype(VType.EMAIL);
		formPanel.add(email);

		TimeField time = new TimeField("Time", "time");
		time.setMinValue("8:00am");
		time.setMaxValue("6:00pm");
		formPanel.add(time);

		Button save = new Button("Save");
		formPanel.addButton(save);

		Button cancel = new Button("Cancel");
		formPanel.addButton(cancel);

		RootPanel.get().add(formPanel);
	}
}
