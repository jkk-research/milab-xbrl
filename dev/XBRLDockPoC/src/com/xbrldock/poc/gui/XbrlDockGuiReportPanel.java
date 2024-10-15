package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts.GenAgent;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.conn.xbrlorg.XbrlDockConnXbrlOrgConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockGuiReportPanel extends JPanel implements XbrlDockGuiConsts, XbrlDockConnXbrlOrgConsts, GenAgent {
	private static final String CTX_ROOT = "<< ALL >>";

	private static final long serialVersionUID = 1L;

	ObjectFormatter fmtCtx = new ObjectFormatter() {
		@Override
		public String toString(Object value, Object root, Object... hints) {
			String ctxId = XbrlDockUtils.simpleGet(root, XDC_FACT_TOKEN_context);
			Map<String, Object> ctx = ctxDef.get(ctxId);
			return XbrlDockUtils.simpleGet(ctx, hints);
		}
	};
//@formatter:off  
	XbrlDockGuiUtilsGrid factGrid = new XbrlDockGuiUtilsGrid(this, 
			new LabeledAccess("Entity", fmtCtx, XDC_FACT_TOKEN_entity), 
			new LabeledAccess("Instant", fmtCtx, XDC_FACT_TOKEN_instant),
			new LabeledAccess("Start", fmtCtx, XDC_FACT_TOKEN_startDate),
			new LabeledAccess("End", fmtCtx, XDC_FACT_TOKEN_endDate),
			new LabeledAccess("Concept", "", XDC_FACT_TOKEN_concept),
			new LabeledAccess("Value", "", XDC_GEN_TOKEN_value),
			new LabeledAccess("Sign", "", XDC_FACT_TOKEN_sign),
			new LabeledAccess("Scale", "", XDC_FACT_TOKEN_scale),
			new LabeledAccess("Decimals", "", XDC_FACT_TOKEN_decimals)
	);
//@formatter:on

	DefaultMutableTreeNode ctxRoot;
	DefaultTreeModel ctxModel;
	JTree ctxTree;

	Map<String, Set> ctxHierarchy = new TreeMap<>();
	Set ctxFilter;

	ArrayList<Map> facts = new ArrayList();
	ArrayList<Map> txts = new ArrayList();

	Map<String, Map<String, Object>> unitDef = new TreeMap<>();
	Map<String, Map<String, Object>> ctxDef = new TreeMap<>();

	Set<String> taxonomies = new TreeSet<>();
	Map<String, String> namespaces = new TreeMap<>();

	ReportDataHandler dh = new ReportDataHandler() {

		@Override
		public void beginReport(String repId) {
			// TODO Auto-generated method stub

		}

		@Override
		public void addTaxonomy(String tx) {
			taxonomies.add(tx);
		}

		@Override
		public void addNamespace(String ref, String id) {
			namespaces.put(ref, id);
		}

		@Override
		public String processSegment(String segment, Map<String, Object> data) {
			String ret = "";

			TreeMap<String, Object> cloneData = new TreeMap<String, Object>(data);

			switch (segment) {
			case XDC_REP_SEG_Unit:
				ret = (String) data.get(XDC_FACT_TOKEN_unit);
				if (XbrlDockUtils.isEmpty(ret)) {
					ret = "unit-" + unitDef.size();
				}
				unitDef.put(ret, cloneData);
				break;
			case XDC_REP_SEG_Context:
				ret = (String) data.get(XDC_FACT_TOKEN_context);
				if (XbrlDockUtils.isEmpty(ret)) {
					ret = "ctx-" + ctxDef.size();
				}
				ctxDef.put(ret, cloneData);

				String key = (String) cloneData.get(XDC_FACT_TOKEN_instant);
				if (XbrlDockUtils.isEmpty(key)) {
					key = XbrlDockUtils.sbAppend(null, "/", true, cloneData.get(XDC_FACT_TOKEN_startDate), cloneData.get(XDC_FACT_TOKEN_endDate)).toString();
				}

				Object dim = cloneData.get(XDC_FACT_TOKEN_dimensions);
				if (null != dim) {
					key = key + ":" + dim.toString();
				}

				XbrlDockUtils.safeGet(ctxHierarchy, key, SET_CREATOR).add(ret);

				// cloneData.put(XDC_EXT_TOKEN_name, key);

				break;
			case XDC_REP_SEG_Fact:
				ret = (String) data.get(XDC_EXT_TOKEN_id);
				if (XbrlDockUtils.isEmpty(ret)) {
					ret = "fact-" + facts.size();
				}

				if (XbrlDockUtils.isEqual(XDC_FACT_VALTYPE_text, data.get(XDC_FACT_TOKEN_xbrldockFactType))) {
					txts.add(cloneData);
				} else {
					facts.add(cloneData);
				}
				break;
			}

			return ret;
		}

		@Override
		public void endReport() {
			// TODO Auto-generated method stub

		}
	};

	public XbrlDockGuiReportPanel() throws Exception {
		super(new BorderLayout());

		ctxRoot = new DefaultMutableTreeNode(CTX_ROOT);
		ctxModel = new DefaultTreeModel(ctxRoot);
		ctxTree = new JTree(ctxModel);
		ctxTree.setRootVisible(true);

		TreeSelectionModel tsm = ctxTree.getSelectionModel();
		tsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		tsm.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				Object sel = ctxTree.getSelectionPath().getLastPathComponent();
				if ( sel instanceof DefaultMutableTreeNode ) {
					ctxFilter = ctxHierarchy.get(((DefaultMutableTreeNode)sel).getUserObject());
				} else {
					ctxFilter = null;
				}
				updateFactGrid();
			}
		});

		factGrid.setShowRowNum(true);
		add(XbrlDockUtilsGui.createSplit(true, new JScrollPane(ctxTree), factGrid.getComp(), 0.2), BorderLayout.CENTER);
	}

	@Override
	public void initModule(Map config) throws Exception {

	}

	@Override
	public Object process(String command, Object... params) throws Exception {
		Object ret = null;

		switch (command) {
		case XDC_CMD_GEN_SETMAIN:
			ctxHierarchy.clear();

			XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_esefConn, XDC_CMD_CONN_VISITREPORT, params[1], dh);

			updateFactGrid();

			DefaultMutableTreeNode lastHead = null;

			ctxRoot.removeAllChildren();
			for (String hKey : ctxHierarchy.keySet()) {
				DefaultMutableTreeNode n = new DefaultMutableTreeNode(hKey);
				int sep = hKey.indexOf(":");

				if (-1 == sep) {
					ctxRoot.add(n);
					lastHead = n;
				} else {
					lastHead.add(n);
				}
			}

			ctxModel.reload();

			break;
		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}

		return ret;

	}

	private void updateFactGrid() {
		factGrid.updateItems(true, new GenProcessor<ArrayList>() {
			@Override
			public boolean process(ArrayList items, ProcessorAction action) throws Exception {
				for (Object f : facts) {
					if ((null == ctxFilter) || ctxFilter.contains(XbrlDockUtils.simpleGet(f, XDC_FACT_TOKEN_context))) {
						items.add(f);
					}
				}
				return true;
			}
		});
	}

}
