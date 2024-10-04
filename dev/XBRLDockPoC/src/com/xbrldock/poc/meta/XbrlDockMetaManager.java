package com.xbrldock.poc.meta;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

	XbrlDockDevCounter importIssues = new XbrlDockDevCounter("Import issues", true);

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
		case XDC_CMD_METAMGR_GETMC:
			ret = getMetaContainer((File) params[0], false);
			break;
		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}

		return (RetType) ret;
	}

	private void importTaxonomy(File taxSource) throws Exception {
		File fTempDir = null;

		if (taxSource.isFile() && taxSource.getName().endsWith(XDC_FEXT_ZIP)) {
			fTempDir = new File(dirInput, XbrlDockUtils.strTime());
			XbrlDockUtilsFile.extractWithApacheZipFile(fTempDir, taxSource, null);

			taxSource = fTempDir;
		}

		if (taxSource.isDirectory()) {
			XbrlDockMetaContainer mc = getMetaContainer(taxSource, true);

			mc.optSave(taxonomyStoreRoot);

			XbrlDock.log(EventLevel.Trace, mc.metaInfo);

			XbrlDock.log(EventLevel.Trace, "Schema stats", mc.cntLinkTypes, mc.cntArcRoles);

		}
	}

	public XbrlDockMetaContainer getMetaContainer(File schemaRoot, boolean cache, String... entryPoints) throws Exception {

		Map<String, Object> metaInfo = XbrlDockPocUtils.readMeta(schemaRoot);

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
		} else if ( null != metaEntryPoints ){
			for (String ep : metaEntryPoints) {
				metaContainer.optQueue(ep, null);
			}
		}

		XbrlDock.log(EventLevel.Trace, "getMetaContainer start loading from", schemaRoot.getCanonicalPath());

		File fMIDir = XbrlDockUtils.simpleGet(metaInfo, XDC_METAINFO_dir);
		Map rewrite = XbrlDockUtils.simpleGet(metaInfo, XDC_METAINFO_urlRewrite);
		XbrlDockUtilsNet.setRewrite(fMIDir, rewrite);

		Collection<String> ownedUrls = rewrite.keySet();

		String url;
		while (null != (url = metaContainer.getQueuedItem())) {
			Map cachedContent = null;
			
			String key = XbrlDockUtils.getPostfix(url, XDC_URL_PSEP);

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

					XbrlDockMetaContainer mcData = null;

					for (String s : ownedUrls) {
						if (url.startsWith(s)) {
							mcData = metaContainer;
							break;
						}
					}

					if (null == mcData) {
						
						key = key.split("/")[0];
						mcData = XbrlDockUtils.safeGet(taxonomies, key, new ItemCreator<XbrlDockMetaContainer>() {
							@Override
							public XbrlDockMetaContainer create(Object key, Object... hints) {
								TreeMap<String, Object> mi = new TreeMap<String, Object>();
								XbrlDockUtils.simpleSet(mi, key, XDC_METAINFO_pkgInfo, "identifier");
								return new XbrlDockMetaContainer(mi);
							}
						});

						mcData.setCurrentUrl(url);
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
			for (String s : ownedUrls) {
				String tKey = XbrlDockUtils.getPostfix(s, XDC_URL_PSEP);
				taxonomies.put(tKey, metaContainer);
			}
		}

		for (XbrlDockMetaContainer mc : taxonomies.values()) {
			mc.optSave(taxonomyStoreRoot);
		}

		XbrlDock.log(EventLevel.Error, importIssues);

		return metaContainer;
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
				em = mcData.getItem(itemId, ns);
				XbrlDockUtilsXml.readAtts(e, em);
				break;
			case "roleType":
				em = mcData.getItem(itemId, ns);
				XbrlDockUtilsXml.readAtts(e, em);
				XbrlDockUtilsXml.readChildNodes(e, em);

				break;
			}

			if (null != sl) {
				metaContainer.optQueue(sl, ns);
			}
		}
	}

	private void readLinkbase(Element eDoc, XbrlDockMetaContainer mcData) throws Exception {

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
				em = mcData.getItem(e);
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
					int refIdx = mcData.storeDocumentRef(rm);
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
				XbrlDockException.wrap(null, "Unhandled linktype", lt, mcData.getCurrentUrl());
				break;
			}

			mcData.addLinkType(lt + " --- " + tagName);
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

			mcData.addArcRole(ar);
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
				if (null == labelType) {
					importIssues.add("Missing labelType - " + ar);
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
					importIssues.add("Missing link endpoints - " + ar);
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

				mcData.addLink(am);
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
