package hu.sze.uni.xbrl;

import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.simple.parser.JSONParser;

public class XbrlTest01 implements XbrlConsts {

//@formatter:off
	private static final String[] INPUT = { 
//			"filings.all.json",
			"bankfrtirolundvorarlbergaktiengesellschaft/META-INF/taxonomyPackage.xml",
			"esef_cor-lab-de.xml",
			"stellantis-2022-12-31-en/stellantis-2022-12-31-en.json", 
			"bankfrtirolundvorarlbergaktiengesellschaft/reports/bankfrtirolundvorarlbergaktiengesellschaft.xbrl",
			"volkswagenag/reports/VWAGAbschlussAnhang_IFRS_Konzern-2022-12-31-de.xhtml" 
	};
//@formatter:on

	private static File data = new File(System.getProperty("user.home") + "/work/xbrl/data");

	private static Map<String, Set<String>> ALL_KEYS = new TreeMap<>();
	static Set<String> CURR_KEYS;
	
	public static void main(String[] args) throws Exception {
//		System.out.println("XBRL value extractor looking for " + Arrays.asList(args) + "\n" );
//		XbrlListener listener = new XbrlListener.Dumper(args);

		XbrlListener listener = new XbrlListener.Collector();
		
		JSONParser jsonParser = new JSONParser();
		XbrlHandlerJson jsonHandler = new XbrlHandlerJson();
		jsonHandler.setListener(listener);

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser xmlParser = factory.newSAXParser();
		
		XbrlHandlerXml xmlHandler = new XbrlHandlerXml();
		xmlHandler.setListener(listener);
		
		XbrlHandlerInnerXhtml xhtmlHandler = new XbrlHandlerInnerXhtml();
		xhtmlHandler.setListener(listener);
		
		XbrlUtilsXmlLoader xmlLoader = new XbrlUtilsXmlLoader();

		for (String fn : INPUT) {
			File f = new File(data, fn);

			String err = null;

			if ( f.exists() ) {
				System.out.println("\nReading " + f.getCanonicalPath() );

				ALL_KEYS.put(f.getName(), CURR_KEYS = new TreeSet<>());

				int d = fn.lastIndexOf('.');
				String type = fn.substring(d + 1).toUpperCase();

				switch ( type ) {
				case "JSON":
					jsonParser.parse(new FileReader(f), jsonHandler, true);
					System.out.println("read complete");
					break;
				case "XHTML":
					xmlParser.parse(f, xhtmlHandler);
					break;
				case "XBRL":
					xmlParser.parse(f, xmlHandler);
					break;
				case "XML":
					xmlParser.parse(f, xmlLoader);
//					Object root = xmlLoader.getRoot();
					System.out.println(xmlLoader.namespaces);
					break;
				default:
					err = "reader not found";
					break;
				}
			} else {
				err = "file not found";
			}

			if ( null != err ) {
				System.out.println("ERROR: " + err);
			} else {
				System.out.println("SUCCESS!");
			}
			
			System.out.println(" ---- ");
		}
		
		listener.dump();
	}

}
