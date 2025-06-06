package com.xbrldock.poc.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.stream.XbrlDockStreamXml;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockPocUtils extends XbrlDockUtils implements XbrlDockPocConsts {

	public static String getGlobalItemId(Map item) {
		return (null == item) ? null
				: XbrlDockUtils.sbAppend(null, "#", true, XbrlDockUtils.getPostfix((String) item.get(XDC_METATOKEN_url), XDC_URL_PSEP), item.get("id")).toString();
	}

	public static Map<String, Object> readMeta(File root) throws Exception {
		Map<String, Object> t = new TreeMap<>();

		GenAgent mif = new GenAgent() {
			boolean ret = true;

			@Override
			public Object process(String cmd, Map<String, Object> params) throws Exception {
				File f = (File) params.get(XDC_GEN_TOKEN_target);
				if (XDC_CMD_GEN_Begin.equals(cmd) && XDC_FNAME_METAINF.equals(f.getName())) {
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

		File fCat = new File(fMetaInf, XDC_FNAME_FILINGCATALOG);
		Element eCatalog = XbrlDockStreamXml.parseDoc(fCat).getDocumentElement();

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

		File fTax = new File(fMetaInf, XDC_FNAME_FILINGTAXPACK);
		Element eTaxPack = XbrlDockStreamXml.parseDoc(fTax).getDocumentElement();

		Set epRefs = new TreeSet();
		Set allRefs = new TreeSet();

		XbrlDockStreamXml.ChildProcessor procEntryPointItem = new XbrlDockStreamXml.ChildProcessor() {
			Map epInfo;

			@Override
			public void processChild(String tagName, Element ch) {
				if (null == epInfo) {
					epInfo = new HashMap();
				}
				switch (tagName) {
				case "entryPointDocument":
					String epRef = ch.getAttribute("href");
					epRef = XbrlDockUtils.optCleanUrl(epRef);

//					if (epRef.contains("01-01.xsdesef_cor-gen-en.xml")) {
//						epRef = epRef.replace("01-01.xsdesef_cor-gen-en.xml", "01-01_cor.xsd");
//					} else if (epRef.contains("01-01.xsdesef_cor-lab-en.xml")) {
//						epRef = epRef.replace("01-01.xsdesef_cor-lab-en.xml", "01-01_cor.xsd");
//					}

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

		XbrlDockStreamXml.ChildProcessor procEntryPointList = new XbrlDockStreamXml.ChildProcessor() {
			@Override
			public void processChild(String tagName, Element ch) {
				switch (tagName) {
				case "entryPoint":
					XbrlDockStreamXml.processChildren(ch, procEntryPointItem);
					break;
				default:
					break;
				}
			}
		};

		XbrlDockStreamXml.processChildren(eTaxPack, new XbrlDockStreamXml.ChildProcessor() {
			@Override
			public void processChild(String tagName, Element ch) {
				switch (tagName) {
				case "entryPoints":
					XbrlDockStreamXml.processChildren(ch, procEntryPointList);
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

}
