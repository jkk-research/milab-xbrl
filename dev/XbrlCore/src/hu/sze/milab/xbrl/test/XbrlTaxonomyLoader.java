package hu.sze.milab.xbrl.test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashSet;
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
import hu.sze.milab.dust.utils.DustUtilsFile;

class XbrlTaxonomyLoader implements DustStreamXmlDocumentGraphLoader.XmlDocumentProcessor, DustStreamXmlConsts {

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

	File root;
	Map<String, String> uriRewrite;

	DustDevFolderCoverage folderCoverage;

	public XbrlTaxonomyLoader(File root, Map<String, String> uriRewrite) {
		this.root = root;
		this.uriRewrite = uriRewrite;

		folderCoverage = new DustDevFolderCoverage(root);

		Dust.dumpObs("Files to visit in folder", root.getName(), folderCoverage.countFilesToVisit());
	}

	public DustDevFolderCoverage getFolderCoverage() {
		return folderCoverage;
	}

	public void dump() throws Exception {
		folderCoverage.dump();

		Dust.dumpObs("namespaces", namespaces.size(), "nsByUrl", nsByUrl.size());

		DustDevCounter dc = new DustDevCounter(true);

		Map<String, Object> ifrsDefs = new TreeMap<>();

		for (NamespaceData nd : namespaces.values()) {
			String tns = nd.e.getAttribute("targetNamespace");

			for (Element ee : nd.items.values()) {
				String[] ii = ee.getAttribute("id").split("_");
				String tn = ii[0];
				dc.add("Taxonomy " + tns + ((1 == ii.length) ? "" : "::" + tn));

				if ( "ifrs-full".equals(tn) ) {
					dc.add("IFRS_FULL");
					String[] tt = ee.getAttribute("type").split(":");
					dc.add("Types " + tt[1]);

					String cn = ii[1];
					NamedNodeMap nma = ee.getAttributes();
					for (int i = nma.getLength(); i-- > 0;) {
						Attr a = (Attr) nma.item(i);
						String an = a.getName();
						an = DustUtils.getPostfix(an, ":");

						switch ( an ) {
						case "name":
						case "id":
							break;
						default:
							String val = a.getValue();
							val = DustUtils.getPostfix(val, ":");
							Dust.access(ifrsDefs, MindAccess.Set, val, cn, an);
							
							dc.add("Values " + an + ": " + val);
							break;
						}
					}

//					Element prev = ifrsDefs.put(cn, ee);
//					if ( null != prev ) {
//						Dust.dumpObs("Duplicate", cn, prev, ee);
//					}
//					Dust.dumpObs(ii[1], ee.getAttribute("type"));
				}
			}
		}
		
		for (LinkData ld : allLinks ) {
			Element el = ld.link;
			
			String tn = el.getTagName();
			
			switch ( tn ) {
			case "link:presentationArc":
				String fromId = ld.from.getAttribute("xlink:href");
				fromId = DustUtils.getPostfix(fromId, "#");
				String toId = ld.to.getAttribute("xlink:href");
				toId = DustUtils.getPostfix(toId, "#");
				Dust.dumpObs("arc from", fromId, "to", toId);
				break;
			}
			
			dc.add("LinkTag " + tn);
		}

		dc.dump("Counts");

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

		linkInfo.dump("Links");
	}

	public void save(File dir, String taxName) throws Exception {
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
						ref = optLocalizeUrl(ref, url);
						locLinks.get(ref).add(ld);
					}
					ref = ld.to.getAttribute("xlink:href");
					if ( !DustUtils.isEmpty(ref) ) {
						ref = optLocalizeUrl(ref, url);
						locLinks.get(ref).add(ld);
					}
				}
			}
		}

		el = root.getElementsByTagName("link:linkbaseRef");
		for (int ei = el.getLength(); ei-- > 0;) {
			String href = ((Element) el.item(ei)).getAttribute("xlink:href");
			String refUrl = optLocalizeUrl(href, url);
			loader.enqueue(href, refUrl);
		}
	}

	public String optLocalizeUrl(String href, String url) {
		String refUrl = href;

		if ( !href.contains(":") ) {
			refUrl = DustUtils.replacePostfix(url, "/", href);
		} else {
			for (Map.Entry<String, String> e : uriRewrite.entrySet()) {
				String prefix = e.getKey();
				if ( url.startsWith(prefix) ) {
					File f = new File(root, e.getValue());
					f = new File(f, url.substring(prefix.length()));
					try {
						refUrl = f.toURI().toURL().toString();
					} catch (Throwable e1) {
						e1.printStackTrace();
					}
					break;
				}
			}
		}

		refUrl = DustUtilsFile.optRemoveUpFromPath(refUrl);

		return refUrl;
	}
}