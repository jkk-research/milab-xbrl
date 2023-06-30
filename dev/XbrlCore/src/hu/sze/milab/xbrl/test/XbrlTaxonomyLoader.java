package hu.sze.milab.xbrl.test;

import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.stream.xml.DustStreamXmlConsts;
import hu.sze.milab.dust.stream.xml.DustStreamXmlLoader;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtils.QueueContainer;

class XbrlTaxonomyLoader implements DustStreamXmlLoader.NamespaceProcessor, DustStreamXmlConsts {

	class NamespaceData {
		Element e;
		Map<String, Element> items = new TreeMap<>();

		public NamespaceData(Element eNS, NodeList el) {
			this.e = eNS;

			int ec = el.getLength();
			for (int ei = 0; ei < ec; ++ei) {

				Node item = el.item(ei);
				NamedNodeMap atts = item.getAttributes();

				if ( null != atts ) {
					String id = atts.getNamedItem("id").getNodeValue();
					items.put(id, (Element) item);
				}
			}
		}
	}

	Map<String, NamespaceData> namespaces = new TreeMap<>();
	Map<String, NamespaceData> nsByUrl = new TreeMap<>();

	@Override
	public void namespaceLoaded(Element root, QueueContainer<String> loader) {
		NodeList el;

//		String url = root.getAttribute(XML_ATT_REF);
		String url = (String) root.getUserData(XML_DATA_DOCURL);

//		Dust.dumpObs("  Reading url", url);

		el = root.getElementsByTagName("xsd:element");
		if ( 0 < el.getLength() ) {
			NamespaceData nsd = new NamespaceData(root, el);

			String ns = root.getAttribute("targetNamespace");
			namespaces.put(ns, nsd);
			nsByUrl.put(url, nsd);

			Dust.dumpObs("      Registered namespace", ns);
		}

		if ( root.getTagName().endsWith("linkbase") ) {
			Dust.dumpObs("      Linkbase found", url);
		}

		el = root.getElementsByTagName("link:linkbaseRef");
		for (int ei = el.getLength(); ei-- > 0;) {
			String href = ((Element) el.item(ei)).getAttribute("xlink:href");
			String refUrl = href;

			if ( DustUtils.isEmpty(href) ) {
				Dust.dumpObs(" HEH?");
			} else {
				if ( !href.contains(":") ) {
//					int sep = url.lastIndexOf("/");
//					refUrl = url.substring(0, sep + 1) + href;
					refUrl = DustUtils.replacePostfix(url, "/", href);
				}
				loader.enqueue(href, refUrl);
			}
		}
	}
}