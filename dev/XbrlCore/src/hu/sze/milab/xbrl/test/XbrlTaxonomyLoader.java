package hu.sze.milab.xbrl.test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.dev.DustDevFolderCoverage;
import hu.sze.milab.dust.stream.xml.DustStreamXmlConsts;
import hu.sze.milab.dust.stream.xml.DustStreamXmlDocumentGraphLoader;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtils.QueueContainer;
import hu.sze.milab.dust.utils.DustUtilsFactory;
import hu.sze.milab.xbrl.XbrlConsts;

public class XbrlTaxonomyLoader implements DustStreamXmlDocumentGraphLoader.XmlDocumentProcessor, DustStreamXmlConsts, XbrlConsts {

	class NamespaceData {
		Element e;
		Map<String, Element> items = new TreeMap<>();

		public NamespaceData(Element eNS) {
			this.e = eNS;
		}
	}

	class LinkData {
		Element link;
		Element from;
		Element to;

		public LinkData(Element link, Map<String, Element> links) {
			this.link = link;

			String ref = link.getAttribute("xlink:from");
			this.from = links.get(ref);
			ref = link.getAttribute("xlink:to");
			this.to = links.get(ref);
		}
	}

	DustDevCounter linkInfo = new DustDevCounter(true);

	Map<String, NamespaceData> namespaces = new TreeMap<>();
	Map<String, NamespaceData> nsByUrl = new TreeMap<>();

	Set<LinkData> allLinks = new HashSet<>();
	@SuppressWarnings({ "unchecked", "rawtypes" })
	DustUtilsFactory<String, Set<LinkData>> locLinks = new DustUtilsFactory.Simple(true, HashSet.class);

	public final DustUrlResolver urlResolver;
	DustDevFolderCoverage folderCoverage;

	Map<String, Object> ifrsDefs = new TreeMap<>();
	Set<String> rootItems = new TreeSet<>();

	public XbrlTaxonomyLoader(DustUrlResolver urlResolver) {
		this.urlResolver = urlResolver;

		File root = urlResolver.getRoot().getParentFile();

		folderCoverage = new DustDevFolderCoverage(root);
		Dust.dumpObs("Files to visit in folder", root.getName(), folderCoverage.countFilesToVisit());
	}

	public DustDevFolderCoverage getFolderCoverage() {
		return folderCoverage;
	}

	@Override
	public DustUrlResolver getUrlResolver() {
		return urlResolver;
	}

	public <RetType> RetType peek(Object val, Object... path) {
		return Dust.access(ifrsDefs, MindAccess.Peek, val, path);
	}

	public void dump() throws Exception {
//		collectData();

		folderCoverage.dump();

		Dust.dumpObs("namespaces", namespaces.size(), "nsByUrl", nsByUrl.size());

		DustDevCounter dc = new DustDevCounter(true);

		@SuppressWarnings("unchecked")
		Map<String, Map<String, Object>> defs = (Map<String, Map<String, Object>>) ifrsDefs.get("item");

		for (Map<String, Object> md : defs.values()) {
			for (Map.Entry<String, Object> ee : md.entrySet()) {
				String an = ee.getKey();
				if ( !"name".equals(an) ) {
					dc.add("Values " + an + ": " + ee.getValue());
				}
			}
		}

		DustDevCounter dcFrom = new DustDevCounter(true);
		DustDevCounter dcTo = new DustDevCounter(true);

		for (LinkData ld : allLinks) {
			Element el = ld.link;

			String tn = el.getTagName();
			String fromId = ld.from.getAttribute("xlink:href");
			fromId = DustUtils.getPostfix(fromId, "#");
			fromId = DustUtils.getPostfix(fromId, "_");
			String toId = ld.to.getAttribute("xlink:href");
			toId = DustUtils.getPostfix(toId, "#");
			toId = DustUtils.getPostfix(toId, "_");

			Object from = Dust.access(ifrsDefs, MindAccess.Peek, null, "item", fromId);
			Object to = Dust.access(ifrsDefs, MindAccess.Peek, null, "item", toId);

			switch ( tn ) {
			case "link:presentationArc":
				if ( (null != from) && (null != to) ) {
					dcFrom.add(fromId);
					dcTo.add(toId);
				}
				break;

			case "link:labelArc":
//				if ( "label".equals(toRole) ) 
				if ( null != from ) {
//					String txt = ld.to.getTextContent();
//					Dust.dumpObs(fromId, toRole, txt);
				}
				break;

			}
			dc.add("LinkTag " + tn);
		}

		Dust.dumpObs("---- Root items ---");
		for (String ri : rootItems) {
			Dust.dumpObs(ri, Dust.access(ifrsDefs, MindAccess.Peek, "???", "res", "en", ri, "label"));
		}
		Dust.dumpObs("-------");

//		dcFrom.dump("From");
//		dcTo.dump("To");

		dc.dump("Counts");

		linkInfo.dump("Links");

		Set<String> skipUrl = new TreeSet<>();

		for (String conceptRef : locLinks.keys()) {
			String[] ref = conceptRef.split("#");

			NamespaceData nsd = nsByUrl.get(ref[0]);
			if ( null == nsd ) {
				skipUrl.add(ref[0]);
			} else {
				Element e = nsd.items.get(ref[1]);
				if ( null == e ) {
					Dust.dumpObs("Referred concept not found", conceptRef);
				}
			}
		}

		for (String uu : skipUrl) {
			Dust.dumpObs("Referred url not found", uu);
		}
	}

	public void collectData() {
		for (NamespaceData nd : namespaces.values()) {
			for (Element ee : nd.items.values()) {
				String[] ii = ee.getAttribute("id").split("_");
				String tn = ii[0];

				if ( "ifrs-full".equals(tn) ) {
					String cn = ii[1];
					NamedNodeMap nma = ee.getAttributes();

					String group = null;
					Map<String, Object> md = new TreeMap<>();

					for (int i = nma.getLength(); i-- > 0;) {
						Attr a = (Attr) nma.item(i);
						String an = a.getName();
						an = DustUtils.getPostfix(an, ":");
						String val = a.getValue();
						val = DustUtils.getPostfix(val, ":");

						switch ( an ) {
						case "id":
							continue;
						case "type": {
							XbrlFactDataType fdt = null;

							switch ( val ) {
							case "durationItemType":
							case "dateItemType":
								fdt = XbrlFactDataType.date;
								break;
							case "stringItemType":
								fdt = XbrlFactDataType.string;
								break;
							case "textBlockItemType":
								fdt = XbrlFactDataType.text;
								break;
							case "domainItemType":
								fdt = null;
								break;

							default:
								fdt = XbrlFactDataType.number;
								break;
							}

							md.put(ATT_FACT_DATA_TYPE, fdt);
						}

							break;
						case "substitutionGroup":
							group = val;
							break;
						case "name":
							rootItems.add(val);
							break;
						default:
							break;
						}

						md.put(an, val);
					}

					if ( null != group ) {
						Dust.access(ifrsDefs, MindAccess.Set, md, group, cn);
					}
				}
			}
		}

		Set<String> dcFrom = new HashSet<>();

		for (LinkData ld : allLinks) {
			Element el = ld.link;

			String tn = el.getTagName();
			String fromId = ld.from.getAttribute("xlink:href");
			fromId = DustUtils.getPostfix(fromId, "#");
			fromId = DustUtils.getPostfix(fromId, "_");
			String toId = ld.to.getAttribute("xlink:href");
			toId = DustUtils.getPostfix(toId, "#");
			toId = DustUtils.getPostfix(toId, "_");

			String toRole = DustUtils.getPostfix(ld.to.getAttribute("xlink:role"), "/");

			Object from = Dust.access(ifrsDefs, MindAccess.Peek, null, "item", fromId);
			Object to = Dust.access(ifrsDefs, MindAccess.Peek, null, "item", toId);

			switch ( tn ) {
			case "link:presentationArc":
				if ( (null != from) && (null != to) ) {
					dcFrom.add(fromId);
				}
				rootItems.remove(toId);
				break;

			case "link:labelArc":
				if ( null != from ) {
					String txt = ld.to.getTextContent();
					Dust.access(ifrsDefs, MindAccess.Set, txt, "res", ld.to.getAttribute("xml:lang"), fromId, toRole);
				}
				break;
			}
		}

		for (Iterator<String> itRoot = rootItems.iterator(); itRoot.hasNext();) {
			String id = itRoot.next();
			if ( !dcFrom.contains(id) ) {
				itRoot.remove();
			}
		}
	}

	@Override
	public void documentLoaded(Element root, QueueContainer<String> loader) {
		NodeList el;

		String url = (String) root.getUserData(XML_DATA_DOCURL);

		if ( url.startsWith("file") ) {
			try {
				File f = Paths.get(new URL(url).toURI()).toFile();
				folderCoverage.setSeen(f);
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		NamespaceData nsd = null;

		el = root.getElementsByTagName("*");
		for (int ei = el.getLength(); ei-- > 0;) {
			Element e = (Element) el.item(ei);

			String id = e.getAttribute("id");
			if ( !DustUtils.isEmpty(id) ) {
				if ( null == nsd ) {
					nsd = new NamespaceData(root);
					String ns = root.getAttribute("targetNamespace");
					if ( !DustUtils.isEmpty(ns) ) {
						namespaces.put(ns, nsd);
//						Dust.dumpObs("      Registered namespace", ns);
					} else {
						Dust.dumpObs("No target namespace given", url);
					}
					nsByUrl.put(url, nsd);
				}

				nsd.items.put(id, e);
			}
		}

		if ( root.getTagName().endsWith("linkbase") ) {
			Map<String, Element> labeledLinks = new TreeMap<>();

			String[] LINK_ATTS = { "xlink:type", "xlink:role", "xlink:arcrole" };

			el = root.getElementsByTagName("*");
			for (int ei = el.getLength(); ei-- > 0;) {
				Element e = (Element) el.item(ei);

				String lbl = e.getAttribute("xlink:label");
				if ( !DustUtils.isEmpty(lbl) ) {
					labeledLinks.put(lbl, e);
				}

				String tn = e.getTagName();
				if ( tn.startsWith("link") ) {
					linkInfo.add(tn);

					for (String an : LINK_ATTS) {
						String av = e.getAttribute(an);
						if ( !DustUtils.isEmpty(av) ) {
							linkInfo.add(tn + " " + an + ": " + av);
						}
					}
				}
			}

			el = root.getElementsByTagName("*");
			for (int ei = el.getLength(); ei-- > 0;) {
				Element e = (Element) el.item(ei);

				String type = e.getAttribute("xlink:type");

				if ( "arc".equals(type) ) {
					LinkData ld = new LinkData(e, labeledLinks);

					allLinks.add(ld);

					String ref = ld.from.getAttribute("xlink:href");
					if ( !DustUtils.isEmpty(ref) ) {
						ref = urlResolver.optLocalizeUrl(ref, url);
						locLinks.get(ref).add(ld);
					}
					ref = ld.to.getAttribute("xlink:href");
					if ( !DustUtils.isEmpty(ref) ) {
						ref = urlResolver.optLocalizeUrl(ref, url);
						locLinks.get(ref).add(ld);
					}
				}
			}
		}

		el = root.getElementsByTagName("link:linkbaseRef");
		for (int ei = el.getLength(); ei-- > 0;) {
			String href = ((Element) el.item(ei)).getAttribute("xlink:href");
			String refUrl = urlResolver.optLocalizeUrl(href, url);
			loader.enqueue(href, refUrl);
		}

	}
}