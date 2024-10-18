package com.xbrldock.poc.gui;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import com.xbrldock.XbrlDockException;

@SuppressWarnings({ "rawtypes", "unchecked"})
public class XbrlDockGuiUtilsTree implements XbrlDockGuiConsts.ComponentWrapper<JComponent>, XbrlDockGuiConsts {
	
	private DefaultMutableTreeNode rootNode;
	private DefaultTreeModel model;
	private JTree tree;
	
	private JComponent comp;

	public XbrlDockGuiUtilsTree(Object rootOb, boolean rootVisible, int selMode, TreeSelectionListener tsl) {
		rootNode = new DefaultMutableTreeNode(rootOb);
		model = new DefaultTreeModel(rootNode);
		tree = new JTree(model);
		tree.setRootVisible(rootVisible);

		TreeSelectionModel tsm = tree.getSelectionModel();
		tsm.setSelectionMode(selMode);

		if ( null != tsl )
		tsm.addTreeSelectionListener(tsl);

		comp = new JScrollPane(tree);
	}
	
	public DefaultMutableTreeNode getRootNode() {
		return rootNode;
	}
	
	public void updateItems(boolean clear, GenProcessor loader) {
		if (clear) {
			rootNode.removeAllChildren();
		}

		try {
			loader.process(ProcessorAction.Process, rootNode);
		} catch (Exception e) {
			XbrlDockException.wrap(e, "Grid updateItems");
		}

		model.reload();
	}
	
	@Override
	public JComponent getComp() {
		return comp;
	}

}