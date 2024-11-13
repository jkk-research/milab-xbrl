package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import com.xbrldock.XbrlDockException;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockGuiWidgetGrid implements XbrlDockGuiConsts.Widget<JComponent>, XbrlDockGuiConsts {

	class GridModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		@Override
		public int getColumnCount() {
			return colIdx.size();
		}

		@Override
		public int getRowCount() {
			return items.size();
		}

		@Override
		public String getColumnName(int column) {
			int ci = colIdx.get(column);

			if (-1 == ci) {
				switch (specColumns[column]) {
				case XDC_GRIDCOL_ROWNUM:
					return "#";
				case XDC_GRIDCOL_SELECTED:
					return "+";
				}
			}
			return cols.get(ci).label;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			int ci = colIdx.get(columnIndex);

			if (-1 == ci) {
				switch (specColumns[columnIndex]) {
				case XDC_GRIDCOL_ROWNUM:
					return rowIndex + 1;
				case XDC_GRIDCOL_SELECTED:
					return selected.contains(items.get(rowIndex));
				}
			}

			return cols.get(ci).get(items.get(rowIndex));
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return (-1 == colIdx.get(columnIndex)) && XDC_GRIDCOL_SELECTED.equals(specColumns[columnIndex]);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			int ci = colIdx.get(columnIndex);

			if (-1 == ci) {
				switch (specColumns[columnIndex]) {
				case XDC_GRIDCOL_ROWNUM:
					return Integer.class;
				case XDC_GRIDCOL_SELECTED:
					return Boolean.class;
				}
			}

			return cols.get(ci).valClass;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			Object o = items.get(rowIndex);

			boolean update = false;
			if (Boolean.TRUE.equals(aValue)) {
				update = selected.add(o);
			} else {
				update = selected.remove(o);
			}

			if (update) {
				al.actionPerformed(new WidgetEvent(XbrlDockGuiWidgetGrid.this, XDC_GUICMD_SELCHG, selected));
			}
		}
	}

	private static final String[] NO_SPEC_COLS = {};

	final String[] specColumns;
	final ArrayList<LabeledAccess> cols = new ArrayList<>();
	final ArrayList<Integer> colIdx = new ArrayList<>();

	final Set selected = new HashSet();
	final ArrayList items = new ArrayList();
	Object selItem;

	GridModel model;
	JTable tbl;
	ListSelectionModel lsm;
	JPanel gridPanel;
	ActionListener al;

	public XbrlDockGuiWidgetGrid(LabeledAccess... columns) {
		this(ListSelectionModel.SINGLE_SELECTION, NO_SPEC_COLS, columns);
	}

	public XbrlDockGuiWidgetGrid(int selMode, String[] specColumns, LabeledAccess... columns) {
		this.specColumns = specColumns;

		for (@SuppressWarnings("unused")
		String sc : specColumns) {
			colIdx.add(-1);
		}

		int ci = 0;
		for (LabeledAccess la : columns) {
			cols.add(la);
			colIdx.add(ci ++ );
		}

		model = new GridModel();
		tbl = new JTable(model);

		lsm = tbl.getSelectionModel();
		lsm.setSelectionMode(selMode);

		gridPanel = new JPanel(new BorderLayout());
		gridPanel.add(new JScrollPane(tbl), BorderLayout.CENTER);
	}

	@Override
	public void setActionListener(ActionListener al, String... guiCmds) {
		this.al = al;

		for (String gc : guiCmds) {
			switch (gc) {
			case XDC_GUICMD_PICK:
				lsm.addListSelectionListener(new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (!e.getValueIsAdjusting()) {
							int sr = tbl.getSelectedRow();
							selItem = (-1 == sr) ? null : items.get(sr);
							al.actionPerformed(new WidgetEvent(XbrlDockGuiWidgetGrid.this, XDC_GUICMD_PICK, selItem));
						}
					}
				});
				break;
			case XDC_GUICMD_SELCHG:
				break;
			case XDC_GUICMD_ACTIVATE:
				tbl.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (1 < e.getClickCount()) {
							al.actionPerformed(new WidgetEvent(XbrlDockGuiWidgetGrid.this, XDC_GUICMD_ACTIVATE, selItem));
						}
					}
				});
				break;
			}
		}
	}

	public void updateItems(boolean clear, GenAgent loader) {
		if (clear) {
			items.clear();
		}

		try {
			loader.process(XDC_CMD_GEN_Process, items);
		} catch (Exception e) {
			XbrlDockException.wrap(e, "Grid updateItems");
		}

		model.fireTableDataChanged();
	}

	@Override
	public JComponent getComp() {
		return gridPanel;
	}
}