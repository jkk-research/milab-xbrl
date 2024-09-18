package com.xbrldock.poc.taxonomy;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockTaxonomy implements XbrlDockTaxonomyConsts {

	private final XbrlDockTaxonomyManager tmgr;

	final String id;
	final File fTaxDir;

	Map<String, Map<String, Object>> itemCache = new TreeMap<>();
	Map<String, Object> labels = new TreeMap<>();
	ArrayList<Map> allRefs = new ArrayList<>();
	Map<String, Set> refRefs = new TreeMap<>();
	Map<String, Object> links = new TreeMap<>();

	Map<String, Object> backLinks = new TreeMap<>();

	String taxRoot;
	private Set<String> schemas = new TreeSet<>();
	Set<String> linkbases = new TreeSet<String>();
	private Set<String> loaded = new TreeSet<>();

	Set<String> allFiles = new TreeSet<>();
	XbrlDockDevCounter cntLinkTypes = new XbrlDockDevCounter("LinkTypeCounts", true);
	XbrlDockDevCounter cntArcRoles = new XbrlDockDevCounter("ArcRoleCounts", true);

	ItemCreator<Map> itemCacheCreator = new ItemCreatorSimple<Map>(TreeMap.class) {
		@Override
		public Map create(Object key, Object... hints) {
			Map ret = super.create(key, hints);
			schemas.add((String) key);
			return ret;
		}
	};

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

	public XbrlDockTaxonomy(String taxId, File fMetaInf, XbrlDockTaxonomyManager tmgr) throws Exception {
		this.tmgr = tmgr;

		this.id = taxId;

		fTaxDir = new File(tmgr.taxomonyStoreRoot, taxId);

		File fData = new File(fTaxDir, TAXONOMY_FNAME);

//		if (fData.isFile()) {
//			Map data = XbrlDockUtilsJson.readJson(fData);
//
//			itemCache = XbrlDockUtils.simpleGet(data, TaxonomyKeys.items);
//			links = XbrlDockUtils.simpleGet(data, TaxonomyKeys.links);
//			allRefs = XbrlDockUtils.simpleGet(data, TaxonomyKeys.references);
//			refRefs = XbrlDockUtils.simpleGet(data, TaxonomyKeys.refLinks);
//
//		} else 
		{
			loadTaxonomy(fMetaInf);

			Map data = new HashMap();

			XbrlDockUtils.simpleSet(data, itemCache, TaxonomyKeys.items);
			XbrlDockUtils.simpleSet(data, links, TaxonomyKeys.links);
			XbrlDockUtils.simpleSet(data, allRefs, TaxonomyKeys.references);
			XbrlDockUtils.simpleSet(data, refRefs, TaxonomyKeys.refLinks);
			
			XbrlDockUtilsFile.ensureDir(fTaxDir);

			XbrlDockUtilsJson.writeJson(fData, data);

			for (Map.Entry<String, Object> le : labels.entrySet()) {
				File fRes = new File(fTaxDir, le.getKey() + RES_FNAME_POSTFIX);
				XbrlDockUtilsJson.writeJson(fRes, le.getValue());
			}
		}
	}

	public Map<String, Object> getRes(String lang) throws Exception {
		Map<String, Object> res = XbrlDockUtils.safeGet(labels, lang, new XbrlDockUtils.ItemCreator<Map<String, Object>>() {
			@Override
			public Map<String, Object> create(Object key, Object... hints) {
				File fRes = new File(fTaxDir, key + RES_FNAME_POSTFIX);
				try {
					return XbrlDockUtilsJson.readJson(fRes);
				} catch (Exception e) {
					return XbrlDockException.wrap(e, "Reading resource", key, "for taxonomy", taxRoot, "from file", fRes);
				}
			}
		});

		return res;
	}

	public String optExtendRef(String itemRef, String currPath) throws Exception {
		String realRef = itemRef;

		if (!realRef.contains(XBRLDOCK_URL_PSEP)) {
			if (realRef.startsWith(XBRLDOCK_URL_HERE)) {
				realRef = currPath + realRef.substring(1);
			} else if (realRef.startsWith(XBRLDOCK_URL_UP)) {
				do {
					currPath = XbrlDockUtils.cutPostfix(currPath, "/");
					realRef = realRef.substring(XBRLDOCK_URL_UP.length());
				} while (realRef.startsWith(XBRLDOCK_URL_UP));

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

		Map m = XbrlDockUtils.safeGet(itemCache, realRef, itemCacheCreator);
		m = XbrlDockUtils.safeGet(m, id, itemCreator, realRef);

		return m;
	}

	public void loadTaxonomy(File fMetaInf, String... urls) throws Exception {
		List<String> u;

		if (0 == urls.length) {
			u = new ArrayList<String>();

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

			File fCat = new File(fMetaInf, XBRLDOCK_FNAME_CATALOG);
			Element eCatalog = XbrlDockUtilsXml.parseDoc(fCat).getDocumentElement();

			nl = eCatalog.getElementsByTagName("*");
			nc = nl.getLength();

			for (int idx = 0; idx < nc; ++idx) {
				Element e = (Element) nl.item(idx);
				String tagName = e.getTagName();

				switch (tagName) {
				case "rewriteURI":
					taxRoot = e.getAttribute("uriStartString");
					break;
				}
			}

			File fTax = new File(fMetaInf, XBRLDOCK_FNAME_TAXPACK);
			Element eTaxPack = XbrlDockUtilsXml.parseDoc(fTax).getDocumentElement();

			nl = eTaxPack.getElementsByTagName("*");
			nc = nl.getLength();

			for (int idx = 0; idx < nc; ++idx) {
				Element e = (Element) nl.item(idx);
				String tagName = e.getTagName();

				switch (tagName) {
				case "tp:entryPointDocument":
					u.add(e.getAttribute("href"));
					break;
				}
			}

		} else {
			u = Arrays.asList(urls);
		}

		for (String schemaUrl : u) {
			loadSchema(schemaUrl);
		}

		loadQueue();

		int allItems = 0;
		for (Map.Entry<String, Map<String, Object>> ie : itemCache.entrySet()) {
			int itemCount = ie.getValue().size();
			allItems += itemCount;
			XbrlDock.log(EventLevel.Info, ie.getKey(), itemCount);
		}

		XbrlDock.log(EventLevel.Info, "Total item count", allItems, "Loaded file count", loaded.size(), "not processed", allFiles);

		XbrlDock.log(EventLevel.Info, cntLinkTypes);
		XbrlDock.log(EventLevel.Info, cntArcRoles);
	}

	public void loadSchema(String schemaUrl) throws Exception {

		XbrlDock.log(EventLevel.Trace, "loadSchema", schemaUrl);

		try (InputStream is = tmgr.resolveEntityStream("", schemaUrl)) {
			Element eSchema = XbrlDockUtilsXml.parseDoc(is).getDocumentElement();

//			String targetNS = eSchema.getAttribute("targetNamespace");
			String path = XbrlDockUtils.cutPostfix(schemaUrl, "/");

			NodeList nl;
			NodeList cl;
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
// 			if ( "ifrs-dim_2024-03-27_role-901000".equals(itemId)) {
//				XbrlDock.log(EventLevel.Debug, "now");
//			}

					if (schemaUrl.startsWith(taxRoot)) {
						em = getItem(schemaUrl, itemId, path);
						XbrlDockUtilsXml.readAtts(e, em);

						cl = e.getChildNodes();
						for (int ii = cl.getLength(); ii-- > 0;) {
							Node cn = cl.item(ii);
							String nodeName = cn.getNodeName();
							String v = (String) em.get(nodeName);

							v = (null == v) ? cn.getNodeValue() : v + ", " + cn.getNodeValue();
							em.put(nodeName, v);
						}
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

		try (InputStream is = tmgr.resolveEntityStream("", url)) {
			Element eRoot = XbrlDockUtilsXml.parseDoc(is).getDocumentElement();

			String path = XbrlDockUtils.cutPostfix(url, "/");

			NodeList nl;
			NodeList cl;
			int nc;

			nl = eRoot.getElementsByTagName("*");
			nc = nl.getLength();

			Map<String, Object> content = new TreeMap<>();
			List<Element> arcs = new ArrayList<>();

			for (int idx = 0; idx < nc; ++idx) {
				Element e = (Element) nl.item(idx);
				String tagName = e.getTagName();

				String tn = XbrlDockUtils.getPostfix(tagName, ":");
				String label = e.getAttribute("xlink:label");

				String lt = e.getAttribute("xlink:type");

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
						Map rm = new TreeMap<String, String>();
						cl = e.getChildNodes();
						for (int ii = cl.getLength(); ii-- > 0;) {
							Node cn = cl.item(ii);
							String txt = cn.getTextContent().trim();
							if (!XbrlDockUtils.isEmpty(txt)) {
								rm.put(cn.getNodeName(), txt);
							}
						}
						content.put(label, allRefs.size());
						allRefs.add(rm);

						break;
					}
					break;
				case "arc":
					arcs.add(e);
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
					content.put(label, em);
				}
			}

			for (Element e : arcs) {
				String ar = e.getAttribute("xlink:arcrole");
				ar = XbrlDockUtils.getPostfix(ar, "/");

				cntArcRoles.add(ar);
				cntArcRoles.add(" <TOTAL> ");

				String fromId = e.getAttribute("xlink:from");
				Map from = (Map) content.get(fromId);
				String toId = e.getAttribute("xlink:to");
				Object toVal = content.get(toId);
				Map to = (toVal instanceof Map) ? (Map) toVal : null;

				if ((null == from) || (null == toVal)) {
					XbrlDock.log(EventLevel.Error, "Missing link endpoint", url, XbrlDockUtilsXml.readAtts(e, null));
//					XbrlDockException.wrap(null, "Missing link endpoint", url, XbrlDockUtilsXml.readAtts(e, null));
				} else {
//					XbrlDock.log(EventLevel.Trace, "Link OK");
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

					Map am = XbrlDockUtilsXml.readAtts(e, null);

					if (null == to) {
						XbrlDockException.wrap(null, "Both should be items", url, am);
					}
					Object idFrom = from.get("id");
					Object idTo = to.get("id");

//					am.put("xlink:from", idFrom);
					
					am.remove("xlink:type");
					am.remove("xlink:arcrole");
					am.remove("xlink:from");
					am.put("xlink:to", idTo);

					Map lm = XbrlDockUtils.safeGet(links, idFrom, MAP_CREATOR);
					ArrayList ls = XbrlDockUtils.safeGet(lm, ar, ARRAY_CREATOR);
					ls.add(am);

					lm = XbrlDockUtils.safeGet(backLinks, idTo, MAP_CREATOR);
					ls = XbrlDockUtils.safeGet(lm, ar, ARRAY_CREATOR);
					ls.add(am);

					break;
				default:
					XbrlDockException.wrap(null, "Unhandled arcrole", url, XbrlDockUtilsXml.readAtts(e, null));
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
		if (schemaUrl.endsWith(XBRLDOCK_EXT_XML)) {
			target = linkbases;
		} else if (schemaUrl.endsWith(XBRLDOCK_EXT_SCHEMA)) {
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
