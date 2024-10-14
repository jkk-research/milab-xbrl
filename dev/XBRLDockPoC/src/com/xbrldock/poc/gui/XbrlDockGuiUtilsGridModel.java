package com.xbrldock.poc.gui;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import com.xbrldock.utils.XbrlDockUtilsConsts.LabeledAccess;

@SuppressWarnings("rawtypes")
public class XbrlDockGuiUtilsGridModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	public final LabeledAccess[] attNames;
	final ArrayList items;

	public XbrlDockGuiUtilsGridModel(ArrayList items, LabeledAccess... columns) {
		this.items = items;
		attNames = columns;
	}

	@Override
	public int getColumnCount() {
		return attNames.length;
	}

	@Override
	public String getColumnName(int column) {
		return attNames[column].label;
	}

	@Override
	public int getRowCount() {
		return items.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return attNames[columnIndex].get(items.get(rowIndex));
	}
}