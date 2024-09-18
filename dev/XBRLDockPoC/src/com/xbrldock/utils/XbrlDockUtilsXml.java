package com.xbrldock.utils;

import java.io.File;
import java.io.FileInputStream;
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
import org.xml.sax.EntityResolver;

import com.xbrldock.XbrlDockException;

public class XbrlDockUtilsXml implements XbrlDockUtilsConsts {

	private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

	private static EntityResolver DEF_ENTITY_RESOLVER;

	public static void setDefEntityResolver(EntityResolver er) {
		DEF_ENTITY_RESOLVER = er;
	}

	private static ThreadLocal<DocumentBuilder> tdb = new ThreadLocal<DocumentBuilder>() {
		protected DocumentBuilder initialValue() {
			try {
				return dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				return XbrlDockException.wrap(e);
			}
		};
	};

	public static Map<String, String> readAtts(Element e, Map<String, String> target) throws Exception {
		if (null == target) {
			target = new TreeMap<String, String>();
		} else {
			target.clear();
		}

		NamedNodeMap nm = e.getAttributes();
		int nc = (null == nm) ? 0 : nm.getLength();

		for (int i = nc; i-- > 0;) {
			Node node = nm.item(i);
			String an = node.getNodeName();
			
			if ( "#text".equals(an)) {
				continue;
			}

			String av = node.getNodeValue().trim();

			if (!XbrlDockUtils.isEmpty(av)) {
				target.put(an, av);
			}
		}

		return target;
	};

	public static Document parseDoc(InputStream is) throws Exception {
		DocumentBuilder db = tdb.get();
		db.reset();

		db.setEntityResolver(DEF_ENTITY_RESOLVER);

		return db.parse(is);
	}

	public static Document parseDoc(File f) throws Exception {
		try (FileInputStream fis = new FileInputStream(f)) {
			return parseDoc(fis);
		}
	}

}
