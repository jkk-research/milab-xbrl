package com.xbrldock.dev;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.poc.meta.XbrlDockMetaConsts;
import com.xbrldock.utils.XbrlDockUtils;

public class XbrlDockDevReportStats implements XbrlDockMetaConsts, XbrlDockPocConsts.ReportDataHandler {

	String repId;

	XbrlDockDevCounter taxonomies = new XbrlDockDevCounter("Taxonomies", true);
	Map<String, XbrlDockDevCounter> nsRefs = new TreeMap<String, XbrlDockDevCounter>();
	XbrlDockDevCounter errors = new XbrlDockDevCounter("Errors", true);

	ItemCreator<XbrlDockDevCounter> rc = new ItemCreator<XbrlDockDevCounter>() {
		@Override
		public XbrlDockDevCounter create(Object key, Object... hints) {
			return new XbrlDockDevCounter((String) key, true);
		}
	};

	XbrlDockDevCounter segCnt = new XbrlDockDevCounter("Segments", true);
	long factCount;

	@Override
	public void beginReport(String repId) {
		this.repId = repId;
		segCnt.reset();
	}

	@Override
	public void addNamespace(String ref, String id) {
		XbrlDockUtils.safeGet(nsRefs, ref, rc).add(id);
	}

	@Override
	public void addTaxonomy(String tx) {
		taxonomies.add(tx);
	}

	@Override
	public String processSegment(String segment, Map<String, Object> data) {
		String ret = "";
		Long sc = segCnt.add(segment);

		switch (segment) {
		case XDC_REP_SEG_Unit:
			ret = (String) data.get(XDC_FACT_TOKEN_unit);
			if (XbrlDockUtils.isEmpty(ret)) {
				ret = "unit-" + sc;
			}
			break;
		case XDC_REP_SEG_Context:
			ret = (String) data.get(XDC_FACT_TOKEN_context);
			if (XbrlDockUtils.isEmpty(ret)) {
				ret = "ctx-" + sc;
			}

			break;
		case XDC_REP_SEG_Fact:
			ret = (String) data.get(XDC_EXT_TOKEN_id);
			if (XbrlDockUtils.isEmpty(ret)) {
				ret = "fact-" + sc;
			}

			String concept = XbrlDockUtils.simpleGet(data, XDC_FACT_TOKEN_concept);
			int sep = concept.indexOf(":");
			if (-1 != sep) {
				String ns = concept.substring(0, sep);
				if (!nsRefs.containsKey(ns)) {
					errors.add("unresolved ns\t" + ns);
				}
			} else {
				errors.add("no ns\t" + concept);
			}

			++factCount;

			break;
		}

		return ret;
	}

	@Override
	public void endReport() {
	}

	public void init(Map<String, File> localPrefixes) {

		factCount = 0;
	}

//	public void loadTaxonomy(String id, String taxRef) throws Exception {
//		Element taxRoot = null;
//
//		for (Map.Entry<String, File> pe : prefixes.entrySet()) {
//			String p = pe.getKey();
//
//			if (taxRef.startsWith(p)) {
//				File fr = new File(pe.getValue(), taxRef.substring(p.length()));
//				taxRoot = XbrlDockUtilsXml.parseDoc(fr).getDocumentElement();
//				break;
//			}
//		}
//
////		if (null == taxRoot) {
////			InputStream is = null;
////			try {
////				is = XbrlDockPoc.URL_CACHE.resolveEntityStream(id, taxRef);
////				taxRoot = XbrlDockUtilsXml.parseDoc(is).getDocumentElement();
////			} finally {
////				if (null != is) {
////					is.close();
////				}
////			}
////		}
//
//		if (null == taxRoot) {
//			XbrlDock.log(EventLevel.Warning, "Taxonomy resolution error", id, taxRef);
//		} else {
//			NodeList nl = taxRoot.getElementsByTagName("*");
//			int nc = nl.getLength();
//
//			for (int idx = 0; idx < nc; ++idx) {
//				Element e = (Element) nl.item(idx);
//				String tagName = e.getTagName();
//
//				switch (tagName) {
//				case "import":
//					String nsId = e.getAttribute("namespace");
//					String nsLoc = e.getAttribute("schemaLocation");
//					
//					if ( !nsLoc.startsWith("http") ) {
//						nsLoc =  XbrlDockUtils.cutPostfix(taxRef, "/") + "/" + nsLoc;
//					}
//										
//					if ( null == imports.put(nsId, nsLoc) ) {
//						loadTaxonomy(nsId, nsLoc);	
//					}
//					break;
//				}
//			}
//		}
//	}

	@Override
	public String toString() {
		return taxonomies.toString() + " " + nsRefs;
	}
}
