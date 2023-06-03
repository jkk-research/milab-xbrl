package hu.sze.uni.xbrl;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings("rawtypes")
public class XbrlUtilsXmlLoader extends DefaultHandler implements XbrlConsts {

	XbrlUtilsDataCollector dc = new XbrlUtilsDataCollector();
	
	public static class NamespaceInfo {
		String url;
		Set<String> tags = new TreeSet<>();
		Set<String> atts = new TreeSet<>();
		
	}
	
	XbrlUtilsFactory<String, NamespaceInfo> namespaces = new XbrlUtilsFactory.Simple<String, NamespaceInfo>(true, NamespaceInfo.class);

	protected StringBuilder currentValue = new StringBuilder();

	public Map getRoot() {
		return (Map) dc.getCurrContainer();
	}

	@Override
	public void startDocument() {
		System.out.println("Start Document");
	}

	@Override
	public void endDocument() {
		System.out.println("End Document");
	}

	public void optOpenContent() {
		if ( dc.getCt() == ContainerType.Map ) {
			dc.setKey(XML_CONTENT);
			dc.open(ContainerType.Arr);
		}
	}

	public void checkNamespace(String name, String value) {
		String[] s = name.split(":");
		
		if ( s.length > 1 ) {
			String ns = s[0];
			NamespaceInfo nsi;
			
			if ( "xmlns".equals(ns) ) {
				nsi = namespaces.get(s[1]);
				nsi.url = value;
			} else {
				nsi = namespaces.get(ns);
				if ( null == value ) {
					nsi.tags.add(s[1]);
				} else {
					nsi.atts.add(s[1]);
				}
			}
		}
	}

	public void optStoreCurrentValue() {		
		String lastString = currentValue.toString().trim();
		if ( 0 < lastString.length() ) {
			optOpenContent();
			dc.putValue(lastString);
			currentValue.setLength(0);
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		optStoreCurrentValue();
		optOpenContent();

		dc.open(ContainerType.Map);

		dc.setKey(XML_ELEMENT);
		dc.putValue(qName);
		checkNamespace(qName, null);

		int ac = attributes.getLength();
		if ( 0 < ac ) {
			dc.setKey(XML_ATTRIBUTES);
			dc.open(ContainerType.Map);
			for (int a = ac; a-- > 0;) {
				String attName = attributes.getQName(a);
				String attVal = attributes.getValue(a);
				checkNamespace(attName, attVal);
				dc.setKey(attName);
				dc.putValue(attVal);
			}
			dc.close();
		}		
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		optStoreCurrentValue();
		
		if ( dc.getCt() == ContainerType.Arr ) {
			dc.close();
		}
		
		dc.close();
	}

	@Override
	public void characters(char ch[], int start, int length) {
		String str = new String(ch, start, length);
		if ( Character.isWhitespace(ch[start]) ) {
			currentValue.append(" ");
		}
		str = str.trim();
		if ( 0 < str.length() ) {
			currentValue.append(str);
			if ( Character.isWhitespace(ch[start + length - 1]) ) {
				currentValue.append(" ");
			}
		}
	}

}