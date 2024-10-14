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

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts.GenAgent;
import com.xbrldock.poc.conn.xbrlorg.XbrlDockConnXbrlOrgConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "rawtypes", /* "unchecked" */ })
public class XbrlDockGuiConnectorEsefPanel extends JPanel implements XbrlDockGuiConsts, XbrlDockConnXbrlOrgConsts, GenAgent {
	private static final long serialVersionUID = 1L;

	ArrayList<Map> items = new ArrayList<>();
	Map selItem;

//@formatter:off  

	XbrlDockGuiUtilsGridModel mdlGrid = new XbrlDockGuiUtilsGridModel(items, 
			new LabeledAccess("Identifier", "", XDC_REPORT_TOKEN_sourceAtts, XDC_XBRLORG_TOKEN_fxo_id),
			new LabeledAccess("Start", "", XDC_REPORT_TOKEN_startDate), 
			new LabeledAccess("End", "", XDC_REPORT_TOKEN_endDate),
			new LabeledAccess("Entity", "", XDC_REPORT_TOKEN_entityName), 
			new LabeledAccess("Namespaces", FMT_MAP, XDC_REPORT_TOKEN_namespaces)
	);
//@formatter:on

	JTable tblGrid;

	JEditorPane txtInfo;
	String placeholder;

	ActionListener al = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			}
		}
	};

	public XbrlDockGuiConnectorEsefPanel() throws Exception {
		super(new BorderLayout());

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
		
//		txtInfo.setText("<html><body>Here comes the description of the selected report</body></html>");

		add(XbrlDockUtilsGui.createSplit(false, new JScrollPane(tblGrid), new JScrollPane(txtInfo), 0.2), BorderLayout.CENTER);
	}

	protected void updateDescPanel() {
		String txt = (null == selItem) ? placeholder : FMT_TOHTML.toString(selItem);

		int cp = txtInfo.getCaretPosition();

		txtInfo.setText("<html><body>" + txt + "</body></html>");

		txtInfo.setCaretPosition(cp);
	}

	@Override
	public void initModule(Map config) throws Exception {
		Map metaCatalog = XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_esefConn, XDC_CMD_GEN_GETCATALOG);
		placeholder = XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_placeholder);

		for (Object rep : ((Map) XbrlDockUtils.simpleGet(metaCatalog, XDC_CONN_CAT_TOKEN_filings)).values()) {
			items.add((Map) rep);
		}

		mdlGrid.fireTableDataChanged();
		
		updateDescPanel();

	}

	@Override
	public Object process(String command, Object... params) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
