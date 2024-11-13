package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts.GenAgent;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.meta.XbrlDockMetaContainer;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockGuiMetaTaxonomyPanel extends JPanel implements XbrlDockGuiConsts, GenAgent {
	private static final long serialVersionUID = 1L;
	
	private static final String XDC_APP_SHOWITEMS = "showItems";


	XbrlDockMetaContainer taxonomy;
	Map<String, Set<String>> entryPointUrls = new TreeMap<>();

	XbrlDockGuiMetaRoleTree roleTree;
	XbrlDockGuiMetaItemInfoGrid itemGrid;

	JComboBox<String> cbType = new JComboBox<String>();
	JComboBox<String> cbLang = new JComboBox<String>();
	JComboBox<String> cbEntryPoint = new JComboBox<String>();

	ActionListener al = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			int li;

			switch (e.getActionCommand()) {
			case XDC_APP_SHOWITEMS:
				itemGrid.displayItems(roleTree.getRelatedItems());
				break;
			case XDC_APP_SETLANG:
//			int li = cbLang.getSelectedIndex();
//			taxonomy.setLang( (0 == li) ? null : cbLang.getItemAt(li));
				roleTree.invalidate();
				roleTree.revalidate();
				roleTree.repaint();
				break;
			case XDC_APP_SETROLETYPE:
				li = cbType.getSelectedIndex();
				roleTree.setRoleType( (0 == li) ? null : cbType.getItemAt(li) );
				break;
			case XDC_APP_SETENTRYPOINT:
				roleTree.setUrlSet(entryPointUrls.get(cbEntryPoint.getSelectedItem()));
			}
		}
	};

	public XbrlDockGuiMetaTaxonomyPanel() throws Exception {
		super(new BorderLayout());

		roleTree = new XbrlDockGuiMetaRoleTree();
		itemGrid = new XbrlDockGuiMetaItemInfoGrid();

		JPanel pnlTree = new JPanel(new BorderLayout());
		pnlTree.add(new JScrollPane(roleTree), BorderLayout.CENTER);

		cbType.setActionCommand(XDC_APP_SETROLETYPE);
		cbType.addActionListener(al);
		pnlTree.add(cbType, BorderLayout.NORTH);

		JPanel pnlTreeActions = new JPanel(new FlowLayout());

		JButton bt = new JButton(XDC_APP_SHOWITEMS);
		bt.setActionCommand(XDC_APP_SHOWITEMS);
		bt.addActionListener(al);
		pnlTreeActions.add(bt);

		pnlTree.add(pnlTreeActions, BorderLayout.SOUTH);

		JPanel pnlTop = new JPanel(new BorderLayout());

		cbLang.setActionCommand(XDC_APP_SETLANG);
		cbLang.addActionListener(al);
		pnlTop.add(cbLang, BorderLayout.EAST);

		cbEntryPoint.setActionCommand(XDC_APP_SETENTRYPOINT);
		cbEntryPoint.addActionListener(al);
		pnlTop.add(cbEntryPoint, BorderLayout.CENTER);

		add(pnlTop, BorderLayout.NORTH);
		add(XbrlDockUtilsGui.createSplit(true, pnlTree, itemGrid, 0.2), BorderLayout.CENTER);
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

	public void showTaxonomy(String taxonomyId) throws Exception {
//		JOptionPane.showMessageDialog(this, "Display taxonomy " + taxonomyId);

		taxonomy = XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_GETMC, taxonomyId);

		Map mi = taxonomy.getMetaInfo();

		cbLang.removeAllItems();
		cbLang.addItem("<< id >>");
		Collection<String> l = (Collection<String>) mi.getOrDefault(XDC_FACT_TOKEN_language, Collections.EMPTY_LIST);
		for (String lang : l) {
			cbLang.addItem(lang);
		}

		cbEntryPoint.removeAllItems();
		cbEntryPoint.addItem("<< all >>");
		Collection<Map> ep = (Collection<Map>) mi.getOrDefault(XDC_METAINFO_entryPoints, Collections.EMPTY_LIST);
		
		int cnt = 0;

		for (Map ee : ep) {
			++cnt;
			String epName = (String) ee.getOrDefault(XDC_EXT_TOKEN_name, "Entry point " + cnt);
			cbEntryPoint.addItem(epName);
			
			Set<String> fls = new TreeSet<String>();
			Collection<String> eps = (Collection<String>) ee.getOrDefault(XDC_METAINFO_entryPointRefs, Collections.EMPTY_LIST);
			for ( String epl : eps ) {
				taxonomy.collectLinks(fls, XbrlDockUtils.getPostfix(epl, XDC_URL_PSEP));
			}
			
			entryPointUrls.put(epName, fls);
		}

		roleTree.setTaxonomy(taxonomy);
		itemGrid.setTaxonomy(taxonomy);

		cbType.removeAllItems();
		cbType.addItem("<< all >>");
		for (String rt : roleTree.getRoleTypes() ) {
			cbType.addItem(rt);
		}

	}

}
