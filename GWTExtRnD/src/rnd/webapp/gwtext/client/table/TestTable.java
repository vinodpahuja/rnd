package rnd.webapp.gwtext.client.table;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.grid.CellSelectionModel;
import com.gwtext.client.widgets.grid.GridPanel;
import com.gwtext.client.widgets.grid.event.EditorGridListenerAdapter;
import com.gwtext.client.widgets.grid.event.GridListenerAdapter;

public class TestTable implements EntryPoint {

	final SampleGrid grid = new SampleGrid(false, null) {
		@Override
		public void onBrowserEvent(Event event) {
			int type = DOM.eventGetType(event);
			switch (type) {

				case Event.ONKEYDOWN:
				case Event.ONKEYPRESS:
				case Event.ONKEYUP:
				case Event.KEYEVENTS:
					Window.alert("key:" + event.getKeyCode());
					break;
			}

			super.onBrowserEvent(event);

		}

	};

	public void onModuleLoad() {
		RootPanel rootPanel = RootPanel.get();

		Panel panel = new Panel();

		this.grid.setHeight(350);
		this.grid.setWidth(600);
		this.grid.setTitle("Array Grid");
		this.grid.setClicksToEdit(1);

		panel.add(this.grid);

		rootPanel.add(panel);

		this.grid.addEditorGridListener(new EditorGridListenerAdapter() {
			@Override
			public void onAfterEdit(GridPanel grid1, Record record, String field, Object newValue, Object oldValue, int rowIndex, int colIndex) {
				record.commit();
			}

		});

		this.grid.addGridListener(new GridListenerAdapter() {
			@Override
			public void onKeyPress(EventObject e) {
				try {
					System.out.println(e.getKey());
					char key = (char) e.getKey();
					switch (key) {
						case 'e': // 101
						case 'E': // 69
							editSelected();
							break;
						case 4: // Ctrl + D
							deleteSelectedRow();
							break;
						case 9:// Ctrl + I
							insertRow();
							break;
					}
				}

				catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});

	}

	void insertRow() {
		try {

			Store s = this.grid.getStore();

			Record r = this.grid.recordDef.createRecord(new Object[]
				{ "", 0f, 0f, 0f, "9/1 12:00am", "", "" });

			s.insert(0, r);

		}
		catch (Throwable t) {
			t.printStackTrace();
		}

	}

	void deleteSelectedRow() {
		try {
			final CellSelectionModel cs = this.grid.getCellSelectionModel();

			int cell[] = cs.getSelectedCell();
			if (cell != null) {
				int r1 = cell[0];
				int c1 = cell[1];

				Store s = this.grid.getStore();

				Record selected = this.grid.getStore().getAt(r1);
				if (selected != null) {

					System.out.println("r1:" + r1);
					System.out.println("count:" + s.getCount());

					int cnt = s.getCount();

					s.remove(selected);
					s.commitChanges();

					if ((r1 + 1) == cnt) {
						r1 -= 1;
					}
					cs.select(r1, c1);
				}
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	void editSelected() {
		final CellSelectionModel cs = this.grid.getCellSelectionModel();
		int s[] = cs.getSelectedCell();
		int r1 = s[0];
		int c1 = s[1];
		edit(r1, c1);
	}

	private void edit(int rowIndex, int colIndex) {
		CellSelectionModel cs = this.grid.getCellSelectionModel();
		if (!cs.isLocked()) {
			cs.lock();
			this.grid.startEditing(rowIndex, colIndex);
			cs.unlock();
		}
	}
}
