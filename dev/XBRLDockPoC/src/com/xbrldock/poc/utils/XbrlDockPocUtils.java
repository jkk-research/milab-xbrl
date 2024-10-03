package com.xbrldock.poc.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockPocUtils extends XbrlDockUtils implements XbrlDockPocConsts {
	
	public static String getGlobalItemId(Map item) {
		return (null == item) ? null : XbrlDockUtils.sbAppend(null, "#", true, item.get(XDC_METATOKEN_url), item.get("id")).toString();
	}


	public static Map<String, Object> readMeta(File root, Map<String, Object> target) throws Exception {
		Map<String, Object> t = ensureMap(target, true);

		XbrlDockUtilsFile.FileProcessor mif = new XbrlDockUtilsFile.FileProcessor() {
			@Override
			public boolean process(File f, ProcessorAction action) {
				if ((action == ProcessorAction.Begin) && isEqual(XDC_FNAME_METAINF, f.getName())) {
					t.put("", f);
					return false;
				}
				return true;
			}
		};

		XbrlDockUtilsFile.processFiles(root, mif, null, true, false);

		File fMetaInf = (File) t.remove("");

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
				simpleSet(t, rp, XDC_METAINFO_urlRewrite, us);
				break;
			}
		}

		File fTax = new File(fMetaInf, XDC_FNAME_TAXPACK);
		Element eTaxPack = XbrlDockUtilsXml.parseDoc(fTax).getDocumentElement();

		Set allRefs = new TreeSet();

		XbrlDockUtilsXml.processChildren(eTaxPack, new XbrlDockUtilsXml.ChildProcessor() {
			@Override
			public void processChild(String tagName, Element ch) {
				switch (tagName) {
				case "entryPoints":
					XbrlDockUtilsXml.processChildren(ch, new XbrlDockUtilsXml.ChildProcessor() {
						@Override
						public void processChild(String tagName, Element ch) {
							switch (tagName) {
							case "entryPoint":
								Map epInfo = new HashMap();
								Set epRefs = new TreeSet();
								XbrlDockUtils.safeGet(t, XDC_METAINFO_entryPoints, ARRAY_CREATOR).add(epInfo);

								XbrlDockUtilsXml.processChildren(ch, new XbrlDockUtilsXml.ChildProcessor() {
									@Override
									public void processChild(String tagName, Element ch) {
										switch (tagName) {
										case "entryPointDocument":
											String epRef = ch.getAttribute("href");
											epRefs.add(epRef);
											allRefs.add(epRef);
//											XbrlDockUtils.safeGet(t, XDC_METAINFO_entryPointRefs, SET_CREATOR).add(epRef);
//											XbrlDockUtils.safeGet(epInfo, XDC_METAINFO_entryPointRefs, SET_CREATOR).add(epRef);
											break;
										default:
											simpleSet(epInfo, ch.getTextContent().replaceAll("\\s+", " ").trim(), tagName);
											break;
										}
									}
								});

								if (!epRefs.isEmpty()) {
									epInfo.put(XDC_METAINFO_entryPointRefs, new ArrayList(epRefs));
								}
								break;
							default:
								break;
							}
						}
					});
					break;
				default:
					simpleSet(t, ch.getTextContent().replaceAll("\\s+", " ").trim(), XDC_METAINFO_pkgInfo, tagName);
					break;
				}
			}
		});
		
		if (!allRefs.isEmpty()) {
			t.put(XDC_METAINFO_entryPointRefs, new ArrayList(allRefs));
		}


		return t;
	}

}
