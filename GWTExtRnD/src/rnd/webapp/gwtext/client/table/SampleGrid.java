/*
 * GWT-Ext Widget Library
 * Copyright(c) 2007-2008, GWT-Ext.
 * licensing@gwt-ext.com
 * 
 * http://www.gwt-ext.com/license
 */
package rnd.webapp.gwtext.client.table;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.gwtext.client.data.ArrayReader;
import com.gwtext.client.data.DateFieldDef;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.FloatFieldDef;
import com.gwtext.client.data.MemoryProxy;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.form.NumberField;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.grid.BaseColumnConfig;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.ColumnModel;
import com.gwtext.client.widgets.grid.EditorGridPanel;
import com.gwtext.client.widgets.grid.GridEditor;
import com.gwtext.client.widgets.grid.Renderer;
import com.gwtext.client.widgets.grid.RowNumberingColumnConfig;

public class SampleGrid extends EditorGridPanel {

	static final NumberFormat nf = NumberFormat.getFormat("#,##0.00", "$");

	static final NumberFormat nfc = NumberFormat.getFormat("#,##0.00");

	static final DateTimeFormat dateFormatter = DateTimeFormat.getFormat("M/d/y");

	protected static BaseColumnConfig[] columns = new BaseColumnConfig[] {

	new RowNumberingColumnConfig(),

	new ColumnConfig("Company", "company", 160, true, null, "company") {
		{
			setEditor(new GridEditor(new TextField()));
		}
	},

	new ColumnConfig("Price", "price", 35, true, new Renderer() {
		public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
			return nf.format(((Number) value).doubleValue());
		}
	}, "price") {
		{
			setEditor(new GridEditor(new NumberField()));
		}
	},

	new ColumnConfig("Change", "change", 45, true, new Renderer() {
		public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
			float val = ((Float) value).floatValue();
			String valString = nfc.format(val);
			if (val < 0) { return "<span style='color:red;'>" + valString + "</span>"; }
			return valString;
		}
	}, "change"),

	new ColumnConfig("% Change", "pctChange", 65, true, null, "pctChange"),

	new ColumnConfig("Last Updated", "lastChanged", 65, true, new Renderer() {
		public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
			Date date = (Date) value;
			return dateFormatter.format(date);
		}
	}),

	new ColumnConfig("Industry", "industry", 60, true) };

	RecordDef recordDef = new RecordDef(new FieldDef[] {

	new StringFieldDef("company"),

	new FloatFieldDef("price"),

	new FloatFieldDef("change"),

	new FloatFieldDef("pctChange"),

	new DateFieldDef("lastChanged", "n/j h:ia"),

	new StringFieldDef("symbol"),

	new StringFieldDef("industry") });

	private int[] columnIndexes;

	public SampleGrid(boolean small, int[] columnIndexes) {

		this.columnIndexes = columnIndexes;

		Object[][] data = small ? SampleData.getCompanyDataSmall() : SampleData.getCompanyDataLarge();
		MemoryProxy proxy = new MemoryProxy(data);
		ArrayReader reader = new ArrayReader(this.recordDef);
		Store store = new Store(proxy, reader);
		store.load();
		setStore(store);

		ColumnModel columnModel = getColumnConfigs();
		setColumnModel(columnModel);

		setFrame(true);
		setStripeRows(true);
		setAutoExpandColumn("company");
		setIconCls("grid-icon");

	}

	protected ColumnModel getColumnConfigs() {
		ColumnModel columnModel = null;
		if (this.columnIndexes == null) {
			columnModel = new ColumnModel(columns);
		} else {
			BaseColumnConfig[] columnConfigs = new BaseColumnConfig[this.columnIndexes.length];
			for (int i = 0; i < this.columnIndexes.length; i++) {
				// int columnIndex = columnIndexes[i];
				columnConfigs[i] = columns[i];
			}
			columnModel = new ColumnModel(columnConfigs);
		}
		return columnModel;
	}
}
