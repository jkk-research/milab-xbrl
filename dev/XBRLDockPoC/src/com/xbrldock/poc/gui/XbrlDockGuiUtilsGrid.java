package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import com.xbrldock.XbrlDockException;

@SuppressWarnings("rawtypes")
public class XbrlDockGuiUtilsGrid implements XbrlDockGuiConsts.ComponentWrapper<JComponent>, XbrlDockGuiConsts {

	class GridModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		@Override
		public int getColumnCount() {
			int ret = attNames.length;

			return showRowNum ? ret + 1 : ret;
		}

		@Override
		public String getColumnName(int column) {
			if (showRowNum) {
				if (0 == column) {
					return "#";
				}
				--column;
			}
			return attNames[column].label;
		}

		@Override
		public int getRowCount() {
			return items.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (showRowNum) {
				if (0 == columnIndex) {
					return rowIndex;
				}
				--columnIndex;
			}

			return attNames[columnIndex].get(items.get(rowIndex));
		}
	}

	public final GenAgent agent;

	boolean showRowNum;
	public final LabeledAccess[] attNames;
	final ArrayList items = new ArrayList();
	Object selItem;

	GridModel model;
	JTable tbl;

	JPanel gridPanel;

	public XbrlDockGuiUtilsGrid(GenAgent agent, LabeledAccess... columns) {
		this.agent = agent;
		attNames = columns;

		model = new GridModel();
		tbl = new JTable(model);

		ListSelectionModel lm = tbl.getSelectionModel();
		lm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lm.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int sr = tbl.getSelectedRow();
					selItem = (-1 == sr) ? null : items.get(sr);
					try {
						agent.process(XDC_CMD_GEN_SELECT, selItem);
					} catch (Exception e1) {
						XbrlDockException.wrap(e1, "Item selection", agent, selItem);
					}
				}
			}
		});

		tbl.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (1 < e.getClickCount()) {
					try {
						agent.process(XDC_CMD_GEN_ACTIVATE, selItem);
					} catch (Exception e1) {
						XbrlDockException.wrap(e1, "Item activation", agent, selItem);
					}
				}
			}
		});

		gridPanel = new JPanel(new BorderLayout());
		gridPanel.add(new JScrollPane(tbl), BorderLayout.CENTER);
	}

	public void setShowRowNum(boolean showRowNum) {
		if (this.showRowNum != showRowNum) {
			this.showRowNum = showRowNum;
			model.fireTableStructureChanged();
		}
	}

	public void updateItems(boolean clear, GenProcessor<ArrayList> loader) {
		if (clear) {
			items.clear();
		}

		try {
			loader.process(items, ProcessorAction.Process);
		} catch (Exception e) {
			XbrlDockException.wrap(e, "Grid updateItems", agent);
		}

		model.fireTableDataChanged();
	}

	@Override
	public JComponent getComp() {
		return gridPanel;
	}
}