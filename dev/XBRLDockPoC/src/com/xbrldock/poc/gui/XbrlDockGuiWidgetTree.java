package com.xbrldock.poc.gui;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.xbrldock.XbrlDockException;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockGuiWidgetTree implements XbrlDockGuiConsts.Widget<JComponent>, XbrlDockGuiConsts {

	private DefaultMutableTreeNode rootNode;
	private DefaultTreeModel model;
	private JTree tree;
	private TreeSelectionModel tsm;

	private final Set selection = new HashSet();

	private JComponent comp;

	public XbrlDockGuiWidgetTree(Object rootOb, boolean rootVisible, int selMode) {
		rootNode = new DefaultMutableTreeNode(rootOb);
		model = new DefaultTreeModel(rootNode);
		tree = new JTree(model);
		tree.setRootVisible(rootVisible);

		tsm = tree.getSelectionModel();
		tsm.setSelectionMode(selMode);

		comp = new JScrollPane(tree);
	}

	@Override
	public void setActionListener(ActionListener al, String... guiCmds) {
		for (String gc : guiCmds) {
			switch (gc) {
			case XDC_GUICMD_PICK:
				tsm.addTreeSelectionListener(new TreeSelectionListener() {
					@Override
					public void valueChanged(TreeSelectionEvent e) {
						Object selItem = tree.getSelectionPath().getLastPathComponent();
						al.actionPerformed(new WidgetEvent(XbrlDockGuiWidgetTree.this, XDC_GUICMD_PICK, selItem));
					}
				});
				break;
			case XDC_GUICMD_SELCHG:
				if (tsm.getSelectionMode() != TreeSelectionModel.SINGLE_TREE_SELECTION) {
					tsm.addTreeSelectionListener(new TreeSelectionListener() {
						@Override
						public void valueChanged(TreeSelectionEvent e) {
							selection.clear();
							TreePath[] sps = tree.getSelectionPaths();
							if (null != sps) {
								for (TreePath tp : sps) {
									selection.add(tp.getLastPathComponent());
								}
							}
							al.actionPerformed(new WidgetEvent(XbrlDockGuiWidgetTree.this, XDC_GUICMD_PICK, selection));
						}
					});
				}
				break;
			case XDC_GUICMD_ACTIVATE:
				tree.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (1 < e.getClickCount()) {
							Object selItem = tree.getSelectionPath().getLastPathComponent();
							al.actionPerformed(new WidgetEvent(XbrlDockGuiWidgetTree.this, XDC_GUICMD_ACTIVATE, selItem));
						}
					}
				});
				break;
			}
		}
	}

	public void setItemFormatter(ObjectFormatter<Object> fmt) {
		TreeCellRenderer tcr = new DefaultTreeCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				Object ob = ((DefaultMutableTreeNode)value).getUserObject();

				return super.getTreeCellRendererComponent(tree, fmt.toString(ob, null), sel, expanded, leaf, row, hasFocus);
			}
		};
		
		setItemRenderer(tcr);
	}

	public void setItemRenderer(TreeCellRenderer tcr) {
		tree.setCellRenderer(tcr);
	}

	public DefaultMutableTreeNode getRootNode() {
		return rootNode;
	}

	public void updateItems(boolean clear, GenAgent loader) {
		if (clear) {
			rootNode.removeAllChildren();
		}

		try {
			loader.process(XDC_CMD_GEN_Process, rootNode);
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