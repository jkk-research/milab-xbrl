package com.xbrldock.utils.stream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.xbrldock.XbrlDockException;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsNet;

public class XbrlDockStreamXml implements XbrlDockStreamConsts {

	private static EntityResolver CACHED_RESOLVER = new EntityResolver() {
		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			String url = systemId.startsWith("http") ? systemId : publicId.startsWith("http") ? publicId : null;
			return (null == url) ? new InputSource() : XbrlDockUtilsNet.resolveEntity(url);
		}
	};

	private static ThreadLocal<DocumentBuilder> TDB = new ThreadLocal<DocumentBuilder>() {
		private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		protected DocumentBuilder initialValue() {
			try {
				dbf.setValidating(false);
				return dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				return XbrlDockException.wrap(e);
			}
		};
	};

	private static ThreadLocal<Transformer> TTF = new ThreadLocal<Transformer>() {
		private TransformerFactory tf = TransformerFactory.newInstance();

		protected Transformer initialValue() {
			try {
				return tf.newTransformer();
			} catch (TransformerConfigurationException e) {
				return XbrlDockException.wrap(e);
			}
		};
	};

	private static ThreadLocal<SAXParser> SPF = new ThreadLocal<SAXParser>() {
		private SAXParserFactory pf = SAXParserFactory.newInstance();

		protected SAXParser initialValue() {
			try {
				return pf.newSAXParser();
			} catch (Throwable e) {
				return XbrlDockException.wrap(e);
			}
		};
	};

	private static DocumentBuilder createDocBuilder() throws Exception {
		DocumentBuilder db = TDB.get();
		db.reset();

		db.setEntityResolver(CACHED_RESOLVER);

		return db;
	};

	public static void parseDoc(InputStream is, GenAgent saxAgent) throws Exception {
		SAXParser sp = SPF.get();

		DefaultHandler h = new DefaultHandler() {
			private StringBuilder currentValue = new StringBuilder();
			private Map<String, Object> params = new TreeMap<>();
			private Stack<Object> stack = new Stack<>();
			private Object element;
			private Object doc;

			@Override
			public void startDocument() {
				element = doc = XbrlDockUtils.optCallNoEx(saxAgent, XDC_CMD_GEN_Init, params);
			}

			@Override
			public void endDocument() {
				params.clear();
				XbrlDockUtils.optCallNoEx(saxAgent, XDC_CMD_GEN_Release, params);
			}

			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) {

				optSendText();

				stack.push(element);
				element = XbrlDockUtils.optCallNoEx(saxAgent, XDC_CMD_GEN_Begin, setParams(uri, localName, qName));

				for (int i = attributes.getLength(); i-- > 0;) {
					setParams(attributes.getURI(i), attributes.getLocalName(i), attributes.getQName(i));
					params.put(XDC_EXT_TOKEN_type, attributes.getType(i));
					params.put(XDC_EXT_TOKEN_value, attributes.getValue(i));
					XbrlDockUtils.optCallNoEx(saxAgent, XDC_CMD_GEN_Process, params);
				}
			}

			@Override
			public void endElement(String uri, String localName, String qName) {
				optSendText();
				XbrlDockUtils.optCallNoEx(saxAgent, XDC_CMD_GEN_End, setParams(uri, localName, qName));
				element = stack.pop();
			}

			@Override
			public void characters(char ch[], int start, int length) {
				currentValue.append(ch, start, length);
			}

			void optSendText() {
				if (!currentValue.isEmpty()) {
					String str = currentValue.toString().trim();
					if (!str.isEmpty()) {
						resetParams();
						params.put(XDC_EXT_TOKEN_value, str);
						XbrlDockUtils.optCallNoEx(saxAgent, XDC_CMD_GEN_Process, params);
					}
					currentValue.setLength(0);
				}
			}

			Map<String, Object> resetParams() {
				params.clear();

				params.put(XDC_EXT_TOKEN_root, doc);
				params.put(XDC_EXT_TOKEN_node, element);

				return params;
			}

			Map<String, Object> setParams(String uri, String localName, String qName) {
				resetParams();

				params.put(XDC_EXT_TOKEN_uri, uri);
				params.put(XDC_EXT_TOKEN_name, localName);
				params.put(XDC_EXT_TOKEN_id, qName);

				return params;
			}

		};

		sp.parse(is, h);
	}

	public static void parseDoc(File f, GenAgent saxHandler) throws Exception {
		try (FileInputStream fis = new FileInputStream(f)) {
			parseDoc(fis, saxHandler);
		}
	}

	public static Document parseDoc(InputStream is) throws Exception {
		DocumentBuilder db = createDocBuilder();
		return db.parse(is);
	}

	public static Document parseDoc(File f) throws Exception {
		try (FileInputStream fis = new FileInputStream(f)) {
			return parseDoc(fis);
		}
	}

	public static Document createDoc() throws Exception {
		DocumentBuilder db = createDocBuilder();
		return db.newDocument();
	}

	public static void saveDoc(Document doc, OutputStream os, int indent) throws Exception {
		Transformer transformer = TTF.get();

		if (0 < indent) {
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(indent));
		}

		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(os);

		transformer.transform(source, result);

		os.flush();
	}

	public static void saveDoc(Document doc, File f, int indent) throws Exception {
		XbrlDockUtilsFile.ensureDir(f.getParentFile());
		try (FileOutputStream os = new FileOutputStream(f)) {
			saveDoc(doc, os, indent);
		}
	}

	public static boolean optSet(Element e, String ns, String aName, Object val) {
		if (null != val) {
			String str = XbrlDockUtils.toString(val);
			if (!XbrlDockUtils.isEmpty(str)) {
				if (XbrlDockUtils.isEmpty(aName)) {
					e.setTextContent(str.trim());
				} else {
					String ai = XbrlDockUtils.isEmpty(ns) ? aName : ns + ":" + aName;
					e.setAttribute(ai, str.trim());
				}

				return true;
			}
		}

		return false;
	}

	public static Element createElement(Document xmlDoc, String ns, String tagId, Element parent, String id) {
		String ti = XbrlDockUtils.isEmpty(ns) ? tagId : ns + ":" + tagId;
		Element e = xmlDoc.createElement(ti);
		if (null != parent) {
			parent.appendChild(e);
		}
		if (!XbrlDockUtils.isEmpty(id)) {
			e.setAttribute("id", id);
		}
		return e;
	}

	public static String getInfo(Element e, String ns, String tagId) {
		String ti = XbrlDockUtils.isEmpty(ns) ? tagId : ns + ":" + tagId;

		NodeList nl = e.getElementsByTagName(ti);
		if (0 < nl.getLength()) {
			String val = nl.item(0).getTextContent();
			if (!XbrlDockUtils.isEmpty(val)) {
				return val.trim();
			}
		}
		return null;
	}

	public static Map<String, String> readAtts(Element e, Map<String, String> target) throws Exception {
		return readAtts(e, null, target);
	}

	public static Map<String, String> readAtts(Element e, String txtAttName, Map<String, String> target) throws Exception {
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

		if (null != txtAttName) {
			String tx = e.getTextContent();
			if (!XbrlDockUtils.isEmpty(tx)) {
				tx = tx.replaceAll("\\s+", " ").trim();
				target.put(txtAttName, tx);
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

		public void finish() {
		};
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
