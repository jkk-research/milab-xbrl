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
import hu.sze.milab.dust.brain.DustImpl;
import hu.sze.milab.dust.stream.DustStreamUrlCache;
import hu.sze.milab.dust.stream.xml.DustStreamXmlAgent;
import hu.sze.milab.dust.stream.xml.DustStreamXmlLoader;
import hu.sze.milab.xbrl.XbrlConsts;

public class XbrlTest02 implements XbrlConsts {

	private static File dataDir;
	private static File out;
	private static File in;

	public static void main(String[] args) throws Exception {
		DustImpl.main(args);

		String data = System.getProperty("user.home") + "/work/xbrl/data";
		dataDir = new File(data);

		out = new File("out");
		out.mkdirs();

		in = new File("in");
		in.mkdirs();

//		readReports(args);
		readTaxonomy(new String[] { 
				"EFRAG-ESRS-2022-PoC-Taxonomy", 
				"IFRSAT-2022-03-24", 
				});
	}

	public static void readTaxonomy(String[] args) throws Exception {
		DustStreamUrlCache c = new DustStreamUrlCache(new File(dataDir, "urlCache"), false);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

//		XbrlTaxonomyToExcel toExcel = new XbrlTaxonomyToExcel();
		
		for (String taxRoot : args) {
			Dust.dumpObs("Reading taxonomy", taxRoot);
			
			File fRoot = new File(in, taxRoot);
			File txMeta = new File(fRoot, "META-INF");
			
			XbrlTaxonomyLoader taxonomyCollector = new XbrlTaxonomyLoader();
			taxonomyCollector.setRootFolder(txMeta.getParentFile());

			File fCat = new File(txMeta, "catalog.xml");
			Document catalog = db.parse(fCat);
			File fTaxPack = new File(txMeta, "taxonomyPackage.xml");
			Element taxPack = db.parse(fTaxPack).getDocumentElement();
			
			taxonomyCollector.setSeen(fCat, fTaxPack);


			Map<String, String> uriRewrite = new TreeMap<>();
			NodeList nl = catalog.getElementsByTagName("rewriteURI");
			for (int ni = nl.getLength(); ni-- > 0;) {
				NamedNodeMap atts = nl.item(ni).getAttributes();
				uriRewrite.put(atts.getNamedItem("uriStartString").getNodeValue(), atts.getNamedItem("rewritePrefix").getNodeValue());
			}
			
			DustStreamXmlLoader xmlLoader = new DustStreamXmlLoader(c);

			nl = taxPack.getElementsByTagName("tp:entryPointDocument");
			for (int ni = 0; ni < nl.getLength(); ++ni ) {
				String url = nl.item(ni).getAttributes().getNamedItem("href").getNodeValue();
				xmlLoader.loadNamespace(txMeta, url, taxonomyCollector, uriRewrite);
			}
			
			taxonomyCollector.dump();

//			toExcel.save(out, taxRoot);
		}
	}

	public static void readReports(String[] args) throws Exception {
		DustStreamXmlAgent aXml = new DustStreamXmlAgent();

		aXml.agentExecAction(MindAction.Init);

		MindHandle target = Dust.createHandle();
		Dust.access(MIND_ATT_AGENT_SELF, MindAccess.Set, target, MISC_ATT_CONN_TARGET);

//		DustDevAgentDump dump = new DustDevAgentDump();
//		dump.prefix = "Commit dump";

//		DustDevAgentXmlWriter dump = new DustDevAgentXmlWriter();
//		dump.hTarget = target;
//		dump.ps = new PrintStream(new File(out, "xmlTest.txt"));

		XbrlAgentReportToExcel dump = new XbrlAgentReportToExcel();

		MindHandle listener = Dust.createHandle();
		Dust.access(listener, MindAccess.Set, dump, BRAIN_ATT_ACTOR_INSTANCE);
		Dust.access(target, MindAccess.Set, listener, MIND_ATT_KNOWLEDGE_LISTENERS);

		for (String fn : args) {
			long t = System.currentTimeMillis();
			File fIn = new File(dataDir, fn);

			String outName = fIn.getName();
			int idx = outName.lastIndexOf(".");
			outName = "out/" + outName.substring(0, idx + 1) + "xlsx";

			dump.init(target, outName);

			Dust.access(MIND_ATT_AGENT_SELF, MindAccess.Set, fIn, STREAM_ATT_STREAM_FILE);
			aXml.agentExecAction(MindAction.Process);

			File f = dump.save();

			if ( null == f ) {
				Dust.dump(" ", false, "No XBRL:", fn, "time:", System.currentTimeMillis() - t, "msec.");
			} else {
				Dust.dump(" ", false, "Result:", f.getName(), "input size:", fIn.length(), "bytes, time:", System.currentTimeMillis() - t, "msec.");
			}
		}

		aXml.agentExecAction(MindAction.Release);

//		dump.ps.flush();
//		dump.ps.close();

	}

}
