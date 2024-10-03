package com.xbrldock.poc.meta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Element;

import com.xbrldock.XbrlDock;
import com.xbrldock.dev.XbrlDockDevCounter;
import com.xbrldock.poc.utils.XbrlDockPocUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsJson;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockMetaContainer implements XbrlDockMetaConsts {

	final Map<String, Object> metaInfo;

	private ArrayList<String> queue = new ArrayList<>();
	Map<String, String> queueNS = new TreeMap<>();
	private Set<String> loaded = new TreeSet<>();

	String currentUrl;
	String currentNS;
	String path;
	Map currentContent;

	Map references = new TreeMap<>();
	Map<String, Map<String, Object>> labels = new TreeMap<>();
	Map<String, Map<String, Object>> contentByURL = new TreeMap<>();
	Map<String, Object> itemsByNS = new TreeMap<>();

	XbrlDockDevCounter cntLinkTypes = new XbrlDockDevCounter("LinkTypeCounts", true);
	XbrlDockDevCounter cntArcRoles = new XbrlDockDevCounter("ArcRoleCounts", true);

	public XbrlDockMetaContainer(Map<String, Object> mi) {
		this.metaInfo = mi;
	}

	public Map getUrlContent(String url) {
		return contentByURL.get(url);
	}

	void setUrlContent(Map content) throws Exception {
		contentByURL.put(currentUrl, content);

		Map<String, Map<String, Object>> items = (Map<String, Map<String, Object>>) content.get(XDC_METATOKEN_items);

		for (Map.Entry<String, Map<String, Object>> ei : items.entrySet()) {
			registerNSItem(ei.getValue().get("name"), ei.getKey());
		}

		Map<String, String> incl = (Map) content.getOrDefault(XDC_METATOKEN_includes, Collections.EMPTY_MAP);

		for (Map.Entry<String, String> ie : incl.entrySet()) {
			optQueue(ie.getKey(), ie.getValue());
		}
	}

	private void registerNSItem(Object name, Object id) {
		itemsByNS.put(currentNS + ":" + name, XbrlDockUtils.sbAppend(null, ":", true, currentUrl, XDC_METATOKEN_items, id).toString());
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

		Map m = XbrlDockUtils.safeGet(contentByURL, realUrl, MAP_CREATOR);
		m = XbrlDockUtils.safeGet(m, XDC_METATOKEN_items, MAP_CREATOR);
		m = XbrlDockUtils.safeGet(m, id, MAP_CREATOR);
		
		m.put(XDC_METATOKEN_url, realUrl);

		return m;
	}

	String getQueuedItem() {
		if (queue.isEmpty()) {
			return null;
		}

		currentUrl = queue.remove(0);
		loaded.add(currentUrl);

		path = XbrlDockUtils.cutPostfix(currentUrl, "/");
		currentNS = queueNS.remove(currentUrl);

		currentContent = XbrlDockUtils.safeGet(contentByURL, currentUrl, MAP_CREATOR);

		return currentUrl;
	}

	String getCurrentUrl() {
		return currentUrl;
	}

	boolean optQueue(String url, String targetNs) {
		url = XbrlDockUtils.optExtendRef(url, path);

		if (null != currentContent) {
			Map incl = XbrlDockUtils.safeGet(currentContent, XDC_METATOKEN_includes, SORTEDMAP_CREATOR);
			incl.put(url, targetNs);
		}
		
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
		ArrayList<Map> allRefs = XbrlDockUtils.safeGet(references, XDC_METATOKEN_references, ARRAY_CREATOR);
//		ArrayList<Map> allRefs = XbrlDockUtils.safeGet(currentContent, XDC_METATOKEN_references, ARRAY_CREATOR);
		allRefs.add(rm);
		return allRefs.size() - 1;
	}

	void setDocumentRef(Map item, Object refIdx) {
//		Map refRefs = XbrlDockUtils.safeGet(currentContent, XDC_METATOKEN_references, SORTEDMAP_CREATOR);
		String itemId = XbrlDockPocUtils.getGlobalItemId(item);
		Map refLinks = XbrlDockUtils.safeGet(references, XDC_METATOKEN_refLinks, SORTEDMAP_CREATOR);

		ArrayList al = XbrlDockUtils.safeGet(refLinks, itemId, ARRAY_CREATOR);
//		String ri = XbrlDockUtils.sbAppend(null, ":", true, currentUrl, XDC_METATOKEN_references, refIdx).toString();

		if (!al.contains(refIdx)) {
			al.add(refIdx);
		}
	}

	void addLinkType(String lt) {
		cntLinkTypes.add(lt);
	}

	void addArcRole(String ar) {
		cntArcRoles.add(ar);
		cntArcRoles.add(" <TOTAL> ");
	}

	void setLabel(Object lang, Object itemId, String labelType, Object value) {
		Map ll = XbrlDockUtils.safeGet(labels, lang, SORTEDMAP_CREATOR);
		XbrlDockUtils.simpleSet(ll, value, itemId, labelType);
	}

	void addLink(Map<String, String> linkInfo) {
		ArrayList<Map<String, String>> links = XbrlDockUtils.safeGet(contentByURL.get(currentUrl), XDC_METATOKEN_links, ARRAY_CREATOR);
		links.add(linkInfo);
	}

	public void save(File fDir) throws Exception {
		metaInfo.remove(XDC_METAINFO_dir);
		ArrayList inc = new ArrayList(loaded);
		
//		String pub = XbrlDockUtils.simpleGet(metaInfo, XDC_METAINFO_pkgInfo, "publisherURL");
//		ArrayList inc = new ArrayList();
//		for ( String i : loaded ) {
//			if ( i.startsWith(pub) ) {
//				inc.add(i);
//			}
//		}
		metaInfo.put(XDC_METATOKEN_includes, inc);
		
		XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYHEAD_FNAME), metaInfo);
		XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYDATA_FNAME), contentByURL);
		XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYREFS_FNAME), references);

		for (Entry<String, Map<String, Object>> le : labels.entrySet()) {
			XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYRES_FNAME_PREFIX + le.getKey() + XDC_FEXT_JSON), le.getValue());

		}
	}

}
