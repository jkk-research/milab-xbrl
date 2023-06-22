package hu.sze.milab.dust.stream;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts;

public class DustStreamXmlAgent implements DustStreamConsts, DustConsts.MindAgent {

	public class XbrlUtilsXmlLoader extends DefaultHandler {

		private MindHandle hTarget;
		private String pendingElement;
		private StringBuilder collectedText = new StringBuilder();

		@Override
		public void startDocument() {
			hTarget = Dust.access(MIND_ATT_AGENT_SELF, MindAccess.Get, null, MISC_ATT_CONN_TARGET);
			Dust.access(hTarget, MindAccess.Commit, MindAction.Init);
		}
		
		@Override
		public void processingInstruction(String target, String data) throws SAXException {
			Dust.log("Processing instruction", target, data);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			optProcessCollectedText();
			optOpenPendingElement();

			pendingElement = qName;
			int ac = attributes.getLength();

			if ( 0 < ac ) {
				optOpenPendingElement();
				Dust.access(hTarget, MindAccess.Set, XmlData.Attribute, MIND_ATT_KNOWLEDGE_TAG);

				for (int a = ac; a-- > 0;) {
					String attName = attributes.getQName(a);
					String attVal = attributes.getValue(a);
					Dust.access(hTarget, MindAccess.Set, attName, TEXT_ATT_NAMED_NAME);
					Dust.access(hTarget, MindAccess.Set, attVal, MISC_ATT_VARIANT_VALUE);
					Dust.access(hTarget, MindAccess.Commit, MindAction.Process);
				}
			}
		}

		@Override
		public void characters(char ch[], int start, int length) {
			String str = new String(ch, start, length);
			if ( Character.isWhitespace(ch[start]) ) {
				collectedText.append(" ");
			}
			str = str.trim();
			if ( 0 < str.length() ) {
				collectedText.append(str);
				if ( Character.isWhitespace(ch[start + length - 1]) ) {
					collectedText.append(" ");
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
			optProcessCollectedText();

			Dust.access(hTarget, MindAccess.Set, null, MISC_ATT_VARIANT_VALUE);
//			Dust.access(hTarget, MindAccess.Reset, null);

			Dust.access(hTarget, MindAccess.Set, XmlData.Element, MIND_ATT_KNOWLEDGE_TAG);
			Dust.access(hTarget, MindAccess.Set, qName, TEXT_ATT_NAMED_NAME);

			MindAction action;
			
			if (null == pendingElement) {
				action = MindAction.End;
			} else {
				action = MindAction.Process;
				pendingElement = null;
			}

			Dust.access(hTarget, MindAccess.Commit, action);
		}

		@Override
		public void endDocument() {
			Dust.access(hTarget, MindAccess.Commit, MindAction.Release);
			hTarget = null;
		}

		public void optOpenPendingElement() {
			if ( null != pendingElement ) {
				Dust.access(hTarget, MindAccess.Set, null, MISC_ATT_VARIANT_VALUE);
				//				Dust.access(hTarget, MindAccess.Reset, null);
				Dust.access(hTarget, MindAccess.Set, XmlData.Element, MIND_ATT_KNOWLEDGE_TAG);
				Dust.access(hTarget, MindAccess.Set, pendingElement, TEXT_ATT_NAMED_NAME);
				Dust.access(hTarget, MindAccess.Commit, MindAction.Begin);
				pendingElement = null;
			}
		}

		public void optProcessCollectedText() {
			String lastString = collectedText.toString().trim();
			if ( 0 < lastString.length() ) {
				optOpenPendingElement();
				Dust.access(hTarget, MindAccess.Set, XmlData.Content, MIND_ATT_KNOWLEDGE_TAG);
				Dust.access(hTarget, MindAccess.Set, lastString, MISC_ATT_VARIANT_VALUE);
				Dust.access(hTarget, MindAccess.Commit, MindAction.Process);
				collectedText.setLength(0);
			}
		}
	}

	SAXParser xmlParser;

	@Override
	public MindStatus agentExecAction(MindAction action) throws Exception {
		MindStatus ret = MindStatus.Accept;

		switch ( action ) {
		case Init:
			if ( null == xmlParser ) {
				SAXParserFactory factory = SAXParserFactory.newInstance();
				xmlParser = factory.newSAXParser();
			}
			break;
		case Begin:
			break;
		case Process:
			File f = Dust.access(MIND_ATT_AGENT_SELF, MindAccess.Peek, null, STREAM_ATT_STREAM_FILE);
			XbrlUtilsXmlLoader loader = new XbrlUtilsXmlLoader();
			xmlParser.parse(f, loader);
			break;
		case End:
			break;
		case Release:
			if ( null != xmlParser ) {
				xmlParser = null;
			}
			break;
		}

		return ret;
	}

}
