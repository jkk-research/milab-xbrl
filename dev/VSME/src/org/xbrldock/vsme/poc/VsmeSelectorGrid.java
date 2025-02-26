package org.xbrldock.vsme.poc;

import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.xbrldock.utils.XbrlDockUtilsGui;

public class VsmeSelectorGrid implements VsmePocConsts, XbrlDockUtilsGui.WidgetContainer {
	JScrollPane scp;
	JTable tbl;
	DefaultTableModel mdl;
	
	GenAgent listener;
	
	public VsmeSelectorGrid(Map<String, Object> config, String sel, GenAgent listener) {
		this.listener = listener;
		
		mdl  = new DefaultTableModel();
		
		
	}
	
	@Override
	public JComponent getWidget() {
		return scp;
	}
}
