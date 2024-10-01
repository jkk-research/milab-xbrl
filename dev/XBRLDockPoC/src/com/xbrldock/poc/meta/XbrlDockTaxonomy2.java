package com.xbrldock.poc.meta;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevCounter;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsJson;
import com.xbrldock.utils.XbrlDockUtilsNet;
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockTaxonomy2 implements XbrlDockMetaConsts {

	final String id;
	final File fTaxDir;

	Map<String, Map> entryPoints = new TreeMap<>();
	Map<String, Object> items = new TreeMap<>();
	Map<String, Object> labels = new TreeMap<>();
	ArrayList<Map> allRefs = new ArrayList<>();
	Map<String, Set> refRefs = new TreeMap<>();
	ArrayList<Map<String, String>> links = new ArrayList<>();
	
	String lang;
	
	String taxRoot;

	private Set<String> schemas = new TreeSet<>();
	Set<String> linkbases = new TreeSet<String>();
	private Set<String> loaded = new TreeSet<>();

	Set<String> allFiles = new TreeSet<>();
	XbrlDockDevCounter cntLinkTypes = new XbrlDockDevCounter("LinkTypeCounts", true);
	XbrlDockDevCounter cntArcRoles = new XbrlDockDevCounter("ArcRoleCounts", true);

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

	public XbrlDockTaxonomy2(String taxId, File fMetaInf, XbrlDockTaxonomyManager tmgr) throws Exception {
		this.id = taxId;

		fTaxDir = new File(tmgr.taxonomyStoreRoot, taxId);

		File fData = new File(fTaxDir, XDC_TAXONOMY_FNAME);

//		if (fData.isFile()) {
//			Map data = XbrlDockUtilsJson.readJson(fData);
//
//			itemCache = XbrlDockUtils.simpleGet(data, XDC_TAXONOMY_TOKEN_items);
//			links = XbrlDockUtils.simpleGet(data, XDC_TAXONOMY_TOKEN_links);
//			allRefs = XbrlDockUtils.simpleGet(data, XDC_TAXONOMY_TOKEN_references);
//			refRefs = XbrlDockUtils.simpleGet(data, XDC_TAXONOMY_TOKEN_refLinks);
//			itemMapClosed = true;
//		} else 
		{
			loadTaxonomy(fMetaInf);

			Map data = new HashMap();
			
			data.put(XDC_TAXONOMY_TOKEN_items, items);
			data.put(XDC_TAXONOMY_TOKEN_links, links);
			data.put(XDC_TAXONOMY_TOKEN_references, allRefs);
			data.put(XDC_TAXONOMY_TOKEN_refLinks, refRefs);

			XbrlDockUtilsFile.ensureDir(fTaxDir);

//			XbrlDockUtilsJson.writeJson(fData, data);
//
//			for (Map.Entry<String, Object> le : labels.entrySet()) {
//				File fRes = new File(fTaxDir, le.getKey() + RES_FNAME_POSTFIX);
//				XbrlDockUtilsJson.writeJson(fRes, le.getValue());
//			}

		}
	}
	
	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public String toString(String itemId) {
		String ret = (null == lang) ? null : XbrlDockUtils.simpleGet(labels, lang, itemId, "label");
		return (null == ret) ? itemId : ret;
	}

	public Iterable<String> getEntryPoints() {
		return entryPoints.keySet();
	}

	public Iterable<String> getLanguages() {
		return labels.keySet();
	}

	public Iterable<String> getItemIds() {
		return items.keySet();
	}

	public Map<String, Object> getItemLabels(String id, String lang) {
		return XbrlDockUtils.simpleGet(labels, lang, id);
	}

	public Iterable<Map<String, Object>> getItemRefs(String id) {
		Set<Integer> ri = refRefs.get(id);

		if (null == ri) {
			return null;
		} else {
			ArrayList<Map<String, Object>> ret = new ArrayList<>();

			for (Integer i : ri) {
				ret.add(allRefs.get((int) i));
			}

			return ret;
		}
	}

	public Map<String, Object> getItem(String id) {
		return (Map<String, Object>) items.get(id);
	}

	public Iterable<Map<String, String>> getLinks() {
		return links;
	}

	public Map<String, Object> getRes(String lang) throws Exception {
		Map<String, Object> res = XbrlDockUtils.safeGet(labels, lang, new XbrlDockUtils.ItemCreator<Map<String, Object>>() {
			@Override
			public Map<String, Object> create(Object key, Object... hints) {
				File fRes = new File(fTaxDir, key + XDC_RES_FNAME_POSTFIX);
				try {
					return XbrlDockUtilsJson.readJson(fRes);
				} catch (Exception e) {
					return XbrlDockException.wrap(e, "Reading resource", key, "for taxonomy", id, "from file", fRes);
				}
			}
		});

		return res;
	}

	public String optExtendRef(String itemRef, String currPath) throws Exception {
		String realRef = itemRef;

		if (!realRef.contains(XDC_URL_PSEP)) {
			if (realRef.startsWith(XDC_URL_HERE)) {
				realRef = currPath + realRef.substring(1);
			} else if (realRef.startsWith(XDC_URL_UP)) {
				do {
					currPath = XbrlDockUtils.cutPostfix(currPath, "/");
					realRef = realRef.substring(XDC_URL_UP.length());
				} while (realRef.startsWith(XDC_URL_UP));

				realRef = currPath + "/" + realRef;
			} else {
				realRef = currPath + "/" + realRef;
			}
		}

		return realRef;
	}

	public Map getItem(Element e, String currPath) throws Exception {
		String itemRef = e.getAttribute("xlink:href");

		int sp = itemRef.lastIndexOf("#");
		String id = itemRef.substring(sp + 1);
		String realRef = itemRef.substring(0, sp);

		return getItem(realRef, id, currPath);
	}

	public Map getItem(String itemRef, String id, String currPath) throws Exception {
		String realRef = optExtendRef(itemRef, currPath);

		optQueue(realRef);

		Map m = XbrlDockUtils.safeGet(items, id, itemCreator, realRef);

		return m;
	}

	public void loadTaxonomy(File fMetaInf) throws Exception {
//		if (0 == urls.length) {
		if (!fMetaInf.isDirectory()) {
			XbrlDock.log(EventLevel.Error, "Missing META_INF folder", fMetaInf.getCanonicalPath());
			return;
		}

		File p = fMetaInf.getParentFile();
		int pp = p.getCanonicalPath().length() + 1;

		allFiles.clear();
		XbrlDockUtilsFile.processFiles(p, new XbrlDockUtilsFile.FileProcessor() {
			@Override
			public boolean process(File item, ProcessorAction action) throws Exception {
				if (action == ProcessorAction.Process) {
					if (!item.isHidden()) {
						allFiles.add(item.getCanonicalPath().substring(pp));
					}
				}
				return true;
			}
		});

		NodeList nl;
		int nc;

		File fCat = new File(fMetaInf, XDC_FNAME_CATALOG);
		Element eCatalog = XbrlDockUtilsXml.parseDoc(fCat).getDocumentElement();
		
		String base = eCatalog.getAttribute("xml:base");
		File fBase = XbrlDockUtils.isEmpty(base) ? fMetaInf : new File(fMetaInf, base).getCanonicalFile();
		
		Map<String, String> ur = new HashMap<String, String>();

		nl = eCatalog.getElementsByTagName("*");
		nc = nl.getLength();

		for (int idx = 0; idx < nc; ++idx) {
			Element e = (Element) nl.item(idx);
			String tagName = e.getTagName();

			switch (tagName) {
			case "rewriteURI":
				String us = e.getAttribute("uriStartString");
				if ( (null == taxRoot) || (taxRoot.length() > us.length())) {
					taxRoot = us;
				}
				ur.put(us, e.getAttribute("rewritePrefix"));
				break;
			}
		}
		
		XbrlDockUtilsNet.setRewrite(fBase, ur);

		File fTax = new File(fMetaInf, XDC_FNAME_TAXPACK);
		Element eTaxPack = XbrlDockUtilsXml.parseDoc(fTax).getDocumentElement();

		nl = eTaxPack.getElementsByTagName("*");
		nc = nl.getLength();

		for (int idx = 0; idx < nc; ++idx) {
			Element e = (Element) nl.item(idx);
			String tagName = e.getTagName();

			switch (tagName) {
			case "tp:entryPointDocument":
				String epRef = e.getAttribute("href");
				String epId = XbrlDockUtils.getPostfix(epRef, "/"); // epRef.substring(taxRoot.length()) + 1;
				epId = XbrlDockUtils.cutPostfix(epId, ".");
				Map epMap = XbrlDockUtilsXml.readChildNodes((Element) e.getParentNode(), null);
				epMap.put("href", epRef);
				entryPoints.put(epId, epMap);
				break;
			}
		}
//		}

		for (Map epMap : entryPoints.values()) {
			String schemaUrl = (String) epMap.get("href");
			loadSchema(schemaUrl);
		}

		loadQueue();

		XbrlDock.log(EventLevel.Info, "Total item count", items.size(), "Loaded file count", loaded.size(), "not processed", allFiles);

		XbrlDock.log(EventLevel.Info, cntLinkTypes);
		XbrlDock.log(EventLevel.Info, cntArcRoles);
	}

	public void loadSchema(String schemaUrl) throws Exception {

		XbrlDock.log(EventLevel.Trace, "loadSchema", schemaUrl);

		try (InputStream is = XbrlDockUtilsNet.resolveEntityStream(schemaUrl)) {
			Element eSchema = XbrlDockUtilsXml.parseDoc(is).getDocumentElement();

//			String targetNS = eSchema.getAttribute("targetNamespace");
			String path = XbrlDockUtils.cutPostfix(schemaUrl, "/");

			NodeList nl;
			int nc;

			nl = eSchema.getElementsByTagName("*");
			nc = nl.getLength();

			Map em;

			for (int idx = 0; idx < nc; ++idx) {
				Element e = (Element) nl.item(idx);
				String tagName = e.getTagName();

//				String ns = targetNS;
				String sl = null;

				String itemId = e.getAttribute("id");

				switch (tagName) {
				case "xsd:import":
//					ns = e.getAttribute("namespace");
					sl = e.getAttribute("schemaLocation");
					break;
				case "xsd:include":
					sl = e.getAttribute("schemaLocation");
					break;
				case "link:linkbaseRef":
					sl = e.getAttribute("xlink:href");
					break;
				case "xsd:element":
					em = getItem(schemaUrl, itemId, path);
					XbrlDockUtilsXml.readAtts(e, em);
					break;
				case "link:roleType":
					if (schemaUrl.startsWith(taxRoot)) {
						em = getItem(schemaUrl, itemId, path);
						XbrlDockUtilsXml.readAtts(e, em);
						XbrlDockUtilsXml.readChildNodes(e, em);
					}
					break;
				}

				if (null != sl) {
					sl = optExtendRef(sl, path);
					optQueue(sl);
				}
			}
		}

		loaded(schemaUrl, schemas);
	}

	public void loadLinkbase(String url) throws Exception {

		if (url.endsWith("810000.xml")) {
			XbrlDock.log(EventLevel.Debug, url);
		}

		try (InputStream is = XbrlDockUtilsNet.resolveEntityStream(url)) {
			Element eRoot = XbrlDockUtilsXml.parseDoc(is).getDocumentElement();

			String path = XbrlDockUtils.cutPostfix(url, "/");

			NodeList nl;
			int nc;

			nl = eRoot.getElementsByTagName("*");
			nc = nl.getLength();

			Map<String, Object> content = new TreeMap<>();
			List<Map<String, String>> arcs = new ArrayList<>();

			for (int idx = 0; idx < nc; ++idx) {
				Element e = (Element) nl.item(idx);
				String tagName = e.getTagName();

				String tn = XbrlDockUtils.getPostfix(tagName, ":");
				String label = e.getAttribute("xlink:label");

				String lt = e.getAttribute("xlink:type");
				Node role = e.getParentNode().getAttributes().getNamedItem("xlink:role");
				String roleID = (null == role) ? "" : XbrlDockUtils.getPostfix(role.getTextContent(), "/");

				Map em = null;

				switch (lt) {
				case "locator":
					em = getItem(e, path);
					break;
				case "resource":
					switch (tn) {
					case "label":
						em = XbrlDockUtilsXml.readAtts(e, null);
						em.put("value", e.getTextContent());
						break;
					case "reference":
						Map rm = XbrlDockUtilsXml.readChildNodes(e, null);
						content.put(roleID + "_" + label, allRefs.size());
						allRefs.add(rm);

						break;
					}
					break;
				case "arc":
					Map<String, String> am = XbrlDockUtilsXml.readAtts(e, null);
					am.put("xlink:role", roleID);
					arcs.add(am);
					break;
				case "extended":
				case "simple":
				case "":
					// do nothing
					break;
				default:
					XbrlDockException.wrap(null, "Unhandled linktype", lt, url);
					break;
				}

				cntLinkTypes.add(lt + " --- " + tagName);

				if (null != em) {
					content.put(roleID + "_" + label, em);
				}
			}

			for (Map<String, String> am : arcs) {
				String roleID = am.get("xlink:role");

				String ar = am.get("xlink:arcrole");
				ar = XbrlDockUtils.getPostfix(ar, "/");

				cntArcRoles.add(ar);
				cntArcRoles.add(" <TOTAL> ");

				String fromId = am.get("xlink:from");
				Map from = (Map) content.get(roleID + "_" + fromId);
				String toId = am.get("xlink:to");
				Object toVal = content.get(roleID + "_" + toId);
				Map to = (toVal instanceof Map) ? (Map) toVal : null;

				if ((null == from) || (null == toVal)) {
//					XbrlDock.log(EventLevel.Error, "Missing link endpoint", url, am);
					XbrlDockException.wrap(null, "Missing link endpoint", url, am);
				}

				switch (ar) {
				case "concept-label":
				case "element-label":
					String labelType = (String) to.get("xlink:role");
					labelType = XbrlDockUtils.getPostfix(labelType, "/");
					XbrlDockUtils.simpleSet(labels, to.get("value"), to.get("xml:lang"), from.get("id"), labelType);
					break;
				case "concept-reference":
				case "element-reference":
					XbrlDockUtils.safeGet(refRefs, from.get("id"), SET_CREATOR).add(toVal);
					break;
				case "all":
				case "dimension-default":
				case "dimension-domain":
				case "domain-member":
				case "hypercube-dimension":
				case "parent-child":
				case "summation-item":

					if (null == to) {
						XbrlDockException.wrap(null, "Both should be items", url, am);
					}
					String idFrom = (String) from.get("id");
					String idTo = (String) to.get("id");

					am.remove("xlink:type");
					am.put("xlink:arcrole", ar);
					am.put("xlink:from", idFrom);
					am.put("xlink:to", idTo);
					am.put("xbrlDock:url", url.substring(taxRoot.length()));

//					if ("ias_1_2024-03-27_role-810000".equals(roleID)) {
//						if ("ifrs-full_CapitalRequirementsAxis".equals(idFrom)) {
//							XbrlDock.log(EventLevel.Debug, "now");
//						}
//					}

					links.add(am);
//					Map lm = XbrlDockUtils.safeGet(links, idFrom, MAP_CREATOR);
//					ArrayList ls = XbrlDockUtils.safeGet(lm, ar, ARRAY_CREATOR);
//					ls.add(am);

					break;
				default:
					XbrlDockException.wrap(null, "Unhandled arcrole", url, am);
					break;
				}
			}
		}

		loaded(url, linkbases);
	}

	private void loaded(String url, Set<String> target) {
		loaded.add(url);
		target.remove(url);

		if (url.startsWith(taxRoot)) {
			String fn = url.substring(taxRoot.length());

			if (!allFiles.remove(fn)) {
				XbrlDock.log(EventLevel.Warning, "url not listed?", url);
			}
		}
	}

	private boolean optQueue(String schemaUrl) {
		Set<String> target = null;
		if (schemaUrl.endsWith(XDC_FEXT_XML)) {
			target = linkbases;
		} else if (schemaUrl.endsWith(XDC_FEXT_SCHEMA)) {
			target = schemas;
		} else {
			XbrlDock.log(EventLevel.Warning, "Strange extension", schemaUrl);
		}
		return loaded.contains(schemaUrl) ? false : target.add(schemaUrl);
	}

	public void loadQueue() throws Exception {
		while (!schemas.isEmpty()) {
			String next = schemas.iterator().next();
			loadSchema(next);
		}

		itemMapClosed = true;

		while (!linkbases.isEmpty()) {
			String next = linkbases.iterator().next();
			loadLinkbase(next);
		}
	}

}
