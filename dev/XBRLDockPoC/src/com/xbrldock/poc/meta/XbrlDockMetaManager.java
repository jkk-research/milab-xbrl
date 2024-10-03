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
import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevCounter;
import com.xbrldock.poc.utils.XbrlDockPocUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsNet;
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockMetaManager implements XbrlDockMetaConsts, XbrlDockConsts.GenAgent {

	File taxonomyStoreRoot;
	File dirInput;

	private final Map<String, XbrlDockMetaContainer> taxonomies = new TreeMap<>();

	XbrlDockDevCounter missingArcRoles = new XbrlDockDevCounter("Missing ArcRoles", true);

	public XbrlDockMetaManager() {
	}

	@Override
	public void initModule(GenApp app, Map config) throws Exception {
		String dataRoot = XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirStore);
		this.taxonomyStoreRoot = new File(dataRoot);
		XbrlDockUtilsFile.ensureDir(taxonomyStoreRoot);

		String inputRoot = XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirInput);
		this.dirInput = new File(inputRoot);
	}

	@Override
	public <RetType> RetType process(String command, Object... params) throws Exception {
		Object ret = null;
		switch (command) {
		case XDC_CMD_METAMGR_IMPORT:
			importTaxonomy((File) params[0]);
			break;
		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}

		return (RetType) ret;
	}

	public Map<String, Object> readMeta(File root) throws Exception {
		Map<String, Object> t = new TreeMap<>();

		XbrlDockUtilsFile.FileProcessor mif = new XbrlDockUtilsFile.FileProcessor() {
			boolean ret = true;

			@Override
			public boolean process(File f, ProcessorAction action) {
				if ((action == ProcessorAction.Begin) && XbrlDockUtils.isEqual(XDC_FNAME_METAINF, f.getName())) {
					t.put(XDC_METAINFO_dir, f);
					ret = false;
				}
				return ret;
			}
		};

		XbrlDockUtilsFile.processFiles(root, mif, null, true, false);

		File fMetaInf = (File) t.get(XDC_METAINFO_dir);

		NodeList nl;
		int nc;

		File fCat = new File(fMetaInf, XDC_FNAME_CATALOG);
		Element eCatalog = XbrlDockUtilsXml.parseDoc(fCat).getDocumentElement();

		String base = eCatalog.getAttribute("xml:base");
		if (XbrlDockUtils.isEmpty(base)) {
			base = "";
		} else if (!base.endsWith("/")) {
			base = base + "/";
		}

		nl = eCatalog.getElementsByTagName("*");
		nc = nl.getLength();

		for (int idx = 0; idx < nc; ++idx) {
			Element e = (Element) nl.item(idx);
			String tagName = e.getTagName();

			switch (tagName) {
			case "rewriteURI":
				String us = e.getAttribute("uriStartString");
				String rp = base + e.getAttribute("rewritePrefix");
				XbrlDockUtils.simpleSet(t, rp, XDC_METAINFO_urlRewrite, us);
				break;
			}
		}

		File fTax = new File(fMetaInf, XDC_FNAME_TAXPACK);
		Element eTaxPack = XbrlDockUtilsXml.parseDoc(fTax).getDocumentElement();

		Set epRefs = new TreeSet();
		Set allRefs = new TreeSet();

		XbrlDockUtilsXml.ChildProcessor procEntryPointItem = new XbrlDockUtilsXml.ChildProcessor() {
			Map epInfo;

			@Override
			public void processChild(String tagName, Element ch) {
				if ( null == epInfo ) {
					epInfo = new HashMap();
				}
				switch (tagName) {
				case "entryPointDocument":
					String epRef = ch.getAttribute("href");
					epRefs.add(epRef);
					allRefs.add(epRef);
					break;
				default:
					XbrlDockUtils.simpleSet(epInfo, ch.getTextContent().replaceAll("\\s+", " ").trim(), tagName);
					break;
				}
			}

			@Override
			public void finish() {
				XbrlDockUtils.safeGet(t, XDC_METAINFO_entryPoints, ARRAY_CREATOR).add(epInfo);

				if (!epRefs.isEmpty()) {
					epInfo.put(XDC_METAINFO_entryPointRefs, new ArrayList(epRefs));
					epRefs.clear();
				}
				epInfo = null;
			}
		};

		XbrlDockUtilsXml.ChildProcessor procEntryPointList = new XbrlDockUtilsXml.ChildProcessor() {
			@Override
			public void processChild(String tagName, Element ch) {
				switch (tagName) {
				case "entryPoint":
					XbrlDockUtilsXml.processChildren(ch, procEntryPointItem);
					break;
				default:
					break;
				}
			}
		};

		XbrlDockUtilsXml.processChildren(eTaxPack, new XbrlDockUtilsXml.ChildProcessor() {
			@Override
			public void processChild(String tagName, Element ch) {
				switch (tagName) {
				case "entryPoints":
					XbrlDockUtilsXml.processChildren(ch, procEntryPointList);
					break;
				default:
					XbrlDockUtils.simpleSet(t, ch.getTextContent().replaceAll("\\s+", " ").trim(), XDC_METAINFO_pkgInfo, tagName);
					break;
				}
			}

			@Override
			public void finish() {
				if (!allRefs.isEmpty()) {
					t.put(XDC_METAINFO_entryPointRefs, new ArrayList(allRefs));
				}
			}
		});

		return t;
	}

	private void importTaxonomy(File taxSource) throws Exception {
		File fTempDir = null;

		if (taxSource.isFile() && taxSource.getName().endsWith(XDC_FEXT_ZIP)) {
			fTempDir = new File(dirInput, XbrlDockUtils.strTime());
			XbrlDockUtilsFile.extractWithApacheZipFile(fTempDir, taxSource, null);

			taxSource = fTempDir;
		}

		if (taxSource.isDirectory()) {
			XbrlDockMetaContainer mc = getMetaContainer(taxSource);

			Map txmyInfo = mc.metaInfo;
//			Map txmyInfo = XbrlDockPocUtils.readMeta(taxSource, null);

			String id = XbrlDockUtils.simpleGet(txmyInfo, XDC_METAINFO_pkgInfo, "identifier");
			id = XbrlDockUtils.getPostfix(id, XDC_URL_PSEP);
			File fDir = new File(taxonomyStoreRoot, id);
			XbrlDockUtilsFile.ensureDir(fDir);
			
			mc.save(fDir);
			
			XbrlDock.log(EventLevel.Trace, txmyInfo);

			XbrlDock.log(EventLevel.Trace, "Schema stats", mc.cntLinkTypes, mc.cntArcRoles);

		}
	}

	public XbrlDockMetaContainer getMetaContainer(File schemaRoot, String... entryPoints) throws Exception {

		Map<String, Object> metaInfo = readMeta(schemaRoot);

		XbrlDockMetaContainer metaContainer = new XbrlDockMetaContainer(metaInfo);

		ArrayList<String> metaEntryPoints = (ArrayList<String>) metaInfo.get(XDC_METAINFO_entryPointRefs);

		if (entryPoints.length > 0) {
			for (String ep : entryPoints) {
				if (metaEntryPoints.contains(ep)) {
					metaContainer.optQueue(ep, null);
				} else {
					XbrlDock.log(EventLevel.Error, "getMetaContainer - invalid entry point", ep);
				}
			}
		} else {
			for (String ep : metaEntryPoints) {
				metaContainer.optQueue(ep, null);
			}
		}

		XbrlDock.log(EventLevel.Trace, "getMetaContainer start loading from", schemaRoot.getCanonicalPath());

		File fMIDir = XbrlDockUtils.simpleGet(metaInfo, XDC_METAINFO_dir);
		Map rewrite = XbrlDockUtils.simpleGet(metaInfo, XDC_METAINFO_urlRewrite);
		XbrlDockUtilsNet.setRewrite(fMIDir, rewrite);

		String url;
		while (null != (url = metaContainer.getQueuedItem())) {
			Map cachedContent = null;

			for (XbrlDockMetaContainer tm : taxonomies.values()) {
				cachedContent = tm.getUrlContent(url);
				if (null != cachedContent) {
					break;
				}
			}

			if (null == cachedContent) {
				try (InputStream is = XbrlDockUtilsNet.resolveEntityStream(url)) {
					Element eDoc = XbrlDockUtilsXml.parseDoc(is).getDocumentElement();

					XbrlDock.log(EventLevel.Trace, "loading", url);

					if (url.endsWith(XDC_FEXT_SCHEMA)) {
						readSchema(eDoc, metaContainer);
					} else {
						readLinkbase(eDoc, metaContainer);
					}
				}
			} else {
				metaContainer.setUrlContent(cachedContent);
			}
		}

		XbrlDock.log(EventLevel.Error, "Unhandled arcroles", missingArcRoles);

		return metaContainer;
	}

	private void readSchema(Element eDoc, XbrlDockMetaContainer metaContainer) throws Exception {

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
				em = metaContainer.getItem(itemId, ns);
				XbrlDockUtilsXml.readAtts(e, em);
				break;
			case "roleType":
//					if (schemaUrl.startsWith(taxRoot)) 
			{
				em = metaContainer.getItem(itemId, ns);
				XbrlDockUtilsXml.readAtts(e, em);
				XbrlDockUtilsXml.readChildNodes(e, em);
			}
				break;
			}

			if (null != sl) {
				metaContainer.optQueue(sl, ns);
			}
		}
	}

	private void readLinkbase(Element eDoc, XbrlDockMetaContainer metaContainer) throws Exception {

		NodeList nl;
		int nc;

		nl = eDoc.getElementsByTagName("*");
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
				em = metaContainer.getItem(e);
				break;
			case "resource":
				switch (tn) {
				case "label":
				case "message":
				case "valueAssertion":
					em = XbrlDockUtilsXml.readAtts(e, null);
					em.put("value", e.getTextContent());
					break;
				case "reference":
					Map rm = XbrlDockUtilsXml.readChildNodes(e, null);
					int refIdx = metaContainer.storeDocumentRef(rm);
					content.put(roleID + "_" + label, refIdx);
//						content.put(roleID + "_" + label,  allRefs.size());
//						allRefs.add(rm);

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
				XbrlDockException.wrap(null, "Unhandled linktype", lt, metaContainer.getCurrentUrl());
				break;
			}

			metaContainer.addLinkType(lt + " --- " + tagName);
//				cntLinkTypes.add(lt + " --- " + tagName);

			if (null != em) {
				content.put(roleID + "_" + label, em);
			}
		}

//		String url = metaContainer.getCurrentUrl();

		for (Map<String, String> am : arcs) {
			String roleID = am.get("xlink:role");

			String ar = am.get("xlink:arcrole");
			ar = XbrlDockUtils.getPostfix(ar, "/");

			metaContainer.addArcRole(ar);
//				cntArcRoles.add(ar);
//				cntArcRoles.add(" <TOTAL> ");

			String fromId = am.get("xlink:from");
			Map from = (Map) content.get(roleID + "_" + fromId);
			String toId = am.get("xlink:to");
			Object toVal = content.get(roleID + "_" + toId);
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
				labelType = XbrlDockUtils.getPostfix(labelType, "/");
				metaContainer.setLabel(to.get("xml:lang"), idFrom, labelType, to.get("value"));
//					XbrlDockUtils.simpleSet(labels, to.get("value"), to.get("xml:lang"), from.get("id"), labelType);
				break;
			case "concept-reference":
			case "element-reference":
				if (null == idFrom) {
					missingArcRoles.add(ar);
				} else {
					metaContainer.setDocumentRef(from, toVal);
				}
//					XbrlDockUtils.safeGet(refRefs, from.get("id"), SET_CREATOR).add(toVal);
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
					missingArcRoles.add(ar);
					break;
//					XbrlDockException.wrap(null, "Both should be items", url, am);
				}
				String idTo = XbrlDockPocUtils.getGlobalItemId(to);

				am.remove("xlink:type");
				am.put("xlink:arcrole", ar);
				am.put("xlink:from", idFrom);
				am.put("xlink:to", idTo);
//				am.put("xbrlDock:url", url);
//					am.put("xbrlDock:url", url.substring(taxRoot.length()));

//					if ("ias_1_2024-03-27_role-810000".equals(roleID)) {
//						if ("ifrs-full_CapitalRequirementsAxis".equals(idFrom)) {
//							XbrlDock.log(EventLevel.Debug, "now");
//						}
//					}

				metaContainer.addLink(am);
//					links.add(am);

//					Map lm = XbrlDockUtils.safeGet(links, idFrom, MAP_CREATOR);
//					ArrayList ls = XbrlDockUtils.safeGet(lm, ar, ARRAY_CREATOR);
//					ls.add(am);

				break;
//			default:
////				XbrlDockException.wrap(null, "Unhandled arcrole", url, am);
////				XbrlDock.log(EventLevel.Error, "Unhandled arcrole", url, am);
//				missingArcRoles.add(ar);
//				break;
			}
		}
	}

//		loaded(url, linkbases);
//	}

}
