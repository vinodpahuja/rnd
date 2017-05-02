package rnd.webapp.gwtext.client.panel;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.RegionPosition;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.FormPanel;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.grid.BaseColumnConfig;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.ColumnModel;
import com.gwtext.client.widgets.grid.EditorGridPanel;
import com.gwtext.client.widgets.grid.RowNumberingColumnConfig;
import com.gwtext.client.widgets.layout.BorderLayout;
import com.gwtext.client.widgets.layout.BorderLayoutData;
import com.gwtext.client.widgets.layout.FitLayout;

public class TestSelectionField implements EntryPoint {

	public void onModuleLoad() {

		RootPanel rootPanel = RootPanel.get();
		rootPanel.add(createSearchField());
	}

	private Widget createSearchField() {

		TextField textField = new TextField("Person");
		final Button selector = new Button("...");

		FormPanel textFieldPanel = new FormPanel();
		textFieldPanel.setBorder(false);
		textFieldPanel.add(textField);

		HorizontalPanel searchFieldPanel = new HorizontalPanel();
		searchFieldPanel.add(textFieldPanel);
		searchFieldPanel.add(selector);

		final Window window = createPopup();

		selector.addListener(new ButtonListenerAdapter() {
			@Override
			public void onClick(Button button, EventObject e) {
				window.setPosition(selector.getAbsoluteLeft(), selector.getAbsoluteTop());
				if (!window.isVisible()) {
					window.setVisible(true);
				}
			}
		});
		return searchFieldPanel;
	}

	private Window createPopup() {

//		Panel searchPanel = createSearchPanel();
		EditorGridPanel grid = createActionBoard();

		final Window window = new Window();
		window.setTitle("Search");
		window.setWidth(400);
		window.setHeight(300);
		window.setClosable(true);
		window.setCloseAction(Window.HIDE);
		window.setModal(true);
		window.setPlain(true);

		window.setLayout(new BorderLayout());

		Panel p = new Panel();
		p.setLayout(new FitLayout());
		p.setSize(300, 200);
		p.add(grid);

		window.add(p, new BorderLayoutData(RegionPosition.CENTER));
//		window.add(searchPanel, new BorderLayoutData(RegionPosition.NORTH));
		return window;
	}

	private EditorGridPanel createActionBoard() {
		final EditorGridPanel grid = new EditorGridPanel();
		grid.setClicksToEdit(1);
		grid.setSize(800, 200);
		BaseColumnConfig[] cc = new BaseColumnConfig[] { new RowNumberingColumnConfig(), new ColumnConfig("First Name", "firstName", 150), new ColumnConfig("Last Name", "lastName", 150), new ColumnConfig("Full Name", "fullName", 150) };
		ColumnModel cm = new ColumnModel(cc);
		grid.setColumnModel(cm);
		FieldDef[] fd = new FieldDef[] { new StringFieldDef("firstName"), new StringFieldDef("lastName"), new StringFieldDef("fullName"), };
		final RecordDef rd = new RecordDef(fd);
		final Store s = new Store(rd);
		grid.setStore(s);
		return grid;
	}

	// private Panel createSearchPanel() {
	//
	// Panel searchFilterPanel = createSearchFilterPanel1();
	// Panel buttonPanel = createButtonPanel();
	//
	// Panel searchPanel = new Panel();
	// searchPanel.setBorder(false);
	// searchPanel.setHeight(50);
	// searchPanel.setLayout(new ColumnLayout());
	// searchPanel.add(searchFilterPanel);
	// searchPanel.add(buttonPanel);
	//
	// return searchPanel;
	// }

//	private Panel createButtonPanel() {
//		Panel buttonPanel = new Panel();
//		buttonPanel.setHeight(25);
//		buttonPanel.setBorder(false);
//		buttonPanel.setLayout(new HorizontalLayout(1));
//
//		Button searchButton = new Button("Search");
//		buttonPanel.add(searchButton);
//		Button filterButton = new Button("Filter");
//		buttonPanel.add(filterButton);
//		Button resetButton = new Button("Reset");
//		buttonPanel.add(resetButton);
//		return buttonPanel;
//	}

	// private Panel createSearchFilterPanel1() {
	// Panel searchFilterPanel = new Panel();
	//
	// searchFilterPanel.setLayout(new HorizontalLayout(1));
	//
	// searchFilterPanel.setHeight(25);
	// searchFilterPanel.setBorder(false);
	//
	// final Store fieldStore = new SimpleStore(new String[] { "field" }, new String[][] { { "First Name" }, { "Middle Name" }, { "Last Name" }, { "Full Name" } });
	// fieldStore.load();
	//
	// final ComboBox fieldCB = new ComboBox();
	// fieldCB.setForceSelection(true);
	// fieldCB.setStore(fieldStore);
	// fieldCB.setDisplayField("field");
	// fieldCB.setMode(ComboBox.LOCAL);
	// fieldCB.setTypeAhead(true);
	// fieldCB.setSelectOnFocus(true);
	// fieldCB.setHideTrigger(false);
	// fieldCB.setWidth(100);
	//
	// final Store store = new SimpleStore(new String[] { "ope" }, new String[][] { { "equals" }, { "greater than" }, { "less than" }, { "like" } });
	// store.load();
	//
	// final ComboBox operationCB = new ComboBox();
	// operationCB.setForceSelection(true);
	// operationCB.setStore(store);
	// operationCB.setDisplayField("ope");
	// operationCB.setMode(ComboBox.LOCAL);
	// operationCB.setTypeAhead(true);
	// operationCB.setSelectOnFocus(true);
	// operationCB.setHideTrigger(false);
	// operationCB.setWidth(100);
	//
	// TextField searchTextField = new TextField("Name", "name");
	//
	// searchFilterPanel.add(fieldCB);
	// searchFilterPanel.add(operationCB);
	// searchFilterPanel.add(searchTextField);
	// return searchFilterPanel;
	// }
}
