package com.xbrldock.poc.taxonomy;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.xbrldock.XbrlDock;
import com.xbrldock.poc.XbrlDockDevUrlCache;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockTaxonomyManager implements XbrlDockTaxonomyConsts {

	private final XbrlDockDevUrlCache urlCache;

	private final Map<String, Map<String, Object>> itemCache = new TreeMap<>();
	private Set<String> loaded = new TreeSet<>();
	private Set<String> toLoad = new TreeSet<>();
	
	Set<String> linkbases = new TreeSet<String>();


	ItemCreator<Map> fileCacheCreator = new ItemCreatorSimple<Map>(TreeMap.class) {
		@Override
		public Map create(Object key, Object... hints) {
			Map ret = super.create(key, hints);
			toLoad.add((String) key);
			return ret;
		}
	};

	public XbrlDockTaxonomyManager(String cacheRoot) {
		urlCache = new XbrlDockDevUrlCache(cacheRoot);

		XbrlDockUtilsXml.setDefEntityResolver(urlCache);
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

		Map m = XbrlDockUtils.safeGet(itemCache, realRef, fileCacheCreator);
		m = XbrlDockUtils.safeGet(m, id, MAP_CREATOR);

		return m;
	}

	public void loadTaxonomy(File fMetaInf, String... urls) throws Exception {

		for (String schemaUrl : urls) {
			loadSchema(schemaUrl);
		}

		loadQueue();

		for (Map.Entry<String, Map<String, Object>> ie : itemCache.entrySet()) {
			XbrlDock.log(EventLevel.Info, ie.getKey(), ie.getValue().size());
		}
	}

	public void loadSchema(String schemaUrl) throws Exception {

		try (InputStream is = urlCache.resolveEntityStream("", schemaUrl)) {
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
					linkbases.add( optExtendRef(sl, path));
					break;
				case "xsd:element":
					em = getItem(schemaUrl, e.getAttribute("id"), path);
					XbrlDockUtilsXml.readAtts(e, em);
					break;
				case "link:roleType":
					em = getItem(schemaUrl, e.getAttribute("id"), path);
					XbrlDockUtilsXml.readAtts(e, em);
					
					cl = e.getChildNodes();
					for (int ii = cl.getLength(); ii-- > 0;) {
						Node cn = cl.item(ii);
						String nodeName = cn.getNodeName();
						String v = (String) em.get(nodeName);
						
						v = (null == v ) ? cn.getNodeValue() : v + ", " + cn.getNodeValue();
						em.put(nodeName, v);
					}

					break;
				}

				if (null != sl) {
					if (!sl.contains(XBRLDOCK_URL_PSEP)) {
						sl = path + "/" + sl;
					}
					optQueue(sl);
				}
			}
		}

		loaded.add(schemaUrl);
		toLoad.remove(schemaUrl);
	}

	public void loadLinkbase(String url) throws Exception {

		try (InputStream is = urlCache.resolveEntityStream("", url)) {
			Element eRoot = XbrlDockUtilsXml.parseDoc(is).getDocumentElement();

			String path = XbrlDockUtils.cutPostfix(url, "/");

			NodeList nl;
			NodeList cl;
			int nc;

			nl = eRoot.getElementsByTagName("*");
			nc = nl.getLength();

			Map<String, Map> content = new TreeMap<>();
			List<Element> arcs = new ArrayList<>();

			for (int idx = 0; idx < nc; ++idx) {
				Element e = (Element) nl.item(idx);
				String tagName = e.getTagName();

				String lt = e.getAttribute("xlink:type");

				Map em = null;

				switch (lt) {
				case "locator":
					
					em = getItem(e, path);
					break;
				case "resource":
					switch (tagName) {
					case "link:label":
						em = XbrlDockUtilsXml.readAtts(e, null);
						break;
					case "link:reference":
						em = new TreeMap<String, String>();
						cl = e.getChildNodes();
						for (int ii = cl.getLength(); ii-- > 0;) {
							Node cn = cl.item(ii);
							em.put(cn.getNodeName(), cn.getNodeValue());
						}
						break;
					}
					break;
				case "arc":
					arcs.add(e);
					break;
				}

				if (null != em) {
					String label = e.getAttribute("xlink:label");
					content.put(label, em);
				}
			}

			for (Element e : arcs) {
				String ar = e.getAttribute("xlink:arcrole");
				ar = XbrlDockUtils.getPostfix(ar, "/");
				switch (ar) {
				case "concept-label":
					break;
				case "concept-reference":
					break;
				default:
					break;
				}
			}
		}
		
		loaded.add(url);
		linkbases.remove(url);

	}

	private boolean optQueue(String schemaUrl) {
		return loaded.contains(schemaUrl) ? false : toLoad.add(schemaUrl);
	}

	public void loadQueue() throws Exception {
		while (!toLoad.isEmpty()) {
			String next = toLoad.iterator().next();
			loadSchema(next);
		}
		
		while (!linkbases.isEmpty()) {
			String next = linkbases.iterator().next();
			loadLinkbase(next);
		}
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
