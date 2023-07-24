package hu.sze.milab.xbrl.test;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.dev.DustDevConsts;
import hu.sze.milab.dust.net.DustNetConsts;
import hu.sze.milab.dust.stream.DustStreamUrlCache;
import hu.sze.milab.dust.stream.xml.DustStreamXmlAgentParser;
import hu.sze.milab.dust.stream.xml.DustStreamXmlDocumentGraphLoader;
import hu.sze.milab.xbrl.XbrlConsts;
import hu.sze.milab.xbrl.XbrlReportAgentXhtmlReader;

public class XbrlTest02 implements XbrlConsts, DustDevConsts, DustNetConsts {

	private static File dataDir;
	private static File out;
	private static File in;

	public static void main(String[] args) throws Exception {
		long ts = System.currentTimeMillis();

		Dust.main(args);

		String data = System.getProperty("user.home") + "/work/xbrl/data";
		dataDir = new File(data);

		out = new File("out");
		out.mkdirs();

		in = new File("in");
		in.mkdirs();

		readReports(new String[] { 
			"in/volkswagenag/reports/VWAGAbschlussAnhang_IFRS_Konzern-2022-12-31-de.xhtml",
			});
//		readTaxonomy(new String[] { 
//				"EFRAG-ESRS-2022-PoC-Taxonomy", 
//				"IFRSAT-2022-03-24", 
//				"esef_taxonomy_2022",
//				});

//		readJsons(new String[] { 
//				"jsonapi_01",
//				"banks",
//				});

//		startServer(args);
		
		Dust.dumpObs("Process complete in", System.currentTimeMillis() - ts, "msec.");
	}

	public static void startServer(String[] args) throws Exception {

		MindHandle hSrv = Dust.resolveID(null, null);
		Dust.access(hSrv, MindAccess.Set, NET_LOG_SRVJETTY, MIND_ATT_AGENT_LOGIC);
		Dust.access(hSrv, MindAccess.Set, hSrv, MIND_ATT_KNOWLEDGE_LISTENERS);

		Dust.access(hSrv, MindAccess.Commit, MindAction.Process);

	}

	public static void readTaxonomy(String[] args) throws Exception {
		DustStreamUrlCache c = new DustStreamUrlCache(new File(dataDir, "urlCache"), false);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

		for (String taxRoot : args) {
			Dust.dumpObs("Reading taxonomy", taxRoot);

			File fRoot = new File(in, taxRoot);
			File txMeta = new File(fRoot, "META-INF");

			File fCat = new File(txMeta, "catalog.xml");
			Document catalog = db.parse(fCat);
			File fTaxPack = new File(txMeta, "taxonomyPackage.xml");
			Element taxPack = db.parse(fTaxPack).getDocumentElement();

			Map<String, String> uriRewrite = new TreeMap<>();
			NodeList nl = catalog.getElementsByTagName("rewriteURI");
			for (int ni = nl.getLength(); ni-- > 0;) {
				NamedNodeMap atts = nl.item(ni).getAttributes();
				uriRewrite.put(atts.getNamedItem("uriStartString").getNodeValue(), atts.getNamedItem("rewritePrefix").getNodeValue());
			}

			DustStreamXmlDocumentGraphLoader xmlLoader = new DustStreamXmlDocumentGraphLoader(c);

			XbrlTaxonomyLoader taxonomyCollector = new XbrlTaxonomyLoader(fRoot, uriRewrite);
			taxonomyCollector.setSeen(fCat, fTaxPack);

			nl = taxPack.getElementsByTagName("tp:entryPointDocument");
			for (int ni = 0; ni < nl.getLength(); ++ni) {
				String url = nl.item(ni).getAttributes().getNamedItem("href").getNodeValue();
				xmlLoader.loadDocument(txMeta, url, taxonomyCollector, uriRewrite);
			}

			taxonomyCollector.dump();

			taxonomyCollector.save(out, taxRoot + "_refs");
		}
	}

	public static void readJsons(String[] args) throws Exception {

//		DustStreamJsonAgentParser aJson = new DustStreamJsonAgentParser();
//
//		aJson.agentExecAction(MindAction.Init);
//
//		MindHandle target = Dust.createHandle();
//		Dust.access(MIND_ATT_AGENT_SELF, MindAccess.Set, target, MISC_ATT_CONN_TARGET);

//		DustDevAgentDump dump = new DustDevAgentDump();
//		dump.prefix = "Commit dump";

//		DustStreamJsonAgentWriter dump = new DustStreamJsonAgentWriter();
//		dump.hTarget = target;
//		dump.ps = System.out;
//		dump.ps = new PrintStream(new File(out, "jsonTest.json"));

//		DustStreamJsonApiAgentSerializer dump = new DustStreamJsonApiAgentSerializer();
//
//		MindHandle listener = Dust.createHandle();
//		Dust.access(listener, MindAccess.Set, dump, DUST_ATT_NATIVE_INSTANCE);
//		Dust.access(target, MindAccess.Set, listener, MIND_ATT_KNOWLEDGE_LISTENERS);

		Dust.dumpObs("Read JSON in XBRL");

		MindHandle hRead = Dust.resolveID(null, null);
		Dust.access(hRead, MindAccess.Set, STREAM_LOG_JSONAPISERIALIZER, MIND_ATT_AGENT_LOGIC);
		Dust.access(hRead, MindAccess.Set, hRead, MIND_ATT_KNOWLEDGE_LISTENERS);

		MindHandle target = Dust.resolveID(null, null);
		Dust.access(hRead, MindAccess.Set, target, MISC_ATT_CONN_TARGET);

		MindHandle listener = Dust.resolveID(null, null);
		Dust.access(listener, MindAccess.Set, DEV_LOG_DUMP, MIND_ATT_AGENT_LOGIC);
		Dust.access(target, MindAccess.Set, listener, MIND_ATT_KNOWLEDGE_LISTENERS);

		for (String fn : args) {
			File fIn = new File(in, fn + ".json");

			Dust.access(hRead, MindAccess.Set, fIn, STREAM_ATT_STREAM_FILE);
			Dust.access(hRead, MindAccess.Commit, MindAction.Process);

//			Dust.access(MIND_ATT_AGENT_SELF, MindAccess.Set, fIn, STREAM_ATT_STREAM_FILE);
//			aJson.agentExecAction(MindAction.Process);
		}

//		aJson.agentExecAction(MindAction.Release);

//		dump.ps.flush();
//		dump.ps.close();

	}

	public static void readReports(String[] args) throws Exception {
		DustStreamXmlAgentParser aXml = new DustStreamXmlAgentParser();

		MindHandle parser = Dust.resolveID(null, null);
		Dust.access(parser, MindAccess.Set, aXml, DUST_ATT_NATIVE_INSTANCE);
		Dust.access(parser, MindAccess.Set, parser, MIND_ATT_KNOWLEDGE_LISTENERS);
		Dust.access(parser, MindAccess.Commit, MindAction.Init);
//		aXml.agentExecAction(MindAction.Init);

		MindHandle target = Dust.resolveID(null, null);
		Dust.access(parser, MindAccess.Set, target, MISC_ATT_CONN_TARGET);

//		DustDevAgentDump processor = new DustDevAgentDump();
//		processor.prefix = "Commit dump";

//		DustDevAgentXmlWriter processor = new DustDevAgentXmlWriter();
//		processor.hTarget = target;
//		processor.ps = new PrintStream(new File(out, "xmlTest.txt"));

//		XbrlAgentReportToExcel processor = new XbrlAgentReportToExcel();
		XbrlReportAgentXhtmlReader processor = new XbrlReportAgentXhtmlReader();

		MindHandle listener = Dust.resolveID(null, null);
		Dust.access(listener, MindAccess.Set, processor, DUST_ATT_NATIVE_INSTANCE);
		Dust.access(listener, MindAccess.Set, "out/ReportExport", STREAM_ATT_STREAM_PATH);
		Dust.access(target, MindAccess.Set, listener, MIND_ATT_KNOWLEDGE_LISTENERS);

		for (String fn : args) {
//			long t = System.currentTimeMillis();
			File fIn = new File(fn);

//			String outName = fIn.getName();
//			int idx = outName.lastIndexOf(".");
//			outName = "out/" + outName.substring(0, idx + 1) + "xlsx";

//			processor.init(target, outName);

			Dust.access(parser, MindAccess.Set, fIn, STREAM_ATT_STREAM_FILE);
			Dust.access(parser, MindAccess.Commit, MindAction.Process);

//			aXml.agentExecAction(MindAction.Process);

//			File f = processor.save();
//
//			if ( null == f ) {
//				Dust.processor(" ", false, "No XBRL:", fn, "time:", System.currentTimeMillis() - t, "msec.");
//			} else {
//				Dust.processor(" ", false, "Result:", f.getName(), "input size:", fIn.length(), "bytes, time:", System.currentTimeMillis() - t, "msec.");
//			}
		}

		aXml.agentExecAction(MindAction.Release);

//		processor.ps.flush();
//		processor.ps.close();

	}

}
