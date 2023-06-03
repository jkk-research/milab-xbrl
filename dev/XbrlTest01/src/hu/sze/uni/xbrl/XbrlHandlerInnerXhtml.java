package hu.sze.uni.xbrl;

import java.util.Arrays;
import java.util.Collection;

import org.xml.sax.Attributes;

@SuppressWarnings({ "unchecked" })
public class XbrlHandlerInnerXhtml extends XbrlHandlerXml implements XbrlConsts {
	private static final Collection<String> FACT_KEYS = Arrays.asList("ix:nonFraction", "ix:nonNumeric");

//	public final Set<String> keys = new TreeSet<>();
	boolean readFact;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if ( qName.startsWith(XBRL_PREFIX_XBRLI) ) {
			super.startElement(uri, localName, qName, attributes);
		} else {
			if ( FACT_KEYS.contains(qName) ) {
				readFact = true;
				currentValue.setLength(0);

				dim.clear();

				for (int i = attributes.getLength(); i-- > 0;) {
					String qn = attributes.getQName(i);
					dim.put(XbrlUtils.XML_TRANSLATE.getOrDefault(qn, qn), attributes.getValue(i));
				}
				
//				if ( "ix:nonNumeric".equals(qName) ) {
//					System.out.println(dim);
//				}
//			} else {
//				keys.add(qName);
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if ( qName.startsWith(XBRL_PREFIX_XBRLI) ) {
			super.endElement(uri, localName, qName);
		} else {
			if ( readFact ) {
				fact.put("value", currentValue.toString());
				listener.handleXbrlInfo(XbrlInfoType.Fact, fact);
				readFact = false;
			}
		}
	}
	
}