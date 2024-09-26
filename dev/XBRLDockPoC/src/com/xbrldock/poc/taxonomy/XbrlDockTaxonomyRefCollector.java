package com.xbrldock.poc.taxonomy;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevCounter;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsXml;

public class XbrlDockTaxonomyRefCollector implements XbrlDockTaxonomyConsts, XbrlDockPocConsts.ReportDataHandler {

	String repId;
	Map<String, String> imports = new TreeMap<>();
	Map<String, File> prefixes = new TreeMap<>();

	XbrlDockDevCounter refUrlCount = new XbrlDockDevCounter("Imported URLs", true);
	XbrlDockDevCounter unresolvedCount = new XbrlDockDevCounter("Unresolved namespaces", true);

	@Override
	public void beginReport(String repId) {
		this.repId = repId;
	}

	@Override
	public void addNamespace(String ref, String id) {		
		String url = imports.get(id);

//		for (Map.Entry<String, File> pe : prefixes.entrySet()) {
//			String p = pe.getKey();
//
//			if (id.startsWith(p)) {
//				refUrlCount.add("<<local>>");
//				return;
//			}
//		}

		if (XbrlDockUtils.isEmpty(url)) {
			unresolvedCount.add(id);
		} else {
			try {
				loadTaxonomy(ref, url);
			} catch (Exception e) {
				XbrlDockException.swallow(e, "Loading taxonomy", ref, id);
			}

			refUrlCount.add(url);
		}
	}

	@Override
	public void addTaxonomy(String tx) {
		try {
			loadTaxonomy("", tx);
		} catch (Exception e) {
			XbrlDockException.swallow(e, "addTaxonomy", tx);
		}

//		for (Map.Entry<String, File> pe : prefixes.entrySet()) {
//			String p = pe.getKey();
//
//			if (tx.startsWith(p)) {
//				refUrlCount.add("<<local>>");
//				return;
//			}
//		}
	}

	@Override
	public String processSegment(String segment, Map<String, Object> data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void endReport() {
	}

	public void init(Map<String, File> localPrefixes) {
		prefixes.clear();
		prefixes.putAll(localPrefixes);

		imports.clear();

//		NodeList nl = taxRoot.getElementsByTagName("*");
//		int nc = nl.getLength();
//
//		for (int idx = 0; idx < nc; ++idx) {
//			Element e = (Element) nl.item(idx);
//			String tagName = e.getTagName();
//
//			switch (tagName) {
//			case "import":
//				imports.put(e.getAttribute("namespace"), e.getAttribute("schemaLocation"));
//				break;
//			}
//		}
	}

	public void loadTaxonomy(String id, String taxRef) throws Exception {
		Element taxRoot = null;

		for (Map.Entry<String, File> pe : prefixes.entrySet()) {
			String p = pe.getKey();

			if (taxRef.startsWith(p)) {
				File fr = new File(pe.getValue(), taxRef.substring(p.length()));
				taxRoot = XbrlDockUtilsXml.parseDoc(fr).getDocumentElement();
				break;
			}
		}

//		if (null == taxRoot) {
//			InputStream is = null;
//			try {
//				is = XbrlDockPoc.URL_CACHE.resolveEntityStream(id, taxRef);
//				taxRoot = XbrlDockUtilsXml.parseDoc(is).getDocumentElement();
//			} finally {
//				if (null != is) {
//					is.close();
//				}
//			}
//		}

		if (null == taxRoot) {
			XbrlDock.log(EventLevel.Warning, "Taxonomy resolution error", id, taxRef);
		} else {
			NodeList nl = taxRoot.getElementsByTagName("*");
			int nc = nl.getLength();

			for (int idx = 0; idx < nc; ++idx) {
				Element e = (Element) nl.item(idx);
				String tagName = e.getTagName();

				switch (tagName) {
				case "import":
					String nsId = e.getAttribute("namespace");
					String nsLoc = e.getAttribute("schemaLocation");
					
					if ( !nsLoc.startsWith("http") ) {
						nsLoc =  XbrlDockUtils.cutPostfix(taxRef, "/") + "/" + nsLoc;
					}
										
					if ( null == imports.put(nsId, nsLoc) ) {
						loadTaxonomy(nsId, nsLoc);	
					}
					break;
				}
			}
		}
	}

	@Override
	public String toString() {
		return refUrlCount.toString() + "\n" + unresolvedCount.toString();
	}
}
