package rnd.webapp.gwtext.client.table;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.FormPanel;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.grid.BaseColumnConfig;
import com.gwtext.client.widgets.grid.CellSelectionModel;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.ColumnModel;
import com.gwtext.client.widgets.grid.EditorGridPanel;
import com.gwtext.client.widgets.grid.GridEditor;
import com.gwtext.client.widgets.grid.GridPanel;
import com.gwtext.client.widgets.grid.RowNumberingColumnConfig;
import com.gwtext.client.widgets.grid.event.EditorGridListenerAdapter;
import com.gwtext.client.widgets.grid.event.GridListenerAdapter;

public class TestTable2 implements EntryPoint {

	public void onModuleLoad() {

		RootPanel rootPanel = RootPanel.get();

		final FormPanel formPanel = new FormPanel();

		final TextField firstNameTF = new TextField("First Name", "first", 230);
		firstNameTF.setAllowBlank(false);
		formPanel.add(firstNameTF);

		final TextField lastNameTF = new TextField("Last Name", "last", 230);
		formPanel.add(lastNameTF);

		Button save = new Button("Save");
		formPanel.addButton(save);

		rootPanel.add(formPanel);

		final EditorGridPanel grid = new EditorGridPanel();

		grid.setClicksToEdit(1);

		grid.setSize(800, 200);

		BaseColumnConfig[] cc = new BaseColumnConfig[]
			{

			new RowNumberingColumnConfig(),

			new ColumnConfig("First Name", "firstName", 150) {
				{
					setEditor(new GridEditor(new TextField()));
				}
			},

			new ColumnConfig("Last Name", "lastName", 150) {
				{
					setEditor(new GridEditor(new TextField()));
				}
			}, new ColumnConfig("Full Name", "fullName", 150) };

		ColumnModel cm = new ColumnModel(cc);
		grid.setColumnModel(cm);

		FieldDef[] fd = new FieldDef[]
			{

			new StringFieldDef("firstName"),

			new StringFieldDef("lastName"),

			new StringFieldDef("fullName"),

			};

		final RecordDef rd = new RecordDef(fd);

		final Store s = new Store(rd);

		// MemoryProxy proxy = new MemoryProxy(new Object[][]
		// { rowData });
		// ArrayReader reader = new ArrayReader(this);
		// Store temp = new Store(proxy, reader);
		// temp.load();

		grid.setStore(s);

		rootPanel.add(grid);

		grid.addEditorGridListener(new EditorGridListenerAdapter() {
			@Override
			public void onAfterEdit(GridPanel grid1, Record record, String field, Object newValue, Object oldValue, int rowIndex, int colIndex) {
				record.commit();
			}

		});

		grid.addGridListener(new GridListenerAdapter() {
			@Override
			public void onKeyPress(EventObject e) {
				if (e.getKey() == 's' || e.getKey() == 'S') {
					editSelected(grid);
				}
			}
		});

		save.addListener(new ButtonListenerAdapter() {
			@Override
			public void onClick(Button button, EventObject e) {

				try {

					System.out.println("save called");

					Record r = rd.createRecord(new Object[]
						{ firstNameTF.getText(), lastNameTF.getText(), "" });

					System.out.println("record:" + r);

					grid.stopEditing();

					System.out.println("count_before:" + s.getTotalCount());

					s.add(r);

					System.out.println("count_after:" + s.getTotalCount());

					grid.startEditing(0, 0);

					firstNameTF.setValue("");
					lastNameTF.setValue("");
					System.out.println("saved");
				}
				catch (Throwable t) {
					t.printStackTrace();
					Window.alert("Error:" + t.getMessage());
				}
			}

		});

	}

	void editSelected(EditorGridPanel grid) {
		final CellSelectionModel cs = grid.getCellSelectionModel();
		int s[] = cs.getSelectedCell();
		int r1 = s[0];
		int c1 = s[1];
		edit(grid, r1, c1);
	}

	private void edit(EditorGridPanel grid, int rowIndex, int colIndex) {
		CellSelectionModel cs = grid.getCellSelectionModel();
		if (!cs.isLocked()) {
			cs.lock();
			grid.startEditing(rowIndex, colIndex);
			cs.unlock();
		}
	}
}
