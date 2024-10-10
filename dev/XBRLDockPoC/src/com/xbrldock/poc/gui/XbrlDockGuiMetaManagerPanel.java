package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "rawtypes", /*"unchecked"*/ })
public class XbrlDockGuiMetaManagerPanel extends JPanel implements XbrlDockGuiConsts, GenAgent {
	private static final long serialVersionUID = 1L;
	
	LabeledAccess[] attNames = {
			new LabeledAccess("Identifier", "", XDC_METAINFO_pkgInfo, XDC_EXT_TOKEN_identifier),
			new LabeledAccess("File count", FMT_COLL, XDC_METATOKEN_includes),
			new LabeledAccess("Requires", FMT_COLL, XDC_GEN_TOKEN_requires),
			new LabeledAccess("Entry points", FMT_COLL, XDC_METAINFO_entryPoints),
	};

	ArrayList<Object> items = new ArrayList<>();
	Object selItem;

	class GridModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

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

	GridModel mdlGrid;
	JTable tblGrid;
	
	JLabel lbDropZone;
	
	JEditorPane txtInfo;
	String placeholder;


	ActionListener al = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			
			}
		}
	};

	public XbrlDockGuiMetaManagerPanel() throws Exception {
		super(new BorderLayout());		
		
		mdlGrid = new GridModel();
		tblGrid = new JTable(mdlGrid);

		ListSelectionModel lm = tblGrid.getSelectionModel();
		lm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lm.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int sr = tblGrid.getSelectedRow();					
					selItem = (-1 == sr) ? null : items.get(sr);
					updateDescPanel();
				}
			}
		});
		
		txtInfo = new JEditorPane();
		txtInfo.setContentType("text/html");
		txtInfo.setEditable(false);
						
		lbDropZone = new JLabel("Drop zone for new taxonomy");
		lbDropZone.setMaximumSize(new Dimension(50, 500));
		
		JPanel pnlTop = new JPanel(new BorderLayout());

		pnlTop.add(XbrlDockUtilsGui.createSplit(true, new JScrollPane(tblGrid), lbDropZone, 0.2), BorderLayout.CENTER);
		add(XbrlDockUtilsGui.createSplit(false, pnlTop, new JScrollPane(txtInfo), 0.2), BorderLayout.CENTER);
	}

	protected void updateDescPanel() {
		String txt = ( null == selItem ) ? placeholder : FMT_TOHTML.toString(selItem);
		
		int cp = txtInfo.getCaretPosition();

		txtInfo.setText("<html><body>" + txt + "</body></html>");
		
		txtInfo.setCaretPosition(cp);
	}

	@Override
	public void initModule(Map config) throws Exception {
		Map metaCatalog = XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_GETCATALOG);
		placeholder = XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_placeholder);
		
		items.clear();
		for ( Object c : metaCatalog.values() ) {
			items.add(c);
		}
		
		mdlGrid.fireTableDataChanged();
		
		updateDescPanel();

	}

	@Override
	public <RetType> RetType process(String command, Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

}
