package com.xbrldock.poc.taxonomy;

import com.xbrldock.poc.XbrlDockDevUrlCache;
import com.xbrldock.utils.XbrlDockUtilsXml;

public class XbrlDockTaxonomyManager implements XbrlDockTaxonomyConsts {
	
	private final XbrlDockDevUrlCache urlCache;
	
	public XbrlDockTaxonomyManager(String cacheRoot) {
		urlCache = new XbrlDockDevUrlCache(cacheRoot);
		
		XbrlDockUtilsXml.setDefEntityResolver(urlCache);
	}
	
	public void getTaxonomies(XbrlSource source, String reportId) {

//	String str = XbrlDockUtils.simpleGet(filingData, XbrlFilingKeys.localMetaInfPath);
//
//	if (XbrlDockUtils.isEmpty(str)) {
//		XbrlDock.log(EventLevel.Error, "Missing META_INF folder", f.getCanonicalPath());
//		return;
//	}
//	File fMetaInf = new File(cacheRoot, str);
//
//	if (!fMetaInf.isDirectory()) {
//		XbrlDock.log(EventLevel.Error, "Missing META_INF folder", fMetaInf.getCanonicalPath());
//		return;
//	}
//
//	NodeList nl;
//	int nc;
//
//	File fCat = new File(fMetaInf, XBRLDOCK_FNAME_CATALOG);
//	Element eCatalog = XbrlDockUtilsXml.parseDoc(fCat).getDocumentElement();
//	
//	Map<String, File> prefixes = new TreeMap<>();
//
//	nl = eCatalog.getElementsByTagName("*");
//	nc = nl.getLength();
//
//	for (int idx = 0; idx < nc; ++idx) {
//		Element e = (Element) nl.item(idx);
//		String tagName = e.getTagName();
//
//		switch (tagName) {
//		case "rewriteURI":
//			String uriStart = e.getAttribute("uriStartString");
//			if ( uriStart.endsWith("/") ) {
//				uriStart = uriStart.substring(0, uriStart.length()-1);
//			}
//			prefixes.put(uriStart, new File(fMetaInf, e.getAttribute("rewritePrefix")));
//			break;
//		}
//	}

//	File fTax = new File(fMetaInf, XBRLDOCK_FNAME_TAXPACK);
//	Element eTaxPack = XbrlDockUtilsXml.parseDoc(fTax, XbrlDockPoc.URL_CACHE).getDocumentElement();
//
//	nl = eTaxPack.getElementsByTagName("*");
//	nc = nl.getLength();
//	
//	for (int idx = 0; idx < nc; ++idx) {
//		Element e = (Element) nl.item(idx);
//		String tagName = e.getTagName();
//
//		switch (tagName) {
//		case "tp:entryPointDocument":
//			str = e.getAttribute("href");
//			
//			for ( Map.Entry<String, File> pe : prefixes.entrySet()) {
//				String p = pe.getKey();
//				
//				if ( str.startsWith(p) ) {
//					File fr = new File(pe.getValue(), str.substring(p.length()));
//					dh.init( XbrlDockUtilsXml.parseDoc(fr, XbrlDockPoc.URL_CACHE).getDocumentElement(), prefixes);				
//				}
//			}
//
//			break;
//		}
//	}
	}

}
