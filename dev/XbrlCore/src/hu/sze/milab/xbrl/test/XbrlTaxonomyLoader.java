package hu.sze.milab.xbrl.test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.stream.xml.DustStreamXmlConsts;
import hu.sze.milab.dust.stream.xml.DustStreamXmlLoader;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtils.QueueContainer;
import hu.sze.milab.dust.utils.DustUtilsFactory;
import hu.sze.milab.dust.utils.DustUtilsFile;

class XbrlTaxonomyLoader implements DustStreamXmlLoader.NamespaceProcessor, DustStreamXmlConsts {

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
	Set<String> allFiles = new TreeSet<>();

	Map<String, NamespaceData> namespaces = new TreeMap<>();
	Map<String, NamespaceData> nsByUrl = new TreeMap<>();

	Set<LinkData> allLinks = new HashSet<>();
	@SuppressWarnings({ "unchecked", "rawtypes" })
	DustUtilsFactory<String, Set<LinkData>> locLinks = new DustUtilsFactory.Simple(true, HashSet.class);

	void setRootFolder(File root) {
		allFiles.clear();
		addFolder(root);

		Dust.dumpObs("Files to visit in folder", root.getName(), allFiles.size());
	}

	void addFolder(File dir) {
		for (File f : dir.listFiles()) {
			if ( f.isFile() ) {
				if ( f.getName().startsWith(".") ) {
					continue;
				}
				try {
					allFiles.add(f.getCanonicalPath());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else if ( f.isDirectory() ) {
				addFolder(f);
			}
		}
	}

	public void setSeen(File... files) throws Exception {
		for (File f : files) {
			allFiles.remove(f.getCanonicalPath());
		}
	}

	public void dump() throws Exception {
		if ( allFiles.isEmpty() ) {
			Dust.dumpObs("All files visited");
		} else {
			Dust.dumpObs("Unseen files");

			for (String s : allFiles) {
				Dust.dumpObs("   ", s);
			}
		}

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

		linkInfo.dump();
	}

	@Override
	public void namespaceLoaded(Element root, QueueContainer<String> loader) {
		NodeList el;

//		String url = root.getAttribute(XML_ATT_REF);
		String url = (String) root.getUserData(XML_DATA_DOCURL);

		if ( url.startsWith("file") ) {
			try {
				File f = Paths.get(new URL(url).toURI()).toFile();
				setSeen(f);
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

//		Dust.dumpObs("  Reading url", url);
		
		if ( root.getTagName().endsWith(":schema")) {
			NamespaceData nsd = null;
			
			el = root.getElementsByTagName("*");
			for (int ei = el.getLength(); ei-- > 0;) {
				Element e = (Element) el.item(ei);
				
				String id = e.getAttribute("id");
				if ( !DustUtils.isEmpty(id) ) {
					if ( null == nsd ) {
						nsd = new NamespaceData(root);
						String ns = root.getAttribute("targetNamespace");
						namespaces.put(ns, nsd);

						nsByUrl.put(url, nsd);

						Dust.dumpObs("      Registered namespace", ns);
					}
					
					nsd.items.put(id, e);
				}
			}
		}

		if ( root.getTagName().endsWith("linkbase") ) {
//			Dust.dumpObs("      Linkbase found", url);

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
		}

		refUrl = DustUtilsFile.optRemoveUpFromPath(refUrl);

		return refUrl;
	}
}