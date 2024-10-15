package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JPanel;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts.GenAgent;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.conn.xbrlorg.XbrlDockConnXbrlOrgConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "rawtypes", "unchecked"})
public class XbrlDockGuiConnectorEsefPanel extends JPanel implements XbrlDockGuiConsts, XbrlDockConnXbrlOrgConsts, GenAgent {
	private static final long serialVersionUID = 1L;

//@formatter:off  
	XbrlDockGuiUtilsGrid reportGrid = new XbrlDockGuiUtilsGrid(this, 
			new LabeledAccess("Entity", "", XDC_REPORT_TOKEN_entityName), 
			new LabeledAccess("Identifier", "", XDC_REPORT_TOKEN_sourceAtts, XDC_XBRLORG_TOKEN_fxo_id),
			new LabeledAccess("Start", "", XDC_REPORT_TOKEN_startDate), 
			new LabeledAccess("End", "", XDC_REPORT_TOKEN_endDate),
			new LabeledAccess("Namespaces", FMT_MAP, XDC_REPORT_TOKEN_namespaces)
	);
//@formatter:on

	XbrlDockGuiUtilsHtmlDisplay repInfo;

	public XbrlDockGuiConnectorEsefPanel() throws Exception {
		super(new BorderLayout());

		repInfo = new XbrlDockGuiUtilsHtmlDisplay();
		
		add(XbrlDockUtilsGui.createSplit(false, reportGrid.getComp(), repInfo.getComp(), 0.2), BorderLayout.CENTER);
	}

	protected void updateDescPanel(Object selItem) {
		repInfo.setObject(selItem);
	}

	@Override
	public void initModule(Map config) throws Exception {
		Map metaCatalog = XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_esefConn, XDC_CMD_GEN_GETCATALOG);
		repInfo.setPlaceholder(XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_placeholder));
		
		reportGrid.updateItems(true, new GenProcessor<ArrayList>() {
			@Override
			public boolean process(ArrayList items, ProcessorAction action) throws Exception {
				for (Object rep : ((Map) XbrlDockUtils.simpleGet(metaCatalog, XDC_CONN_CAT_TOKEN_filings)).values()) {
					items.add(rep);
				}
				return true;
			}
		});
		
		updateDescPanel(null);
	}

	@Override
	public Object process(String command, Object... params) throws Exception {
		Object ret = null;
		
		switch (command) {
		case XDC_CMD_GEN_SELECT:
			updateDescPanel(params[0]);
			break;
		case XDC_CMD_GEN_ACTIVATE:
			String selId = XbrlDockUtils.simpleGet(params[0], XDC_EXT_TOKEN_id);
			XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_gui, XDC_CMD_GEN_SELECT, XDC_CFGTOKEN_AGENT_esefConn, selId);
			break;
		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}
		
		return ret;

	}

}
