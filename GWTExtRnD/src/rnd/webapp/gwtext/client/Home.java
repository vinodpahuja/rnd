package rnd.webapp.gwtext.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.core.RegionPosition;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.Viewport;
import com.gwtext.client.widgets.layout.BorderLayout;
import com.gwtext.client.widgets.layout.BorderLayoutData;
import com.gwtext.client.widgets.layout.FitLayout;

public class Home implements EntryPoint {

	public void onModuleLoad() {
		RootPanel rootPanel = RootPanel.get();

		Panel panel = new Panel();
		panel.setLayout(new FitLayout());

		Panel borderPanel = new Panel();
		borderPanel.setLayout(new BorderLayout());

		// add north panel
		Panel northPanel = new Panel();
		northPanel.setHeight("25");
		borderPanel.add(northPanel, new BorderLayoutData(RegionPosition.NORTH));

		// add south panel
		Panel southPanel = new Panel();
		southPanel.setTitle("Status");
		southPanel.setCollapsible(true);
		southPanel.collapse();

		BorderLayoutData southData = new BorderLayoutData(RegionPosition.SOUTH);
		southData.setSplit(true);
		borderPanel.add(southPanel, southData);

		Panel westPanel = new Panel();
		westPanel.setTitle("Action");
		westPanel.setCollapsible(true);
		westPanel.setWidth("25%");

		BorderLayoutData westData = new BorderLayoutData(RegionPosition.WEST);
		westData.setSplit(true);
		borderPanel.add(westPanel, westData);

		Panel centerPanel = new Panel();
		borderPanel.add(centerPanel, new BorderLayoutData(RegionPosition.CENTER));

		panel.add(borderPanel);

		new Viewport(panel);

		rootPanel.add(panel);

	}

}
