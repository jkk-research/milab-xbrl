package hu.sze.milab.xbrl.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.brain.DustImpl;
import hu.sze.milab.dust.stream.DustStreamUrlCache;
import hu.sze.milab.dust.stream.DustStreamXmlAgent;
import hu.sze.milab.dust.stream.DustStreamXmlLoader;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.xbrl.XbrlConsts;

public class XbrlTest02 implements XbrlConsts {

	private static File dataDir;
	private static File out;
	private static File in;

	private static class TaxonomyToExcel implements DustStreamXmlLoader.NamespaceProcessor {
		DustUtils.Indexer<String> attCols = new DustUtils.Indexer<String>();
		private static final String[] KNOWN_ATT_COLS = {"name", "id", "type", "substitutionGroup", "xbrli:periodType", "abstract"};

		private Workbook wb;

		private int colCount;
		
		public TaxonomyToExcel() {
			for ( String a : KNOWN_ATT_COLS ) {
				attCols.getIndex(a);
			}
		}

		@Override
		public void namespaceLoaded(Element root) {
			if ( null == wb ) {
				wb = new XSSFWorkbook();
				colCount = attCols.getSize();
			}

			int rc = 0;

			Sheet sheet = null;
			Row row;
			Cell c;

			NodeList el = root.getElementsByTagName("xsd:element");
			int ec = el.getLength();
			for (int ei = 0; ei < ec; ++ei) {
				NamedNodeMap atts = el.item(ei).getAttributes();

				if ( null != atts ) {
					if ( null == sheet ) {
						String nsOrig = root.getAttribute("targetNamespace");
						String ns = nsOrig;
						int sep = ns.indexOf("://");
						if ( -1 != sep ) {
							ns = ns.substring(sep + 3);
						}
						ns = ns.replace("/", "_");
						
						int nl = ns.length();
						if ( 31 <= nl ) {
							ns = ns.substring(nl - 31, nl);
//							sep = ns.indexOf("_");
//							String s1 = ns.substring(0, sep);
//							String s2 = ns.substring(nl - 31 + sep + 2, nl);
//							ns = s1 + ".." + s2;
//							nl = ns.length();
						}
						
						sheet = wb.getSheet(ns);
						
						if ( null != sheet ) {
							Dust.dumpObs("  Skipping duplicated sheet", ns, "for namespace", nsOrig);
							return;
						}

						Dust.dumpObs("  Creating sheet", ns, "for namespace", nsOrig);
						sheet = wb.createSheet(ns);

						row = sheet.createRow(rc++);
						for (String k : attCols.keys()) {
							c = row.createCell(attCols.getIndex(k));
							c.setCellValue(k);
						}
					}

					row = sheet.createRow(rc++);
					int ac = atts.getLength();
					for (int ai = 0; ai < ac; ++ai) {
						Node a = atts.item(ai);
						String an = a.getNodeName();

						int ax = attCols.getIndex(an);

						if ( ax >= colCount ) {
							addAttCol(an);
							colCount = ax + 1;
						}

						c = row.createCell(ax);
						c.setCellValue(a.getNodeValue());
					}
				}
			}
		}

		private void addAttCol(String an) {
			for (Iterator<Sheet> it = wb.sheetIterator(); it.hasNext();) {
				Cell c = it.next().getRow(0).createCell(colCount);
				c.setCellValue(an);
			}
		}

		public void save(String taxName) throws Exception {
			File f = null;

			if ( null != wb ) {
				f = new File(out, taxName + ".xlsx");
				FileOutputStream fileOut = new FileOutputStream(f);

				wb.write(fileOut);
				fileOut.flush();
				fileOut.close();

				wb.close();

				wb = null;
			}
		}
	};

	public static void main(String[] args) throws Exception {
		DustImpl.main(args);

		String data = System.getProperty("user.home") + "/work/xbrl/data";
		dataDir = new File(data);

		out = new File("out");
		out.mkdirs();

		in = new File("in");
		in.mkdirs();

//		readReports(args);
		readTaxonomy(new String[] { "EFRAG-ESRS-2022-PoC-Taxonomy", "IFRSAT-2022-03-24", });
	}

	public static void readTaxonomy(String[] args) throws Exception {
		DustStreamUrlCache c = new DustStreamUrlCache(new File(dataDir, "urlCache"), false);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

		TaxonomyToExcel toExcel = new TaxonomyToExcel();

		for (String taxRoot : args) {
			Dust.dumpObs("Reading taxonomy", taxRoot);
			File txMeta = new File(in, taxRoot + "/META-INF");

			Document catalog = db.parse(new File(txMeta, "catalog.xml"));
			Element taxPack = db.parse(new File(txMeta, "taxonomyPackage.xml")).getDocumentElement();

			Map<String, String> rewrite = new TreeMap<>();
			NodeList nl = catalog.getElementsByTagName("rewriteURI");
			for (int ni = nl.getLength(); ni-- > 0;) {
				NamedNodeMap atts = nl.item(ni).getAttributes();
				rewrite.put(atts.getNamedItem("uriStartString").getNodeValue(), atts.getNamedItem("rewritePrefix").getNodeValue());
			}

			DustStreamXmlLoader xLoader = new DustStreamXmlLoader(c);

			nl = taxPack.getElementsByTagName("tp:entryPointDocument");
			for (int ni = 0; ni < nl.getLength(); ++ni ) {
				String url = nl.item(ni).getAttributes().getNamedItem("href").getNodeValue();
				xLoader.loadNamespace(txMeta, url, toExcel, rewrite);
			}

			toExcel.save(taxRoot);
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
