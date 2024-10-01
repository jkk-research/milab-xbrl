package com.xbrldock.poc.meta;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Element;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevCounter;
import com.xbrldock.utils.XbrlDockUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockMetaContainer implements XbrlDockMetaConsts {
	
	final Map<String, Object> metaInfo;

	private Set<String> prefixes = new TreeSet<String>();

	private ArrayList<String> queue = new ArrayList<>();
	Map<String, String> queueNS = new TreeMap<>();
	private Set<String> loaded = new TreeSet<>();

	String currentUrl;
	String path;

	Map<String, Map<String, Object>> itemsByURL = new TreeMap<>();
	Map<String, Object> itemsByNS = new TreeMap<>();
	boolean itemMapClosed;
	ItemCreator<Map> itemCreator = new ItemCreatorSimple<Map>(TreeMap.class) {
		@Override
		public Map create(Object key, Object... hints) {
			if (itemMapClosed) {
				XbrlDockException.wrap(null, "Item creation closed", key, hints[0]);
			}
			Map ret = super.create(key, hints);
			return ret;
		}
	};

	Map<String, Object> labels = new TreeMap<>();

	ArrayList<Map> allRefs = new ArrayList<>();
	Map<String, Set> refRefs = new TreeMap<>();
	ArrayList<Map<String, String>> links = new ArrayList<>();

	XbrlDockDevCounter cntLinkTypes = new XbrlDockDevCounter("LinkTypeCounts", true);
	XbrlDockDevCounter cntArcRoles = new XbrlDockDevCounter("ArcRoleCounts", true);

	public XbrlDockMetaContainer(Map<String, Object> mi) {
		this.metaInfo = mi;
	}

	public void addPrefix(String p) {
		prefixes.add(p);
	}

	public Map getUrlContent(String itemUrl) {
		Map ret = null;

		boolean supported = false;

		for (String p : prefixes) {
			if (itemUrl.startsWith(p)) {
				supported = true;
				break;
			}
		}

		if (supported) {
			/// try to find
		}

		return ret;
	}

	void setUrlContent(Map content) throws Exception {
	}

	Map getItem(Element e) throws Exception {
		String itemRef = e.getAttribute("xlink:href");

		int sp = itemRef.lastIndexOf("#");
		String id = itemRef.substring(sp + 1);
		String realRef = itemRef.substring(0, sp);

		return getItem(realRef, id, null);
	}

	Map getItem(String id, String callerNamespace) throws Exception {
		return getItem(currentUrl, id, callerNamespace);
	}

	Map getItem(String itemUrl, String id, String callerNamespace) throws Exception {
		String realUrl = XbrlDockUtils.optExtendRef(itemUrl, path);

		optQueue(realUrl, callerNamespace);

		Map m = XbrlDockUtils.safeGet(itemsByURL, id, itemCreator, realUrl);

		return m;
	}

//	public void storeItem(String url, String ns, Map item) {
//		itemsByURL.put(url, item);
//		itemsByNS.put(XbrlDockUtils.sbAppend(null, ":", true, ns, item.get("name")).toString(), item);
//	}

	String getQueuedItem() {
		if (queue.isEmpty()) {
			return null;
		}

		currentUrl = queue.remove(0);
		loaded.add(currentUrl);

		path = XbrlDockUtils.cutPostfix(currentUrl, "/");

		return currentUrl;
	}

	String getQueuedNS(String url) {
		return queueNS.remove(url);
	}

	String getCurrentUrl() {
		return currentUrl;
	}

	boolean optQueue(String url, String targetNs) {
		url = XbrlDockUtils.optExtendRef(url, path);

		if (loaded.contains(url) || queue.contains(url)) {
			return false;
		}

		int idx = -1;
		if (url.endsWith(XDC_FEXT_XML)) {
			idx = queue.size();
		} else if (url.endsWith(XDC_FEXT_SCHEMA)) {
			idx = 0;
		} else {
			XbrlDock.log(EventLevel.Warning, "Strange extension", url);
		}

		if (-1 != idx) {
			queue.add(idx, url);
			if (!XbrlDockUtils.isEmpty(targetNs)) {
				queueNS.put(url, targetNs);
			}
			return true;
		}

		return false;
	}

	int storeDocumentRef(Map rm) {
		allRefs.add(rm);
		return allRefs.size() - 1;
	}

	void setDocumentRef(Object itemId, Object refIdx) {
		XbrlDockUtils.safeGet(refRefs, itemId, SET_CREATOR).add(refIdx);
	}

	void addLinkType(String lt) {
		cntLinkTypes.add(lt);
	}

	void addArcRole(String ar) {
		cntArcRoles.add(ar);
		cntArcRoles.add(" <TOTAL> ");
	}

	void setLabel(Object lang, Object itemId, String labelType, Object value) {
		XbrlDockUtils.simpleSet(labels, value, lang, itemId, labelType);
	}

	void addLink(Map<String, String> linkInfo) {
		links.add(linkInfo);
	}

//	public Map getItem(String itemId) {
//		String srcUrl = null;
//		String id = null;
//
//		int sep = itemId.indexOf('#');
//
//		if (-1 != sep) {
//			srcUrl = itemId.substring(0, sep);
//			id = itemId.substring(sep + 1);
//		}
//
//		Map<String, Map> ret = XbrlDockUtils.safeGet(items, srcUrl, itemMapCreator);
//
//		return ret.get(id);
//	}

//	@Override
//	public String toString() {
//		return refUrlCount.toString() + "\n" + unresolvedCount.toString();
//	}
}
