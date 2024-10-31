package com.xbrldock.poc.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;

public interface XbrlDockMetaConsts extends XbrlDockPocConsts {

	String KEY_LOADED = "loaded";

	String XDC_TAXONOMYHEAD_FNAME = "taxonomyHead" + XDC_FEXT_JSON;
	String XDC_TAXONOMYDATA_FNAME = "taxonomyData" + XDC_FEXT_JSON;
	String XDC_TAXONOMYREFS_FNAME = "taxonomyRefs" + XDC_FEXT_JSON;
	String XDC_TAXONOMYRES_FNAME_PREFIX = "taxonomyRes_";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	class RoleTreeNode implements Comparable<RoleTreeNode>{
		public final String url;
		public final Map item;
		private Map upLink;
		
		private List<RoleTreeNode> children = Collections.EMPTY_LIST;
		private Double order;

		public RoleTreeNode(String url, Map item) {
			this.url = url;
			this.item = item;			
		}
		
		void setUpLink( Map upLink) {
			if ( null != this.upLink ) {
				XbrlDock.log(EventLevel.Warning, "Duplicate uplink info?", url, this.upLink, upLink);
			}
			
			this.upLink = upLink;
			order = Double.parseDouble((String) upLink.getOrDefault(XDC_EXT_TOKEN_order, "0"));
		}
		
		void addChild(RoleTreeNode tn) {
			if ( children.isEmpty() ) {
				children = new ArrayList<RoleTreeNode>();
			}
			children.add(tn);
			children.sort(null);
		}
		
		public Map getUpLink() {
			return upLink;
		}
		
		public Iterable<RoleTreeNode> children() {
			return children;
		}

		@Override
		public int compareTo(RoleTreeNode o) {
			return XbrlDockUtils.safeCompare(order, o.order);
		}
	}
	
	@SuppressWarnings({ "rawtypes" })
	class RoleTree extends RoleTreeNode {
		Map<String, RoleTreeNode> allNodes = new TreeMap<>();
		
		public RoleTree(String url, Map item) {
			super(url, item);
		}
	}

}
