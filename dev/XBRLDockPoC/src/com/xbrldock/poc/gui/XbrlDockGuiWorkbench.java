package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "rawtypes", /* "unchecked" */ })
public class XbrlDockGuiWorkbench extends JFrame implements XbrlDockGuiConsts, XbrlDockConsts.GenAgent {
	private static final long serialVersionUID = 1L;

	DefaultMutableTreeNode rootNode;
	DefaultTreeModel panelModel;
	JTree panelTree;
	JPanel pnlRight;

	class WBNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 1L;

		JComponent comp;

		public WBNode(Object o) {
			super(o);
		}

		@Override
		public String toString() {
			return XbrlDockUtils.simpleGet(getUserObject(), XDC_EXT_TOKEN_name);
		}

		JComponent getComp() {
			if (null == comp) {
				try {
					comp = XbrlDockUtils.createObject(null, (Map) getUserObject());
				} catch (Exception e) {
					XbrlDockException.wrap(e, "Workbench node component creation", getUserObject());
				}
			}

			return comp;
		}
	}

	ActionListener al = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			}
		}
	};
	
	TreeSelectionListener tsl = new TreeSelectionListener() {
		
		@Override
		public void valueChanged(TreeSelectionEvent e) {
			Object o = panelTree.getSelectionPath().getPath()[1];
			
			if ( o instanceof WBNode ) {
				JComponent comp = ((WBNode)o).getComp();
				
				pnlRight.removeAll();
				pnlRight.add(comp, BorderLayout.CENTER);
				
				pnlRight.revalidate();
				pnlRight.repaint();
			}
		}
	};

	public XbrlDockGuiWorkbench() throws Exception {
		rootNode = new DefaultMutableTreeNode();
		panelModel = new DefaultTreeModel(rootNode);
		panelTree = new JTree(panelModel);
		panelTree.setRootVisible(false);

		TreeSelectionModel tsm = panelTree.getSelectionModel();
		tsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tsm.addTreeSelectionListener(tsl);

		pnlRight = new JPanel(new BorderLayout());

		Container cp = getContentPane();
		cp.add(XbrlDockUtilsGui.createSplit(true, new JScrollPane(panelTree), pnlRight, 0.2), BorderLayout.CENTER);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	@Override
	public <RetType> RetType process(String command, Object... params) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initModule(Map config) throws Exception {
		pack();

		setTitle(XbrlDockUtils.simpleGet(config, XDC_EXT_TOKEN_name));
		XbrlDockGuiUtils.placementLoad(this, config);

		rootNode.removeAllChildren();

		for (Object m : (Collection) XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_members)) {
			rootNode.add(new WBNode(m));
		}

		panelModel.reload();

		setVisible(true);
	}

}
