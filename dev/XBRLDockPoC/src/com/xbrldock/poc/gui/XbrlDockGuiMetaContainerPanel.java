package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts.GenAgent;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.meta.XbrlDockMetaConsts;
import com.xbrldock.poc.meta.XbrlDockMetaContainer;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockGuiMetaContainerPanel extends JPanel implements XbrlDockGuiConsts, XbrlDockMetaConsts, GenAgent {
	private static final String CB_ALL = "<< all >>";
	private static final String CB_LANG_ID = "<< id >>";

	private static final long serialVersionUID = 1L;

	XbrlDockMetaContainer taxonomy;
	Map<String, Set<String>> entryPointUrls = new TreeMap<>();

	String lang;
	String selRoleType;
	Set<String> urlFilter;

	GenAgent roleTreeLoader = new GenAgent() {
		private DefaultMutableTreeNode toTreeNode(RoleTreeNode n) {
			DefaultMutableTreeNode ret = new DefaultMutableTreeNode(n);

			for (RoleTreeNode cn : n.children()) {
				ret.add(toTreeNode(cn));
			}
			return ret;
		}

		@Override
		public Object process(String cmd, Object... params) throws Exception {
			DefaultMutableTreeNode item = (DefaultMutableTreeNode ) params[0];
			if (XDC_CMD_GEN_Process.equals(cmd)) {
				Map<String, RoleTree> rtm = taxonomy.getRoleTreeMap(selRoleType);

				for (RoleTree rt : rtm.values()) {
					item.add(toTreeNode(rt));
				}
			}

			return true;
		}
	};

	JComboBox<String> cbRoleType = new JComboBox<String>();
	JComboBox<String> cbLang = new JComboBox<String>();
	JComboBox<String> cbEntryPoint = new JComboBox<String>();

	XbrlDockGuiWidgetTree roleTree = new XbrlDockGuiWidgetTree("", false, TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

	ObjectFormatter<Object> roleItemFormatter = new ObjectFormatter<Object>() {
		@Override
		public String toString(Object value, Object root, Object... hints) {
			String ret = null;

			if (value instanceof RoleTreeNode) {
				Map<String, String> item = ((RoleTreeNode) value).item;
				if ( null == item ) {
					return "???";
				}
				ret = item.get("roleURI");

				if (XbrlDockUtils.isEmpty(ret)) {
					if (null == lang ) {
						ret = item.get("id");
					} else {
						ret = item.get("id");
//						taxonomy.getItemLabel(ret);
					}
				}
			} else {
				ret = XbrlDockUtils.toString(value);
			}

			return ret;
		}
	};

//@formatter:off  
	XbrlDockGuiWidgetGrid itemGrid = new XbrlDockGuiWidgetGrid(
			ListSelectionModel.SINGLE_SELECTION, new String[] {XDC_GRIDCOL_ROWNUM},
			
			new LabeledAccess("Name", "", XDC_EXT_TOKEN_name), 
			new LabeledAccess("Group", "", "substitutionGroup"),
			new LabeledAccess("Type", "", "type"), 
			new LabeledAccess("Period", "", "xbrli:periodType"),
			new LabeledAccess("Balance", "", "xbrli:balance")
	);
//@formatter:on

	XbrlDockGuiMetaItemPanel itemPanel = new XbrlDockGuiMetaItemPanel();

	ActionListener al = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			case XDC_CMD_GEN_SETLANG:
				lang = (String) cbLang.getSelectedItem();
				if (CB_LANG_ID.equals(lang)) {
					lang = null;
				}
				break;
			case XDC_APP_SETROLETYPE:
				selRoleType = (String) cbRoleType.getSelectedItem();
				updateRoleTree();
				break;
			case XDC_APP_SETENTRYPOINT:
				String ep = (String) cbEntryPoint.getSelectedItem();
				urlFilter = entryPointUrls.get(ep);
				updateRoleTree();
				break;
			}
		}
	};

	public XbrlDockGuiMetaContainerPanel() throws Exception {
		super(new BorderLayout());

		roleTree.setActionListener(al, XDC_GUICMD_SELCHG);
		roleTree.setItemFormatter(roleItemFormatter);

		JPanel pnlTop = new JPanel(new BorderLayout());
		pnlTop.add(XbrlDockGuiUtils.setActive(cbLang, XDC_CMD_GEN_SETLANG, al), BorderLayout.EAST);
		pnlTop.add(XbrlDockGuiUtils.setActive(cbEntryPoint, XDC_APP_SETENTRYPOINT, al), BorderLayout.CENTER);

		JPanel pnlTree = new JPanel(new BorderLayout());
		pnlTree.add(XbrlDockGuiUtils.setActive(cbRoleType, XDC_APP_SETROLETYPE, al), BorderLayout.NORTH);
		pnlTree.add(XbrlDockUtilsGui.setTitle(roleTree.getComp(), "Taxonomy tree"), BorderLayout.CENTER);

		JPanel pnlRight = new JPanel(new BorderLayout());
		pnlRight.add(XbrlDockUtilsGui.createSplit(false, itemGrid.getComp(), XbrlDockUtilsGui.setTitle(itemPanel, "Selected item info"), 0.5), BorderLayout.CENTER);

		add(pnlTop, BorderLayout.NORTH);
		add(XbrlDockUtilsGui.createSplit(true, XbrlDockUtilsGui.setTitle(pnlTree, "Taxonomy tree"), XbrlDockUtilsGui.setTitle(pnlRight, "Related items"), 0.2),
				BorderLayout.CENTER);
	}

	@Override
	public Object process(String command, Object... params) throws Exception {
		Object ret = null;
		switch (command) {
		case XDC_CMD_GEN_Init:
			break;
		case XDC_CMD_GEN_SETMAIN:
			showTaxonomy((String) params[1]);
			break;
		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}

		return ret;
	}

	private void showTaxonomy(String taxonomyId) throws Exception {
		taxonomy = XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_GETMC, taxonomyId);

		Map mi = taxonomy.getMetaInfo();

		cbLang.removeAllItems();
		cbLang.addItem(CB_LANG_ID);
		Collection<String> l = (Collection<String>) mi.getOrDefault(XDC_FACT_TOKEN_language, Collections.EMPTY_LIST);
		for (String lang : l) {
			cbLang.addItem(lang);
		}
		cbLang.setSelectedItem(CB_LANG_ID);

		cbRoleType.removeAllItems();
		for (String rt : taxonomy.getRoleTypes()) {
			cbRoleType.addItem(rt);
		}
//		cbRoleType.setSelectedIndex(0);
//		selRoleType = cbRoleType.getItemAt(0);

		cbEntryPoint.removeAllItems();
		cbEntryPoint.addItem(CB_ALL);
		Collection<Map> ep = (Collection<Map>) mi.getOrDefault(XDC_METAINFO_entryPoints, Collections.EMPTY_LIST);
		for (Map ee : ep) {
			String epName = (String) ee.getOrDefault(XDC_EXT_TOKEN_name, "Entry point " + cbEntryPoint.getItemCount());
			cbEntryPoint.addItem(epName);

			Set<String> fls = new TreeSet<String>();
			Collection<String> eps = (Collection<String>) ee.getOrDefault(XDC_METAINFO_entryPointRefs, Collections.EMPTY_LIST);
			for (String epl : eps) {
				taxonomy.collectLinks(fls, XbrlDockUtils.getPostfix(epl, XDC_URL_PSEP));
			}

			entryPointUrls.put(epName, fls);
		}

//		updateRoleTree();
	}

	public void updateRoleTree() {
		roleTree.updateItems(true, roleTreeLoader);
	}
}
