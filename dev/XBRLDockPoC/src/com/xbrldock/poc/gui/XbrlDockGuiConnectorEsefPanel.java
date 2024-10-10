package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts.GenAgent;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "rawtypes", /*"unchecked"*/ })
public class XbrlDockGuiConnectorEsefPanel extends JPanel implements XbrlDockGuiConsts, GenAgent {
	private static final long serialVersionUID = 1L;
	
	GenAgent esefConn;
	
	ArrayList<String> attNames = new ArrayList<>();

	ArrayList<Map> items = new ArrayList<>();
	Map selItem;

	class GridModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		@Override
		public int getRowCount() {
			return items.size();
		}

		@Override
		public int getColumnCount() {
			return attNames.size();
		}

		@Override
		public String getColumnName(int column) {
			return attNames.get(column);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			String k = attNames.get(columnIndex);
			return items.get(rowIndex).get(k);
		}
	}

	GridModel mdlGrid;
	JTable tblGrid;
	
	JEditorPane txtInfo;


	ActionListener al = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			}
		}
	};

	public XbrlDockGuiConnectorEsefPanel() throws Exception {
		super(new BorderLayout());		
		
//		mdlGrid = new GridModel();
//		tblGrid = new JTable(mdlGrid);
		tblGrid = new JTable();

		ListSelectionModel lm = tblGrid.getSelectionModel();
		lm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lm.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {}
			}
		});
		
		txtInfo = new JEditorPane();
		txtInfo.setContentType("text/html");
		txtInfo.setEditable(false);
		
		txtInfo.setText("<html><body>Here comes the description of the selected report</body></html>");
		
		add(XbrlDockUtilsGui.createSplit(false, new JScrollPane(tblGrid), new JScrollPane(txtInfo), 0.2), BorderLayout.CENTER);
	}

	@Override
	public void initModule(Map config) throws Exception {
		esefConn = XbrlDock.getAgent(XDC_CFGTOKEN_AGENT_esefConn);
	}

	@Override
	public Object process(String command, Object... params) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
