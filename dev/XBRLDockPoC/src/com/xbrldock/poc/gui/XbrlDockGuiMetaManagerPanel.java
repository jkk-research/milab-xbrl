package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
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
public class XbrlDockGuiMetaManagerPanel extends JPanel implements XbrlDockGuiConsts, GenAgent {
	private static final long serialVersionUID = 1L;
	
	GenAgent metaManager;
	
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
	
	JLabel lbDropZone;
	
	JEditorPane txtInfo;


	ActionListener al = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			}
		}
	};

	public XbrlDockGuiMetaManagerPanel() throws Exception {
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
		
		txtInfo.setText("<html><body>Here comes the description of the selected taxonomy</body></html>");
		
		lbDropZone = new JLabel("Drop zone for new taxonomy");
		
		JPanel pnlTop = new JPanel(new BorderLayout());

		pnlTop.add(XbrlDockUtilsGui.createSplit(true, new JScrollPane(tblGrid), lbDropZone, 0.2), BorderLayout.CENTER);
		add(XbrlDockUtilsGui.createSplit(false, pnlTop, new JScrollPane(txtInfo), 0.2), BorderLayout.CENTER);
	}

	@Override
	public void initModule(Map config) throws Exception {
		metaManager = XbrlDock.getAgent(XDC_CFGTOKEN_AGENT_metaManager);
	}

	@Override
	public <RetType> RetType process(String command, Object... params) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
