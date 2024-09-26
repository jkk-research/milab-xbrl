package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.xbrldock.poc.XbrlDockPoc;
import com.xbrldock.poc.taxonomy.XbrlDockTaxonomy;
import com.xbrldock.utils.XbrlDockUtilsGui;

//@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockGuiWorkbench extends JFrame implements XbrlDockGuiConsts {
	private static final long serialVersionUID = 1L;

	XbrlDockPoc xbrlDock;

	XbrlDockTaxonomy taxonomy;

	XbrlDockGuiRoleTree roleTree;
	XbrlDockGuiItemInfoGrid itemGrid;
	
	JComboBox<String> cbLang = new JComboBox<String>();
	JComboBox<String> cbEntryPoint = new JComboBox<String>();

	ActionListener al = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			case XDC_APP_SHOWITEMS:
				itemGrid.displayItems(roleTree.getRelatedItems());
				break;
			case XDC_APP_SETLANG:
				int li = cbLang.getSelectedIndex();
				taxonomy.setLang( (0 == li) ? null : cbLang.getItemAt(li));
				roleTree.invalidate();
				roleTree.revalidate();
				roleTree.repaint();
				break;
			}
		}
	};

	public XbrlDockGuiWorkbench(XbrlDockPoc xbrlDock) throws Exception {
		super("XBRLDock PoC Workbench");

		this.xbrlDock = xbrlDock;
		
		roleTree = new XbrlDockGuiRoleTree(xbrlDock);
		itemGrid = new XbrlDockGuiItemInfoGrid(xbrlDock);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void display() {
		pack();

//		Map cfg = XbrlDock.getSubConfig(null, XDC_CFGTOKEN_gui);
//
//		setLocation((int) XbrlDock.getConfig(cfg, 10, XDC_CFG_GEOM_location, XDC_CFG_GEOM_x), (int) XbrlDock.getConfig(cfg, 10, XDC_CFG_GEOM_location, XDC_CFG_GEOM_y));
//		setSize((int) XbrlDock.getConfig(cfg, 200, XDC_CFG_GEOM_dimension, XDC_CFG_GEOM_x), (int) XbrlDock.getConfig(cfg, 100, XDC_CFG_GEOM_dimension, XDC_CFG_GEOM_y));

		setLocation(50, 50);
		setSize(1000, 800);

		JPanel pnlTree = new JPanel(new BorderLayout());
		pnlTree.add(new JScrollPane(roleTree), BorderLayout.CENTER);
		
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


		Container cp = getContentPane();
		cp.add(pnlTop, BorderLayout.NORTH);
		cp.add(XbrlDockUtilsGui.createSplit(true, pnlTree, itemGrid, 0.2), BorderLayout.CENTER);

	}

	public void showTaxonomy(String taxonomyId) throws Exception {

		taxonomy = xbrlDock.getTaxMgr().loadTaxonomy(taxonomyId);

		roleTree.setTaxonomy(taxonomy);
		itemGrid.setTaxonomy(taxonomy);
		
		cbLang.removeAllItems();
		cbLang.addItem("<< id >>");
		for ( String lang : taxonomy.getLanguages() ) {
			cbLang.addItem(lang);
		}

		cbEntryPoint.removeAllItems();
		cbEntryPoint.addItem("<< all >>");
		for ( String ep : taxonomy.getEntryPoints() ) {
			cbEntryPoint.addItem(ep);
		}

		if (!isVisible()) {
			display();
			setVisible(true);
		}

	}

}
