package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
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
	XbrlDockGuiWidgetGrid factGrid = new XbrlDockGuiWidgetGrid( 
			ListSelectionModel.SINGLE_SELECTION, new String[] {XDC_GRIDCOL_ROWNUM},
			
			new LabeledAccess("Entity", fmtCtx, XDC_FACT_TOKEN_entity), 
			new LabeledAccess("Instant", fmtCtx, XDC_FACT_TOKEN_instant),
			new LabeledAccess("Start", fmtCtx, XDC_EXT_TOKEN_startDate),
			new LabeledAccess("End", fmtCtx, XDC_EXT_TOKEN_endDate),
			new LabeledAccess("Concept", "", XDC_FACT_TOKEN_concept),
			new LabeledAccess("Value", "", XDC_EXT_TOKEN_value),
			new LabeledAccess("Sign", "", XDC_FACT_TOKEN_sign),
			new LabeledAccess("Scale", "", XDC_FACT_TOKEN_scale),
			new LabeledAccess("Decimals", "", XDC_FACT_TOKEN_decimals)
	);
//@formatter:on

	GenAgent ctxTreeLoader = new GenAgent() {
		DefaultMutableTreeNode lastHead = null;

		@Override
		public Object process(String cmd, Map params) throws Exception {
			if (XDC_CMD_GEN_Process.equals(cmd)) {
				DefaultMutableTreeNode root = (DefaultMutableTreeNode) params.get(XDC_EXT_TOKEN_value);
				
				Set<String> emptyKeys = new TreeSet<>();

				for (String hKey : ctxHierarchy.keySet()) {
					DefaultMutableTreeNode n = new DefaultMutableTreeNode(hKey);
					int sep = hKey.indexOf(":");

					if (-1 == sep) {
						root.add(n);
						lastHead = n;
					} else {
						if ( null == lastHead ) {
							String hk = hKey.substring(0, sep);
							emptyKeys.add(hk);
							lastHead = new DefaultMutableTreeNode(hk);
							root.add(lastHead);
						}
						lastHead.add(n);
					}
				}
				
				for ( String ek : emptyKeys ) {
					ctxHierarchy.put(ek, Collections.EMPTY_SET);
				}
			}
			return true;
		}
	};

	TreeSelectionListener ctxTreeSelListener = new TreeSelectionListener() {
		@Override
		public void valueChanged(TreeSelectionEvent e) {
			Object sel = ((TreeSelectionModel) e.getSource()).getSelectionPath().getLastPathComponent();
			if (sel instanceof DefaultMutableTreeNode) {
				ctxFilter = ctxHierarchy.get(((DefaultMutableTreeNode) sel).getUserObject());
			} else {
				ctxFilter = null;
			}
			updateFactGrid();
		}
	};

//XbrlDockGuiUtilsTree ctxTree = new XbrlDockGuiUtilsTree(CTX_ROOT, true, TreeSelectionModel.SINGLE_TREE_SELECTION, ctxTreeSelListener);
	XbrlDockGuiWidgetTree ctxTree = new XbrlDockGuiWidgetTree(CTX_ROOT, true, TreeSelectionModel.SINGLE_TREE_SELECTION);

	GenAgent conceptTreeLoader = new GenAgent() {
		@Override
		public Object process(String cmd, Map params) throws Exception {
			DefaultMutableTreeNode item = (DefaultMutableTreeNode) params.get(XDC_EXT_TOKEN_value);
			if (XDC_CMD_GEN_Process.equals(cmd)) {
				for (Map.Entry<String, Set> ec : conceptHierarchy.entrySet()) {
					DefaultMutableTreeNode n = new DefaultMutableTreeNode(namespaces.get(ec.getKey()));
					item.add(n);
					for (Object o : ec.getValue()) {
						n.add(new DefaultMutableTreeNode(o));
					}
				}
			}
			return true;
		}
	};

	TreeSelectionListener conceptTreeSelListener = new TreeSelectionListener() {
		@Override
		public void valueChanged(TreeSelectionEvent e) {
			conceptFilter.clear();

			for (TreePath tp : ((TreeSelectionModel) e.getSource()).getSelectionPaths()) {
				Object sel = tp.getLastPathComponent();
				if (sel instanceof DefaultMutableTreeNode) {
					addNodeRec((DefaultMutableTreeNode) sel);
				}
			}
			updateFactGrid();
		}

		private void addNodeRec(DefaultMutableTreeNode sel) {
			conceptFilter.add(sel.getUserObject());
			for (Enumeration<TreeNode> ce = sel.children(); ce.hasMoreElements();) {
				addNodeRec((DefaultMutableTreeNode) ce.nextElement());
			}
		}
	};

//	XbrlDockGuiUtilsTree conceptTree = new XbrlDockGuiUtilsTree("", false, TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION, conceptTreeSelListener);
	XbrlDockGuiWidgetTree conceptTree = new XbrlDockGuiWidgetTree("", false, TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

	Map<String, Set> ctxHierarchy = new TreeMap<>();
	Set ctxFilter;

	Map<String, Set> conceptHierarchy = new TreeMap<>();
	Set conceptFilter = new HashSet();

	ArrayList<Map> facts = new ArrayList();
	ArrayList<Map> txts = new ArrayList();

	Map<String, Map<String, Object>> unitDef = new TreeMap<>();
	Map<String, Map<String, Object>> ctxDef = new TreeMap<>();

	Set<String> taxonomies = new TreeSet<>();
	Map<String, String> namespaces = new TreeMap<>();

	GenAgent dh = new GenAgent() {
		
		@Override
		public Object process(String cmd, Map<String, Object> params) throws Exception {
			switch (cmd) {
			case XDC_CMD_REP_ADD_NAMESPACE:
				namespaces.put((String) params.get(XDC_EXT_TOKEN_id), (String) params.get(XDC_EXT_TOKEN_value));				
				break;
			case XDC_CMD_REP_ADD_SCHEMA:
				taxonomies.add((String) params.get(XDC_EXT_TOKEN_id));
				break;
			case XDC_REP_SEG_Unit:
			case XDC_REP_SEG_Context:
			case XDC_REP_SEG_Fact:
				return processSegment(cmd, (Map<String, Object>) params.get(XDC_GEN_TOKEN_source));
			}
			return null;
		}

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
					key = XbrlDockUtils.sbAppend(null, "/", true, cloneData.get(XDC_EXT_TOKEN_startDate), cloneData.get(XDC_EXT_TOKEN_endDate)).toString();
				}

				String dim = XbrlDockUtils.toString(cloneData.get(XDC_FACT_TOKEN_dimensions));
				if (!XbrlDockUtils.isEmpty(dim)) {
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

				String concept = XbrlDockUtils.simpleGet(data, XDC_FACT_TOKEN_concept);
				int sep = concept.indexOf(":");

				XbrlDockUtils.safeGet(conceptHierarchy, concept.substring(0, sep), SORTEDSET_CREATOR).add(concept);

				break;
			}

			return ret;
		}
	};

	public XbrlDockGuiReportPanel() throws Exception {
		super(new BorderLayout());

//		factGrid.setShowRowNum(true);
		JPanel pnlLeft = new JPanel(new BorderLayout());

		pnlLeft.add(XbrlDockUtilsGui.createSplit(false, conceptTree.getComp(), ctxTree.getComp(), 0.4), BorderLayout.CENTER);
		add(XbrlDockUtilsGui.createSplit(true, pnlLeft, factGrid.getComp(), 0.2), BorderLayout.CENTER);
	}

	@Override
	public Object process(String command, Map params) throws Exception {
		Object ret = null;

		switch (command) {
		case XDC_CMD_GEN_SETMAIN:
			ctxHierarchy.clear();

			XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_esefConn, XDC_CMD_CONN_VISITREPORT, XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, params.get(XDC_EXT_TOKEN_id), XDC_GEN_TOKEN_processor, dh));

			ctxTree.updateItems(true, ctxTreeLoader);
			conceptTree.updateItems(true, conceptTreeLoader);

			updateFactGrid();

			break;
		case XDC_CMD_GEN_Init:
			break;
		case XDC_CMD_GEN_SELECT:
			// fact selected
			break;
		case XDC_CMD_GEN_ACTIVATE:
			// fact activated
			break;
		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}

		return ret;

	}

	private void updateFactGrid() {
		factGrid.updateItems(true, new GenAgent() {
			
			@Override
			public Object process(String cmd, Map params) throws Exception {
				ArrayList items = (ArrayList) params.get(XDC_GEN_TOKEN_members);
				for (Object f : facts) {
					Object ctx = XbrlDockUtils.simpleGet(f, XDC_FACT_TOKEN_context);
					if ((null == ctxFilter) || ctxFilter.contains(ctx)) {
						Object concept = XbrlDockUtils.simpleGet(f, XDC_FACT_TOKEN_concept);
						if (conceptFilter.isEmpty() || conceptFilter.contains(concept)) {
							items.add(f);
						}
					}
				}
				return true;
			}
		});
	}

}
