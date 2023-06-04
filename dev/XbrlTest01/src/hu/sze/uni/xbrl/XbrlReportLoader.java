package hu.sze.uni.xbrl;

import java.io.File;
import java.io.FileReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.simple.parser.JSONParser;

public class XbrlReportLoader implements XbrlConsts {

	XbrlListener listener;

	JSONParser jsonParser;
	XbrlHandlerJson hJson;

	SAXParser xmlParser;
	XbrlHandlerXml hXml;
	XbrlHandlerInnerXhtml hXhtml;

	public XbrlReportLoader(XbrlListener listener) {
		this.listener = listener;
	}

	public void load(File f) throws Exception {

		XbrlReportFormat fmt = XbrlReportFormat.getFormat(f);
		if ( null != fmt ) {

			if ( fmt == XbrlReportFormat.JSON ) {
				if ( null == hJson ) {
					jsonParser = new JSONParser();
					hJson = new XbrlHandlerJson();
					hJson.setListener(listener);
				}
				
				jsonParser.parse(new FileReader(f), hJson, true);
			} else {
				if ( null == xmlParser ) {
					SAXParserFactory factory = SAXParserFactory.newInstance();
					xmlParser = factory.newSAXParser();

					hXml = new XbrlHandlerXml();
					hXml.setListener(listener);

					hXhtml = new XbrlHandlerInnerXhtml();
					hXhtml.setListener(listener);
				}
				
				xmlParser.parse(f, fmt.isXml ? hXml : hXhtml);
			}
		}
	}
}
