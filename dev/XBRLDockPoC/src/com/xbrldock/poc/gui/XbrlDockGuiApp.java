package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import com.xbrldock.XbrlDock;
import com.xbrldock.poc.XbrlDockPoc;
import com.xbrldock.poc.taxonomy.XbrlDockTaxonomy;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockGuiApp extends JFrame implements XbrlDockGuiConsts {
	private static final long serialVersionUID = 1L;

	XbrlDockPoc xbrlDock;
	
	XbrlDockTaxonomy taxonomy;
	
	XbrlDockGuiRoleTree roleTree;

	public XbrlDockGuiApp(XbrlDockPoc xbrlDock) throws Exception {
		super("XBRLDock PoC");

		this.xbrlDock = xbrlDock;

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void display() {
		pack();

		Map cfg = XbrlDock.getSubConfig(null, ConfigKey.gui, ConfigKey.frame);

		setLocation((int) XbrlDock.getConfig(cfg, 10, ConfigKey.location, ConfigKey.x), (int) XbrlDock.getConfig(cfg, 10, ConfigKey.location, ConfigKey.y));
		setSize((int) XbrlDock.getConfig(cfg, 200, ConfigKey.dimension, ConfigKey.x), (int) XbrlDock.getConfig(cfg, 100, ConfigKey.dimension, ConfigKey.y));
		
		roleTree = new XbrlDockGuiRoleTree(xbrlDock);
		
		getContentPane().add(new JScrollPane(roleTree), BorderLayout.CENTER);
		

	}

	public void showTaxonomy(String taxonomyId) throws Exception {
		
		if ( !isVisible() ) {
			display();
			
			setVisible(true);
		}
		
		taxonomy = xbrlDock.getTaxMgr().loadTaxonomy(taxonomyId);
		
		roleTree.showTaxonomy(taxonomy);


	}

}
