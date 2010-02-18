package rnd.webapp.gwtext.client.panel.form;

/* 
 * GWT-Ext Widget Library 
 * Copyright 2007 - 2008, GWT-Ext LLC., and individual contributors as indicated 
 * by the @authors tag. See the copyright.txt in the distribution for a 
 * full listing of individual contributors. 
 * 
 * This is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation; either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * This software is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * Lesser General Public License for more details. 
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this software; if not, write to the Free 
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 */

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.TimeField;
import com.gwtext.client.widgets.form.VType;

public class SimpleFormSample implements EntryPoint {

	public void onModuleLoad() {
		Panel panel = new Panel();
		panel.setBorder(false);
		panel.setPaddings(15);

		final Panel formPanel = new Panel();
//		final FormPanel formPanel = new FormPanel();
		formPanel.setFrame(true);
		formPanel.setTitle("Simple Form");

		formPanel.setWidth(350);
//		formPanel.setLabelWidth(75);
//		formPanel.setUrl("save-form.php");

		Label label = new Label();
		label.setText("<p>This is an example of a Form Label. This can have any <b>HTML</b> content.</p>");
		// label.setCls("simple-form-label");
//		label.setCls("x-form-item-label");
//		label.setWidth(350);
//		label.setHeight(20);
		formPanel.add(label);
		
		HTML html = new HTML();
		html.setHTML("<p>This is an example of a Form Label. This can have any <b>HTML</b> content.</p>");
		formPanel.add(html);

		TextField firstName = new TextField("First Name", "first", 230);
		firstName.setAllowBlank(false);
		formPanel.add(firstName);

		TextField lastName = new TextField("Last Name", "last", 230);
		formPanel.add(lastName);

		TextField company = new TextField("Company", "company", 230);
		formPanel.add(company);

		TextField email = new TextField("Email", "email", 230);
		email.setVtype(VType.EMAIL);
		formPanel.add(email);

		TimeField time = new TimeField("Time", "time", 230);
		time.setMinValue("8:00am");
		time.setMaxValue("6:00pm");
		formPanel.add(time);

		Button save = new Button("Save");
		formPanel.addButton(save);

		Button cancel = new Button("Cancel");
		formPanel.addButton(cancel);

		panel.add(formPanel);

		RootPanel.get().add(panel);
	}
}
