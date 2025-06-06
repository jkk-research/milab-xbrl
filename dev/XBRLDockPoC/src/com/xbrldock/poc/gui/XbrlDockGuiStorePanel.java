package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts.GenAgent;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevReportStats;
import com.xbrldock.poc.conn.xbrlorg.XbrlDockConnXbrlOrgConsts;
import com.xbrldock.poc.report.XbrlDockReportExprEval;
import com.xbrldock.poc.report.XbrlDockReportUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;
import com.xbrldock.utils.XbrlDockUtilsMvel;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockGuiStorePanel extends JPanel implements XbrlDockGuiConsts, XbrlDockConnXbrlOrgConsts, GenAgent {
	private static final long serialVersionUID = 1L;

//@formatter:off  
	XbrlDockGuiWidgetGrid reportGrid = new XbrlDockGuiWidgetGrid(
			ListSelectionModel.SINGLE_SELECTION, new String[] {XDC_GRIDCOL_ROWNUM, XDC_GRIDCOL_SELECTED},
			
			new LabeledAccess("Entity", "", XDC_REPORT_TOKEN_entityName), 
			new LabeledAccess("Identifier", "", XDC_REPORT_TOKEN_sourceAtts, XDC_XBRLORG_TOKEN_fxo_id),
			new LabeledAccess("Start", "", XDC_EXT_TOKEN_startDate), 
			new LabeledAccess("End", "", XDC_EXT_TOKEN_endDate),
			new LabeledAccess("Namespaces", FMT_MAP, XDC_REPORT_TOKEN_namespaces)
	);
//@formatter:on

	Map catalog;
	String store;

	JTextArea repFilterTA;
	String repFilterStr;
	Object repFilterOb;

	JTextArea factFilterTA;
	JCheckBox chkByCtx;
	XbrlDockReportExprEval factFilterEval;
	XbrlDockReportUtils.SimpleMatchTester factFilterTest = new XbrlDockReportUtils.SimpleMatchTester();

	JButton btFilter;

	Collection<String> cmds;
	JPanel pnlCmds;

	XbrlDockGuiUtilsHtmlDisplay repInfo;

	ActionListener al = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();

			switch (cmd) {
			case XDC_CMD_GEN_FILTER:
				String txt = repFilterTA.getText().trim();
				if (!XbrlDockUtils.isEqual(txt, repFilterStr)) {
					repFilterStr = txt;
					repFilterOb = XbrlDockUtils.isEmpty(repFilterStr) ? null : XbrlDockUtilsMvel.compile(repFilterStr);
				}
				updateReportGrid();
				break;
			case XDC_CMD_GEN_TEST01:
				XbrlDockDevReportStats stats = new XbrlDockDevReportStats();
				XbrlDock.callAgentNoEx(store, XDC_CMD_GEN_TEST01, XbrlDockUtils.setParams(XDC_GEN_TOKEN_processor, stats));

				XbrlDock.log(EventLevel.Info, "Test visit sats", stats);

//				ReportDataHandler dhv = (ReportDataHandler) params[1];

//				for ( Object rep : reportGrid.items ) {
//					
//				}
				break;
			case XDC_CMD_GEN_TEST02:
				XbrlDock.callAgentNoEx(store, XDC_CMD_GEN_TEST02, XbrlDockUtils.setParams(XDC_GEN_TOKEN_members, reportGrid.items));
				break;
			case XDC_GUICMD_PICK:
				updateDescPanel(((WidgetEvent) e).getUserOb());
				break;
			case XDC_GUICMD_SELCHG:

				break;
			case XDC_GUICMD_ACTIVATE:
				String selId = XbrlDockUtils.simpleGet(((WidgetEvent) e).getUserOb(), XDC_EXT_TOKEN_id);
				XbrlDock.callAgentNoEx(XDC_CFGTOKEN_AGENT_gui, XDC_CMD_GEN_SELECT, XbrlDockUtils.setParams(XDC_GUICMD_WBAGENT, store, XDC_EXT_TOKEN_id, selId));
				break;
			default:
				if (cmds.contains(cmd)) {
					XbrlDock.callAgentNoEx(store, cmd, XbrlDockUtils.setParams(XDC_UTILS_MVEL_mvelCondition, repFilterTA.getText()));
				} else {
					XbrlDockException.wrap(null, "Unknown command", cmd);
				}
				break;
			}
		}
	};

	public XbrlDockGuiStorePanel() throws Exception {
		super(new BorderLayout());

		repInfo = new XbrlDockGuiUtilsHtmlDisplay();

		repFilterTA = new JTextArea();
		if (XbrlDock.checkUserFlag(XDC_FLAG_ADMIN)) {
			repFilterTA.setText("\"HU\".equals(sourceAtts.country)");
		}
		btFilter = XbrlDockGuiUtils.createBtn(XDC_CMD_GEN_FILTER, al, JButton.class);

		factFilterTA = new JTextArea();
		factFilterEval = new XbrlDockReportExprEval();

//		chkByCtx = new JCheckBox("Group facts by context");
//		JPanel pnlFactFilter = new JPanel(new BorderLayout());
//		pnlFactFilter.add(new JScrollPane(factFilterTA), BorderLayout.CENTER);
//		pnlFactFilter.add(chkByCtx, BorderLayout.SOUTH);

		JPanel pnlFilterInput = new JPanel(new BorderLayout());
		pnlFilterInput.add(XbrlDockUtilsGui.createSplit(true, XbrlDockGuiUtils.setTitle(new JScrollPane(repFilterTA), "Filter by report data"),
				XbrlDockGuiUtils.setTitle(new JScrollPane(factFilterTA), "Filter by fact data"), 0.5), BorderLayout.CENTER);
//		XbrlDockGuiUtils.setTitle(pnlFactFilter, "Filter reports by fact content"), 0.5), BorderLayout.CENTER);

		JPanel pnlFilter = new JPanel(new BorderLayout());
		pnlFilter.add(pnlFilterInput, BorderLayout.CENTER);
		pnlFilter.add(btFilter, BorderLayout.EAST);

		pnlCmds = new JPanel();
		pnlFilter.add(pnlCmds, BorderLayout.WEST);

//		if (XbrlDock.checkFlag(XDC_FLAG_ADMIN)) {
//			pnlFilter.add(XbrlDockGuiUtils.createBtn(XDC_CMD_GEN_TEST02, al, JButton.class), BorderLayout.WEST);
//		}

		JPanel pnlTop = new JPanel(new BorderLayout());
		pnlTop.add(XbrlDockUtilsGui.createSplit(false, pnlFilter, XbrlDockGuiUtils.setTitle(reportGrid, "Reports"), 0.2), BorderLayout.CENTER);

		add(XbrlDockUtilsGui.createSplit(false, pnlTop, XbrlDockGuiUtils.setTitle(repInfo, "Selected report information"), 0.2), BorderLayout.CENTER);

		reportGrid.setActionListener(al, XDC_GUICMD_ACTIVATE, XDC_GUICMD_PICK);
	}

	protected void updateDescPanel(Object selItem) {
		repInfo.setObject(selItem);
	}

	public void initModule(Map config) throws Exception {
		store = XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_store);
		catalog = XbrlDock.callAgent(store, XDC_CMD_GEN_GETCATALOG);
		repInfo.setPlaceholder(XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_placeholder));

		cmds = XbrlDockUtils.simpleGet(config, XDC_GUI_COMMANDS);

		if (null != cmds) {
			pnlCmds.setLayout(new GridLayout(cmds.size(), 1));

			for (String cmd : cmds) {
				pnlCmds.add(XbrlDockGuiUtils.createBtn(cmd, al, JButton.class));
			}
		} else {
			pnlCmds.getParent().remove(pnlCmds);
		}

		updateReportGrid();

		updateDescPanel(null);
	}

	private void updateReportGrid() {
		reportGrid.updateItems(true, new GenAgent() {

			@Override
			public Object process(String cmd, Map params) throws Exception {
				if (!XDC_CMD_GEN_Process.equals(cmd) || catalog.isEmpty()) {
					return true;
				}

				ArrayList items = (ArrayList) params.get(XDC_GEN_TOKEN_members);
				String txt = factFilterTA.getText().trim();
				boolean factExpr = !XbrlDockUtils.isEmpty(txt);

				if (factExpr) {
					factFilterTest.setByContext(chkByCtx.isSelected());
					factFilterEval.setExpression(txt, factFilterTest);

					XbrlDock.log(EventLevel.Trace, "Fact expr testing", txt);
				}

				for (Map.Entry<String, Object> re : ((Map<String, Object>) XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings)).entrySet()) {
					Object rep = re.getValue();

					if ((null != repFilterOb) && !(Boolean) XbrlDockUtilsMvel.evalCompiled(repFilterOb, rep)) {
						continue;
					}

					if (factExpr) {
						String repId = re.getKey();
						XbrlDock.log(EventLevel.Trace, "   on report", repId);

						XbrlDock.callAgent(store, XDC_CMD_CONN_VISITREPORT,  XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, repId, XDC_GEN_TOKEN_processor, factFilterEval));

						if (!factFilterTest.isMatch()) {
							continue;
						}
					}

					items.add(rep);
				}

				XbrlDock.log(EventLevel.Trace, "Match", items.size());
				return true;
			}
		});
	}

	@Override
	public Object process(String command, Map params) throws Exception {
		Object ret = null;

		switch (command) {
		case XDC_CMD_GEN_Init:
			initModule(params);
			break;
		case XDC_CMD_GEN_SELECT:
			updateDescPanel(params.get(XDC_EXT_TOKEN_id));
			break;
		case XDC_CMD_GEN_ACTIVATE:
			String selId = XbrlDockUtils.simpleGet(params.get(XDC_EXT_TOKEN_value), XDC_EXT_TOKEN_id);
			XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_gui, XDC_CMD_GEN_SELECT, XbrlDockUtils.setParams(XDC_GUICMD_WBAGENT, store, XDC_EXT_TOKEN_id, selId));
			break;
		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}

		return ret;

	}

}
