package com.xbrldock.poc.gui;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import com.xbrldock.poc.XbrlDockPoc;
import com.xbrldock.poc.taxonomy.XbrlDockTaxonomy;
import com.xbrldock.utils.XbrlDockUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockGuiRoleTree extends JTree implements XbrlDockGuiConsts {
	private static final long serialVersionUID = 1L;

	XbrlDockPoc xbrlDock;

	XbrlDockTaxonomy taxonomy;

	private static final ArrayList<ItemNode> LEAF = new ArrayList<>();

	class ItemNode implements TreeNode {

		TreeNode parent;

		String id;
		String label;

		Map item;
		ArrayList<Map<String, String>> roleLinks;

		ArrayList<ItemNode> children = null;

		public ItemNode() {
			this.parent = null;
			children = new ArrayList<>();
		}

		public ItemNode(String id, TreeNode parent, ArrayList<Map<String, String>> roleLinks) {
			this.parent = parent;

			this.id = id;
			this.item = taxonomy.getItem(id);
			this.roleLinks = roleLinks;

			label = id;
		}

		ArrayList<ItemNode> optLoadChildren() {
			if (null == children) {
				Set<String> roleItems = new TreeSet<>();

				for (Map<String, String> rl : roleLinks) {
					if (id.equals(rl.get("xlink:from"))) {
						roleItems.add(rl.get("xlink:to"));
					}
				}

				if (roleItems.isEmpty()) {
					children = LEAF;
				} else {
					children = new ArrayList<>();
					for (String rootItem : roleItems) {
						children.add(new ItemNode(rootItem, this, roleLinks));
					}
				}
			}

			return children;
		}

		@Override
		public TreeNode getChildAt(int childIndex) {
			return optLoadChildren().get(childIndex);
		}

		@Override
		public int getChildCount() {
			return optLoadChildren().size();
		}

		@Override
		public TreeNode getParent() {
			return parent;
		}

		@Override
		public int getIndex(TreeNode node) {
			return optLoadChildren().indexOf(node);
		}

		@Override
		public boolean getAllowsChildren() {
			return true;
		}

		@Override
		public boolean isLeaf() {
			return 0 == getChildCount();
		}

		@Override
		public Enumeration<? extends TreeNode> children() {
			return (Enumeration<? extends TreeNode>) children;
		}

		@Override
		public String toString() {
			return id;
		}

	}

	ItemNode tnRoot = new ItemNode();
	DefaultTreeModel tm = new DefaultTreeModel(tnRoot);

	public XbrlDockGuiRoleTree(XbrlDockPoc xbrlDock) {
		this.xbrlDock = xbrlDock;
		setRootVisible(false);

		setModel(tm);
	}

	public void showTaxonomy(XbrlDockTaxonomy taxonomy) throws Exception {

		this.taxonomy = taxonomy;

		tnRoot.children.clear();

		Map<String, ItemNode> roles = new TreeMap<>();

		for (Map<String, String> l : taxonomy.getLinks()) {
			String roleName = l.get("xlink:role");
			ItemNode rn = XbrlDockUtils.safeGet(roles, roleName, new XbrlDockUtils.ItemCreator<ItemNode>() {
				@Override
				public ItemNode create(Object key, Object... hints) {
					return new ItemNode((String) key, tnRoot, new ArrayList<Map<String, String>>());
				}
			});

			rn.roleLinks.add(l);
		}

		Set<String> roleItems = new TreeSet<>();

		for (ItemNode rn : roles.values()) {
			roleItems.clear();

			for (Map<String, String> rl : rn.roleLinks) {
				roleItems.add(rl.get("xlink:from"));
			}
			for (Map<String, String> rl : rn.roleLinks) {
				roleItems.remove(rl.get("xlink:to"));
			}

			if (roleItems.isEmpty()) {
				rn.children = LEAF;
			} else {
				rn.children = new ArrayList<>();
				for (String rootItem : roleItems) {
					rn.children.add(new ItemNode(rootItem, rn, rn.roleLinks));
				}
			}
			
			tnRoot.children.add(rn);
		}

		tm.reload();

	}

}
