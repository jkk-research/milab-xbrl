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
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsJson;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockMetaContainer implements XbrlDockMetaConsts {

	final XbrlDockMetaManager metaManager;
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

	boolean updated;

	XbrlDockDevCounter cntLinkTypes = new XbrlDockDevCounter("LinkTypeCounts", true);
	XbrlDockDevCounter cntArcRoles = new XbrlDockDevCounter("ArcRoleCounts", true);

	public XbrlDockMetaContainer(XbrlDockMetaManager mm, Map<String, Object> mi) {
		metaManager = mm;
		metaInfo = mi;
		updated = false;
	}

	public Map getUrlContent(String url) {
		String key = XbrlDockUtils.getPostfix(url, XDC_URL_PSEP);
		return contentByURL.get(key);
	}

	public Map getHead() {
		return metaInfo;
	}

	void setUrlContent(Map content) throws Exception {
		String key = XbrlDockUtils.getPostfix(currentUrl, XDC_URL_PSEP);
		contentByURL.put(key, content);

		Map<String, Map<String, Object>> items = (Map<String, Map<String, Object>>) content.getOrDefault(XDC_METATOKEN_items, Collections.EMPTY_MAP);
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
		
		realRef = XbrlDockUtils.optCleanUrl(realRef);
		
//		int psp = realRef.indexOf(XDC_URL_PSEP) + XDC_URL_PSEP.length();
//		String rr = realRef.substring(psp);
//		if ( rr.contains("//") ) {
//			realRef = realRef.substring(0, psp) + rr.replaceAll("/+", "/");
//		}

		return getItem(realRef, id, null);
	}

	Map getItem(String id, String callerNamespace) throws Exception {
		return getItem(currentUrl, id, callerNamespace);
	}

	Map getItem(String itemUrl, String id, String callerNamespace) throws Exception {
		String realUrl = XbrlDockUtils.optExtendRef(itemUrl, path);

		optQueue(realUrl, callerNamespace);

		String key = XbrlDockUtils.getPostfix(realUrl, XDC_URL_PSEP);

		Map m = metaManager.getKnownItemForKey(key, id);

		if (null == m) {
			m = XbrlDockUtils.safeGet(contentByURL, key, MAP_CREATOR);
			m = XbrlDockUtils.safeGet(m, XDC_METATOKEN_items, MAP_CREATOR);
			m = XbrlDockUtils.safeGet(m, id, MAP_CREATOR);

			m.put(XDC_METATOKEN_url, realUrl);
			m.put("id", id);
		} else {
//			XbrlDock.log(EventLevel.Trace, "External item resolved", m);
		}

		updated = true;

		return m;
	}

	String getQueuedItem() {
		if (queue.isEmpty()) {
			return null;
		}

		return setCurrentUrl(queue.remove(0));
	}

	String setCurrentUrl(String url) {
		currentUrl = url;
		loaded.add(currentUrl);

		path = XbrlDockUtils.cutPostfix(currentUrl, "/");
		currentNS = queueNS.remove(currentUrl);

		String key = XbrlDockUtils.getPostfix(currentUrl, XDC_URL_PSEP);
		currentContent = XbrlDockUtils.safeGet(contentByURL, key, MAP_CREATOR);

		return currentUrl;
	}

	String getCurrentUrl() {
		return currentUrl;
	}

	boolean optQueue(String url, String targetNs) {
		url = XbrlDockUtils.optExtendRef(url, path);
		
		if ( url.split("//").length > 2 ) {
			XbrlDock.log(EventLevel.Warning, "Sorry?", url);
		}

		if ( url.contains("xsdesef_cor") ) {
			XbrlDock.log(EventLevel.Warning, "Sorry?", url);
		}

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
		allRefs.add(rm);
		return allRefs.size() - 1;
	}

	void setDocumentRef(Map item, Object refIdx) {
		String itemId = XbrlDockPocUtils.getGlobalItemId(item);
		Map refLinks = XbrlDockUtils.safeGet(references, XDC_METATOKEN_refLinks, SORTEDMAP_CREATOR);

		ArrayList al = XbrlDockUtils.safeGet(refLinks, itemId, ARRAY_CREATOR);

		if (!al.contains(refIdx)) {
			al.add(refIdx);
			updated = true;
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
		updated = true;
	}

	void addLink(Map<String, String> linkInfo) {
		ArrayList<Map<String, String>> links = XbrlDockUtils.safeGet(currentContent, XDC_METATOKEN_links, ARRAY_CREATOR);
		links.add(linkInfo);
		updated = true;
	}

	public void optSave(File taxonomyStoreRoot) throws Exception {
		if (updated) {
			String id = XbrlDockUtils.simpleGet(metaInfo, XDC_METAINFO_pkgInfo, "identifier");
			id = XbrlDockUtils.getPostfix(id, XDC_URL_PSEP);
			File fDir = new File(taxonomyStoreRoot, id);
			XbrlDockUtilsFile.ensureDir(fDir);

			XbrlDock.log(EventLevel.Trace, "Saving MetaContainer", id);

			metaInfo.remove(XDC_METAINFO_dir);
			ArrayList inc = new ArrayList(loaded);
			metaInfo.put(XDC_METATOKEN_includes, inc);

			XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYHEAD_FNAME), metaInfo);
			XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYDATA_FNAME), contentByURL);
			XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYREFS_FNAME), references);

			for (Entry<String, Map<String, Object>> le : labels.entrySet()) {
				XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYRES_FNAME_PREFIX + le.getKey() + XDC_FEXT_JSON), le.getValue());
			}

			updated = false;
		}
	}

}
