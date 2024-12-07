package com.xbrldock.poc.meta;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevCounter;
import com.xbrldock.poc.utils.XbrlDockPocUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsJson;
import com.xbrldock.utils.XbrlDockUtilsNet;
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockMetaManager implements XbrlDockMetaConsts, XbrlDockConsts.GenAgent {

	File metaStoreRoot;
	File dirInput;

	private final Map<String, Map> metaCatalog = new TreeMap<>();
	private final Map<String, XbrlDockMetaContainer> mcById = new TreeMap<>();
	private final Map<String, XbrlDockMetaContainer> mcByUrl = new TreeMap<>();

	XbrlDockDevCounter importIssues = new XbrlDockDevCounter("Import issues", true);

	public static boolean LOAD_CACHE = false;

	ArrayList<String> cacheQueue = new ArrayList<>();
	ItemCreator<XbrlDockMetaContainer> cacheLoader = new ItemCreator<XbrlDockMetaContainer>() {
		@Override
		public XbrlDockMetaContainer create(Object key, Object... hints) {
			String s = XbrlDockUtils.getPostfix((String) key, XDC_URL_PSEP);
			Map<String, Object> mi = XbrlDockUtils.simpleGet(metaCatalog, s);

			XbrlDockMetaContainer mc = (null == mi) ? new XbrlDockMetaContainer(XbrlDockMetaManager.this, (String) key)
					: new XbrlDockMetaContainer(XbrlDockMetaManager.this, mi);

			if (LOAD_CACHE) {
				try {
					mc.load();
					for (String prefix : mc.ownedUrls) {
						mcByUrl.put(prefix, mc);
					}
					for (String r : (Collection<String>) mi.getOrDefault(XDC_GEN_TOKEN_requires, Collections.EMPTY_LIST)) {
						if (!mcById.keySet().contains(r) && !cacheQueue.contains(r)) {
							cacheQueue.add(r);
						}
					}
				} catch (Exception e) {
					return XbrlDockException.wrap(e, "Cache load failed", key, mi);
				}
			}

			return mc;
		}
	};

	XbrlDockMetaContainer loadFromCache(String id) {
		XbrlDockMetaContainer ret = mcById.get(id);
		if (null != ret) {
			return ret;
		}

		Map<String, XbrlDockMetaContainer> loaded = new TreeMap<>();
		cacheQueue.add(id);

		while (!cacheQueue.isEmpty()) {
			String mcid = cacheQueue.get(0);
			loaded.put(mcid, XbrlDockUtils.safeGet(mcById, mcid, cacheLoader));
			cacheQueue.remove(0);
		}

		for (XbrlDockMetaContainer mc : loaded.values()) {
			for (String r : (Collection<String>) mc.metaInfo.getOrDefault(XDC_GEN_TOKEN_requires, Collections.EMPTY_LIST)) {
				mc.requires.add(loaded.get(r));
			}
		}

		return loaded.get(id);
	}

	public XbrlDockMetaManager() {
	}

	private void initModule(Map config) throws Exception {
		String dataRoot = XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirStore);
		this.metaStoreRoot = new File(dataRoot);
		XbrlDockUtilsFile.ensureDir(metaStoreRoot);

		String inputRoot = XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirInput);
		this.dirInput = new File(inputRoot);

		File fc = new File(metaStoreRoot, XDC_FNAME_METACATALOG);
		if (fc.isFile()) {
			Map mc = XbrlDockUtilsJson.readJson(fc);
			metaCatalog.putAll(mc);
		}
	}

	@Override
	public Object process(String command, Object... params) throws Exception {
		Object ret = null;
//		XbrlDockMetaContainer mc;

		switch (command) {
		case XDC_CMD_GEN_Init:
			initModule((Map) params[0]);
			break;
		case XDC_CMD_GEN_GETCATALOG:
			ret = metaCatalog;
			break;
		case XDC_CMD_METAMGR_IMPORT:
			importTaxonomy((File) params[0]);
			break;
		case XDC_CMD_METAMGR_GETMC:
			ret = loadFromCache((String) params[0]);
			break;
		case XDC_CMD_METAMGR_LOADMC:
			ret = buildMetaContainer((File) params[0], false);
			break;
		case XDC_CMD_GEN_TEST01:
			File f = new File(dirInput, "esef_taxonomy_2022_v1.1");
			ret = importTaxonomy(f);
			break;
		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}

		return ret;
	}

	private XbrlDockMetaContainer importTaxonomy(File taxSource) throws Exception {
		XbrlDockMetaContainer mc = null;

		String fName = taxSource.getName();

		if (!taxSource.exists()) {
			File fTaxZip = new File(taxSource.getParentFile(), fName + XDC_FEXT_ZIP);
			if (fTaxZip.isFile()) {
				XbrlDockUtilsFile.extractWithApacheZipFile(taxSource, fTaxZip, null);
			}
		}

		if (taxSource.isDirectory()) {
			importIssues.reset();

			mc = buildMetaContainer(taxSource, true);

			saveChanges();

//			mc.optSave();

			XbrlDock.log(EventLevel.Trace, mc.metaInfo);

//			XbrlDock.log(EventLevel.Trace, "Schema stats", mc.cntLinkTypes, mc.cntArcRoles);

		}

		return mc;
	}

	public XbrlDockMetaContainer buildMetaContainer(File schemaRoot, boolean cache, String... entryPoints) throws Exception {
		XbrlDock.log(EventLevel.Context, "Loading MetaContainer from", schemaRoot.getPath());

		Map<String, Object> metaInfo = XbrlDockPocUtils.readMeta(schemaRoot);

		XbrlDockMetaContainer metaContainer = new XbrlDockMetaContainer(this, metaInfo);

		ArrayList<String> metaEntryPoints = (ArrayList<String>) metaInfo.get(XDC_METAINFO_entryPointRefs);

		if (entryPoints.length > 0) {
			for (String ep : entryPoints) {
				if (metaEntryPoints.contains(ep)) {
					metaContainer.optQueue(ep, null);
				} else {
					XbrlDock.log(EventLevel.Error, "getMetaContainer - invalid entry point", ep);
				}
			}
		} else if (null != metaEntryPoints) {
			for (String ep : metaEntryPoints) {
				metaContainer.optQueue(ep, null);
			}
		}

		File fMIDir = XbrlDockUtils.simpleGet(metaInfo, XDC_METAINFO_dir);
		Map rewrite = XbrlDockUtils.simpleGet(metaInfo, XDC_METAINFO_urlRewrite);
		XbrlDockUtilsNet.setRewrite(fMIDir, rewrite);

		String url;
		while (null != (url = metaContainer.getQueuedItem())) {
			String key = XbrlDockUtils.getPostfix(url, XDC_URL_PSEP);
			Map cachedContent = getKnownContentForKey(key, metaContainer);

			if (null == cachedContent) {
				try (InputStream is = XbrlDockUtilsNet.resolveEntityStream(url)) {
					Element eDoc = XbrlDockUtilsXml.parseDoc(is).getDocumentElement();

					XbrlDock.log(EventLevel.Trace, "loading ", url);

					XbrlDockMetaContainer mcData = null;

					for (String s : metaContainer.ownedUrls) {
						if (key.startsWith(s)) {
							mcData = metaContainer;
							break;
						}
					}

					if (null == mcData) {
						key = key.split("/")[0];
//						mcData = XbrlDockUtils.safeGet(mcByUrl, key, cacheLoader);
						mcData = loadFromCache(key);
						mcData.setCurrentUrl(url);

						metaContainer.requires.add(mcData);
					}

					if (url.endsWith(XDC_FEXT_SCHEMA)) {
						readSchema(eDoc, metaContainer, mcData);
					} else {
						readLinkbase(eDoc, mcData);
					}
				}
			} else {
				if (!cache) {
					metaContainer.setUrlContent(cachedContent);
				}
			}
		}

		if (cache) {
			Set<String> alienKeys = new TreeSet<>(metaContainer.contentByURL.keySet());

			for (String prefix : metaContainer.ownedUrls) {
				for (Iterator<String> iak = alienKeys.iterator(); iak.hasNext();) {
					String ak = iak.next();
					if (ak.startsWith(prefix)) {
						iak.remove();
					}
				}
				mcByUrl.put(prefix, metaContainer);
			}

			for (String ak : alienKeys) {
				metaContainer.contentByURL.remove(ak);
			}

//			alienKeys = new TreeSet<>(metaContainer.fileLinks.keySet());
//
//			for (String prefix : metaContainer.ownedUrls) {
//				for (Iterator<String> iak = alienKeys.iterator(); iak.hasNext();) {
//					String ak = iak.next();
//					if (ak.startsWith(prefix)) {
//						iak.remove();
//					}
//				}
//			}
//
//			for (String ak : alienKeys) {
//				metaContainer.fileLinks.remove(ak);
//			}

			mcById.put(metaContainer.getId(), metaContainer);
		}

		saveChanges();

		if (!importIssues.isEmpty()) {
			XbrlDock.log(EventLevel.Error, importIssues);
		}

		return metaContainer;
	}

	private void saveChanges() throws Exception {
		boolean updateCatalog = false;

		for (Map.Entry<String, XbrlDockMetaContainer> mce : mcById.entrySet()) {
			XbrlDockMetaContainer mc = mce.getValue();
			if (mc.optSave()) {
				updateCatalog = true;
				metaCatalog.put(mce.getKey(), mc.metaInfo);
			}
		}

		if (updateCatalog) {
			XbrlDockUtilsJson.writeJson(new File(metaStoreRoot, XDC_FNAME_METACATALOG), metaCatalog);
		}
	}

	public Map getKnownItemForKey(String key, String id, XbrlDockMetaContainer metaContainer) {
		Map ret = null;

		Map cachedContent = getKnownContentForKey(key, metaContainer);

		if (null != cachedContent) {
			ret = null;
			ret = XbrlDockUtils.simpleGet(cachedContent, XDC_METATOKEN_items, id);
		}

		return ret;
	}

	private Map getKnownContentForKey(String key, XbrlDockMetaContainer metaContainer) {
		Map cachedContent = null;

		for (XbrlDockMetaContainer tm : mcByUrl.values()) {
			cachedContent = tm.getUrlContent(key);
			if (null != cachedContent) {
				metaContainer.requires.add(tm);
				break;
			}
		}

		return cachedContent;
	}

	private void readSchema(Element eDoc, XbrlDockMetaContainer metaContainer, XbrlDockMetaContainer mcData) throws Exception {

		String targetNS = eDoc.getAttribute("targetNamespace");

		NodeList nl;
		int nc;

		nl = eDoc.getElementsByTagName("*");
		nc = nl.getLength();

		Map em;

		for (int idx = 0; idx < nc; ++idx) {
			Element e = (Element) nl.item(idx);
			String tagName = e.getTagName();
			String tn = XbrlDockUtils.getPostfix(tagName, ":");

			String ns = targetNS;
			String sl = null;

			String itemId = e.getAttribute("id");

			switch (tn) {
			case "import":
				ns = e.getAttribute("namespace");
				sl = e.getAttribute("schemaLocation");
				break;
			case "include":
				sl = e.getAttribute("schemaLocation");
				break;
			case "linkbaseRef":
				sl = e.getAttribute("xlink:href");
				break;
			case "element":
				if (XbrlDockUtils.isEmpty(itemId)) {
					itemId = e.getAttribute("name");
				}
				if (!XbrlDockUtils.isEmpty(itemId)) {
					em = mcData.getItem(itemId, ns);
					XbrlDockUtilsXml.readAtts(e, null, em);
				}
				break;
			case "roleType":
				em = mcData.getItem(itemId, ns);
				XbrlDockUtilsXml.readAtts(e, null, em);
				XbrlDockUtilsXml.readChildNodes(e, em);

				break;
			}

			if (null != sl) {
				sl = XbrlDockUtils.optCleanUrl(sl);
				metaContainer.optQueue(sl, ns);
			}
		}
	}

	private Map addFormulaOb(XbrlDockMetaContainer mcData, String type, Element e) throws Exception {
		Map formula = XbrlDockUtils.safeGet(mcData.metaInfo, XDC_METATOKEN_formula, SORTEDMAP_CREATOR);
		ArrayList fa = XbrlDockUtils.safeGet(formula, type, ARRAY_CREATOR);
		Map fOb = new TreeMap();
		fOb.put(XDC_EXT_TOKEN_id, e.getAttribute("id"));
		
		fa.add(fOb);

		return fOb;
	}

	private void readLinkbase(Element eDoc, XbrlDockMetaContainer mcData) throws Exception {

		NodeList nl;
		int nc;

		nl = eDoc.getElementsByTagName("*");
		nc = nl.getLength();

		Map<String, Object> content = new TreeMap<>();
		List<Map<String, String>> arcs = new ArrayList<>();

		String url = mcData.getCurrentUrl();
		
		Map formOb = null;

		for (int idx = 0; idx < nc; ++idx) {
			Element e = (Element) nl.item(idx);
			String tagName = e.getTagName();

			String tn = XbrlDockUtils.getPostfix(tagName, ":");
			String label = e.getAttribute("xlink:label");

			String lt = e.getAttribute("xlink:type");
			Element eParent = (Element) e.getParentNode();
			String roleID = XbrlDockUtils.getPostfix(eParent.getAttribute("xlink:role"), XDC_URL_PSEP);

			Map em = null;

			boolean storeInContent = !XbrlDockUtils.isEmpty(label);

			switch (lt) {
			case "locator":
//				String r = e.getAttribute("xlink:href");
//				if ( r.contains("severities")) {
//					XbrlDock.log(EventLevel.Debug, "hopp", r);
//				}
				em = mcData.getItem(e);
				break;
			case "resource":
				switch (tn) {
				case "label":
				case "message":
					break;
				case "reference":
					Map rm = XbrlDockUtilsXml.readChildNodes(e, null);
					int refIdx = mcData.storeDocumentRef(rm);
					content.put(roleID + XDC_SEP_ID + label, refIdx);
					storeInContent = false;
					break;
				case "valueAssertion":
					
					Map fOb = addFormulaOb(mcData, XDC_FORMULA_assertions, e);
					fOb.put(XDC_FORMULA_condition, e.getAttribute("test"));
					
					break;
				case "formula":
					formOb = addFormulaOb(mcData, XDC_FORMULA_expressions, e);
					formOb.put(XDC_FACT_TOKEN_concept, XbrlDockUtilsXml.getInfo(e, "formula", "qname"));
					formOb.put(XDC_FORMULA_formula, e.getAttribute("value"));

//					XbrlDock.log(EventLevel.Trace, "Calculation", e.getAttribute("id"), XbrlDockUtilsXml.getInfo(e, "formula", "qname"), e.getAttribute("value"));
					break;
				case "precondition":
					formOb.put(XDC_FORMULA_condition, e.getAttribute("test"));
					formOb = null;
//					XbrlDock.log(EventLevel.Trace, "Precondition", e.getAttribute("test"));
					break;
				default:
					String iid = e.getAttribute("id");
					if (XbrlDockUtils.isEmpty(iid)) {
						iid = roleID + XDC_SEP_ID + label;
					}
					em = mcData.getItem(url, iid, null);
					em.put("id", iid);
					em.put(XDC_METATOKEN_tagName, tn);
					XbrlDockUtilsXml.readAtts(e, "value", em);
					break;
				}
				break;
			case "arc":
				Map<String, String> am = XbrlDockUtilsXml.readAtts(e, null, null);
				am.put("xlink:role", roleID);
				am.put(XDC_EXT_TOKEN_type, XbrlDockUtils.getPostfix(eParent.getTagName(), ":"));
				arcs.add(am);
				storeInContent = false;
				break;
			case "extended":
			case "simple":
			case "":
				storeInContent = false;
				break;
			default:
				XbrlDockException.wrap(null, "Unhandled linktype", lt, mcData.getCurrentUrl());
				break;
			}

			mcData.addLinkType(lt + " --- " + tagName);

			if (storeInContent) {
				if (null == em) {
					em = XbrlDockUtilsXml.readAtts(e, "value", null);
					em.put(XDC_METATOKEN_url, url + "#" + em.get("id"));
				}
				content.put(roleID + XDC_SEP_ID + label, em);
			}
		}

		for (Map<String, String> am : arcs) {
			String roleID = am.get("xlink:role");

			String ar = am.get("xlink:arcrole");
			ar = XbrlDockUtils.getPostfix(ar, "/");

			mcData.addArcRole(ar);

			String fromId = am.get("xlink:from");
			Map from = (Map) content.get(roleID + XDC_SEP_ID + fromId);
			String toId = am.get("xlink:to");
			Object toVal = content.get(roleID + XDC_SEP_ID + toId);
			Map to = (toVal instanceof Map) ? (Map) toVal : null;

			if ((null == from) || (null == toVal)) {
//					XbrlDock.log(EventLevel.Error, "Missing link endpoint", url, am);
//				XbrlDockException.wrap(null, "Missing link endpoint", url, am);
			}

			String idFrom = XbrlDockPocUtils.getGlobalItemId(from);

			switch (ar) {
			case "concept-label":
			case "element-label":
			case "assertion-unsatisfied-message":
				String labelType = (String) to.get("xlink:role");
				if (null == labelType) {
					if (XbrlDockUtils.isEqual("prohibited", am.get("use"))) {
						// that's OK
					} else {
						importIssues.add("Missing labelType -  " + ar);
					}
				} else {
					labelType = XbrlDockUtils.getPostfix(labelType, "/");
					mcData.setLabel(to.get("xml:lang"), idFrom, labelType, to.get("value"));
				}
				break;
			case "concept-reference":
			case "element-reference":
				if (null == idFrom) {
					importIssues.add("Missing ref source - " + ar);
				} else {
					mcData.setDocumentRef(from, toVal);
				}
				break;
			case "all":
			case "dimension-default":
			case "dimension-domain":
			case "domain-member":
			case "hypercube-dimension":
			case "parent-child":
			case "summation-item":
			default:

				if ((null == to) || (null == from)) {
					importIssues.add("Missing link endpoints -  " + ar);
					break;
//					XbrlDockException.wrap(null, "Both should be items", url, am);
				}
				String idTo = XbrlDockPocUtils.getGlobalItemId(to);

				if (idFrom.contains("#null#")) {
					importIssues.add("Missing link endpoints -  " + ar);
					break;
//					XbrlDockException.wrap(null, "Both should be items", url, am);
				}

				am.remove("xlink:type");
				am.put("xlink:arcrole", ar);
				am.put("xlink:from", idFrom);
				am.put("xlink:to", idTo);

				mcData.addLink(am);

				break;
//			default:
////				XbrlDockException.wrap(null, "Unhandled arcrole", url, am);
////				XbrlDock.log(EventLevel.Error, "Unhandled arcrole", url, am);
//				missingArcRoles.add(ar);
//				break;
			}
		}
	}
}
