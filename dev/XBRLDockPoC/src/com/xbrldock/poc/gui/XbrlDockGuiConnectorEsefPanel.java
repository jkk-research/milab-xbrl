package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts.GenAgent;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevReportStats;
import com.xbrldock.poc.conn.xbrlorg.XbrlDockConnXbrlOrgConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;
import com.xbrldock.utils.XbrlDockUtilsMvel;

@SuppressWarnings({ "rawtypes", "unchecked" })
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

	Map metaCatalog;

	JTextArea taFilter;
	JButton btFilter;

	String filter;
	Object mvelFilter;

	XbrlDockGuiUtilsHtmlDisplay repInfo;

	ActionListener al = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();

			switch (cmd) {
			case XDC_CMD_GEN_FILTER:
				String txt = taFilter.getText().trim();
				if (!XbrlDockUtils.isEqual(txt, filter)) {
					filter = txt;
					mvelFilter = XbrlDockUtils.isEmpty(filter) ? null : XbrlDockUtilsMvel.compile(filter);

					updateReportGrid();
				}
				break;
			case XDC_CMD_GEN_TEST01:
				XbrlDockDevReportStats stats = new XbrlDockDevReportStats();
				XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_esefConn, XDC_CMD_GEN_TEST01, stats);

				XbrlDock.log(EventLevel.Info, "Test visit sats", stats);
				break;
			default:
				XbrlDockException.wrap(null, "Unknown command", cmd);
				break;
			}
		}
	};

	public XbrlDockGuiConnectorEsefPanel() throws Exception {
		super(new BorderLayout());

		repInfo = new XbrlDockGuiUtilsHtmlDisplay();

		taFilter = new JTextArea();
		btFilter = XbrlDockGuiUtils.createBtn(XDC_CMD_GEN_FILTER, al, JButton.class);

		JPanel pnlFilter = new JPanel(new BorderLayout());
		pnlFilter.add(XbrlDockGuiUtils.setTitle(new JScrollPane(taFilter), "Filter report list"), BorderLayout.CENTER);
		pnlFilter.add(btFilter, BorderLayout.EAST);

		if (XbrlDock.checkFlag(XDC_FLAG_ADMIN)) {
			pnlFilter.add(XbrlDockGuiUtils.createBtn(XDC_CMD_GEN_TEST01, al, JButton.class), BorderLayout.WEST);
		}

		JPanel pnlTop = new JPanel(new BorderLayout());
		pnlTop.add(XbrlDockUtilsGui.createSplit(false, pnlFilter, XbrlDockGuiUtils.setTitle(reportGrid, "Reports"), 0.2), BorderLayout.CENTER);

		add(XbrlDockUtilsGui.createSplit(false, pnlTop, XbrlDockGuiUtils.setTitle(repInfo, "Selected report information"), 0.2), BorderLayout.CENTER);
	}

	protected void updateDescPanel(Object selItem) {
		repInfo.setObject(selItem);
	}

	@Override
	public void initModule(Map config) throws Exception {
		metaCatalog = XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_esefConn, XDC_CMD_GEN_GETCATALOG);
		repInfo.setPlaceholder(XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_placeholder));

		updateReportGrid();

		updateDescPanel(null);
	}

	private void updateReportGrid() {
		reportGrid.updateItems(true, new GenProcessor<ArrayList>() {
			@Override
			public boolean process(ProcessorAction action, ArrayList items) throws Exception {
				for (Object rep : ((Map) XbrlDockUtils.simpleGet(metaCatalog, XDC_CONN_CAT_TOKEN_filings)).values()) {

					if ((null == mvelFilter) || (Boolean) XbrlDockUtilsMvel.evalCompiled(mvelFilter, rep)) {
						items.add(rep);
					}
				}
				return true;
			}
		});
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
