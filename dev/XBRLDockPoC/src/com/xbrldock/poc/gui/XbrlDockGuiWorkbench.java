package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "rawtypes", /* "unchecked" */ })
public class XbrlDockGuiWorkbench extends JFrame implements XbrlDockGuiConsts, XbrlDockConsts.GenAgent {
	private static final long serialVersionUID = 1L;

	class ChildFrame extends JFrame {
		private static final long serialVersionUID = 1L;

		WBNode node;

		ChildFrame(WBNode node, JComponent mainComp) {
			getContentPane().add(mainComp, BorderLayout.CENTER);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			addWindowStateListener(childWindowListener);

			pack();
		}
	}

	Map<Object, Map> childConfigs;

	WBNode rootNode;
	DefaultTreeModel panelModel;
	JTree panelTree;
	JPanel pnlRight;

	WindowAdapter childWindowListener = new WindowAdapter() {
		@Override
		public void windowClosing(WindowEvent e) {
			WBNode wbn = ((ChildFrame) e.getWindow()).node;

			wbn.removeFromParent();
		}
	};

	class WBNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 1L;

		String id;
		JComponent comp;
		ChildFrame frm;

		public WBNode(String id, Object o) {
			super(o);
			this.id = id;
		}

		@Override
		public String toString() {
			return XbrlDockUtils.simpleGet(getUserObject(), XDC_EXT_TOKEN_name);
		}

		WBNode getChildById(String id, ItemCreator<WBNode> ic, Object... params) {
			WBNode wbn;

			if (0 < getChildCount()) {
				for (Object n : children) {
					wbn = (WBNode) n;
					if (XbrlDockUtils.isEqual(wbn.id, id)) {
						return wbn;
					}
				}
			}
			wbn = ic.create(id, params);

			add(wbn);

			return wbn;
		}

		void focusFrame() {
			frm.setVisible(true);
			frm.toFront();
			frm.requestFocus();
		}

		JComponent getComp(Map config) {
			if (null == comp) {
				try {
					comp = XbrlDockUtils.createObject(config);
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
			Object[] sp = panelTree.getSelectionPath().getPath();

			switch (sp.length) {
			case 2:
				selectRightPanel((WBNode) sp[1]);
				break;
			}
		}
	};

	ItemCreator<WBNode> childCreator = new ItemCreator<XbrlDockGuiWorkbench.WBNode>() {
		@Override
		public WBNode create(Object key, Object... hints) {
			try {
				WBNode n = new WBNode((String) key, null);
				Map cfg = childConfigs.get(hints[0]);
				JComponent mainPanel = n.getComp(cfg);

				if (mainPanel instanceof GenAgent) {
					((GenAgent) mainPanel).process(XDC_CMD_GEN_SELECT, hints);
				}
				n.frm = new ChildFrame(n, mainPanel);
				return n;
			} catch (Throwable e) {
				return XbrlDockException.wrap(e, "Activating child frame", key, hints);
			}
		}
	};

	public XbrlDockGuiWorkbench() throws Exception {
		rootNode = new WBNode("", Collections.EMPTY_MAP);
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
	public Object process(String command, Object... params) throws Exception {
		Object ret = null;
		switch (command) {
		case XDC_CMD_GEN_SELECT:
			WBNode wbn = rootNode.getChildById((String) params[0], null);

			if (params.length == 1) {
				TreePath tp = new TreePath(wbn);
				panelTree.setSelectionPath(tp);
				selectRightPanel(wbn);
			} else {
				wbn = wbn.getChildById((String) params[1], childCreator, params);

				wbn.focusFrame();

//				JOptionPane.showMessageDialog(this, XbrlDockUtils.sbAppend(new StringBuilder("Select hit"), " ", true, params));
			}
			break;
		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}

		return ret;
	}

	@Override
	public void initModule(Map config) throws Exception {
		pack();

		setTitle(XbrlDockUtils.simpleGet(config, XDC_EXT_TOKEN_name));
		XbrlDockGuiUtils.placementLoad(this, config);

		rootNode.removeAllChildren();

		for (Object m : (Collection) XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_members)) {
			rootNode.add(new WBNode(XbrlDockUtils.simpleGet(m, XDC_EXT_TOKEN_id), m));
		}

		childConfigs = XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_childPanels);

		panelModel.reload();

		setVisible(true);
	}

	private void selectRightPanel(WBNode wbn) {
		JComponent comp = wbn.getComp((Map) wbn.getUserObject());

		pnlRight.removeAll();
		pnlRight.add(comp, BorderLayout.CENTER);

		pnlRight.revalidate();
		pnlRight.repaint();
	}

}
