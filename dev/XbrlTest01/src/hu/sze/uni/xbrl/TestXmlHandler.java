package hu.sze.uni.xbrl;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class TestXmlHandler extends DefaultHandler {

		private StringBuilder currentValue = new StringBuilder();

		@Override
		public void startDocument() {
			System.out.println("Start Document");
		}

		@Override
		public void endDocument() {
			System.out.println("End Document");
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			currentValue.setLength(0);

			XbrlTest01.CURR_KEYS.add("TAG " + qName);

			for (int i = attributes.getLength(); i-- > 0;) {
				XbrlTest01.CURR_KEYS.add("ATT " + attributes.getQName(i));
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
//			System.out.printf("End Element : %s%n", qName);
		}

		@Override
		public void characters(char ch[], int start, int length) {
			currentValue.append(ch, start, length);
		}
	}