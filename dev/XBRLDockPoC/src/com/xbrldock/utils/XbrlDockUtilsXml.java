package com.xbrldock.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.xbrldock.XbrlDockException;

public class XbrlDockUtilsXml implements XbrlDockUtilsConsts {

	private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

	private static EntityResolver CACHED_RESOLVER = new EntityResolver() {
		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			String url = systemId.startsWith("http") ? systemId : publicId.startsWith("http") ? publicId : null;
			return (null == url) ? new InputSource() : XbrlDockUtilsNet.resolveEntity(url);
		}
	};

	private static ThreadLocal<DocumentBuilder> tdb = new ThreadLocal<DocumentBuilder>() {
		protected DocumentBuilder initialValue() {
			try {
				dbf.setValidating(false);
				return dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				return XbrlDockException.wrap(e);
			}
		};
	};

	public static Document parseDoc(InputStream is) throws Exception {
		DocumentBuilder db = tdb.get();
		db.reset();

		db.setEntityResolver(CACHED_RESOLVER);

		return db.parse(is);
	}

	public static Document parseDoc(File f) throws Exception {
		try (FileInputStream fis = new FileInputStream(f)) {
			return parseDoc(fis);
		}
	}

	public static Map<String, String> readAtts(Element e, Map<String, String> target) throws Exception {
		if (null == target) {
			target = new TreeMap<String, String>();
		}

		NamedNodeMap nm = e.getAttributes();
		int nc = (null == nm) ? 0 : nm.getLength();

		for (int i = nc; i-- > 0;) {
			Node node = nm.item(i);
			String an = node.getNodeName();

			String av = node.getNodeValue().trim();

			if (!XbrlDockUtils.isEmpty(av)) {
				target.put(an, av);
			}
		}

		return target;
	};

	public static Map<String, String> readChildNodes(Element e, Map<String, String> target) {
		if (null == target) {
			target = new TreeMap<String, String>();
		}

		NodeList cl;
		cl = e.getChildNodes();
		for (int ii = cl.getLength(); ii-- > 0;) {
			Node cn = cl.item(ii);
			String nodeName = cn.getNodeName();
			String nodeVal = cn.getTextContent();
			if (null != nodeVal) {
				nodeVal = nodeVal.trim();
			}
			if (!XbrlDockUtils.isEmpty(nodeVal)) {
				String v = (String) target.get(nodeName);

				v = (null == v) ? nodeVal : v + " " + nodeVal;
				target.put(nodeName, v);
			}
		}

		return target;
	}

	public static abstract class ChildProcessor {
		public abstract void processChild(String tagName, Element ch);
		public void finish() {};
	}

	public static void processChildren(Element eTaxPack, ChildProcessor childProcessor) {
		NodeList nl = eTaxPack.getChildNodes();
		int nc = nl.getLength();

		for (int idx = 0; idx < nc; ++idx) {
			Node n = nl.item(idx);
			if (n instanceof Element) {
				Element e = (Element) n;
				String tagName = XbrlDockUtils.getPostfix(e.getTagName(), ":");
				childProcessor.processChild(tagName, e);
			}
		}
		
		childProcessor.finish();
	}
}
