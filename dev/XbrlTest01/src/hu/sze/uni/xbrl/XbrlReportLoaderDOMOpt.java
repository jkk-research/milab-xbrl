package hu.sze.uni.xbrl;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.simple.parser.JSONParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.xbrl.XbrlCoreUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlReportLoaderDOMOpt implements XbrlConsts {

	DocumentBuilderFactory dbf;
	XbrlUtilsCounter cntFormats = new XbrlUtilsCounter(true);
	XbrlUtilsCounter cntTags = new XbrlUtilsCounter(true);
	XbrlUtilsCounter cntLang = new XbrlUtilsCounter(true);

	DecimalFormat df = new DecimalFormat("#");
	SimpleDateFormat fmtTimestamp = new SimpleDateFormat(FMT_TIMESTAMP);

	PrintWriter wData;
	PrintWriter wText;

	long startTS;
	int count;
	long totSize;

	int maxDimNum = 0;

	int dimCount;
	
	long cntNum = 0;
	long cntTxt = 0;

	public XbrlReportLoaderDOMOpt(int dimCount, String name) throws Exception {
		this.dimCount = dimCount;

		startTS = System.currentTimeMillis();

		String ts = fmtTimestamp.format(startTS);
		wData = new PrintWriter("work/" + name + "_" + ts + "_Data.csv");
		wText = new PrintWriter("work/" + name + "_" + ts + "_Text.csv");

		String colCommon = "File\tEntityId\tTagNamespace\tTagId\tStartDate\tEndDate\tInstant";

		wData.print(colCommon);
		wText.print(colCommon);

		for (int i = 1; i <= dimCount; ++i) {
			String colDim = "\tDimName_" + i + "\tDimValue_" + i;

			wData.print(colDim);
			wText.print(colDim);
		}

		wData.println("\tUnit\tOrigValue\tFormat\tSign\tDec\tScale\tRealValue");
		wText.println("\tLanguage\tFormat\tValue");

		wData.flush();
		wText.flush();
	}

	public void load(File f) throws Exception {
		long ts1 = System.currentTimeMillis();

		df.setMaximumFractionDigits(8);

		String fName = f.getName();
		Dust.dumpObs("Reading", fName);

		totSize += f.length();

		if ( 0 == (++count % 100) ) {
			Dust.dumpObs("-----\nPROGRESS so far", count, "size", totSize, "speed", totSize / (ts1 - startTS), "\n------");
		}

		Map<String, Map> facts = null;
		File fTest = new File("work/" + DustUtils.replacePostfix(fName, ".", "json"));
		if ( fTest.exists() ) {
			Map root = (Map) new JSONParser().parse(new FileReader(fTest));
			facts = (Map<String, Map>) root.get("facts");
		}

		if ( null == dbf ) {
			dbf = DocumentBuilderFactory.newInstance();
		}

		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(f);

		Element eHtml = doc.getDocumentElement();

		NamedNodeMap nnm = eHtml.getAttributes();
		for (int idx = 0; idx < nnm.getLength(); ++idx) {
			Attr a = (Attr) nnm.item(idx);
			if ( a.getName().startsWith("xmlns:") ) {
//				String aVal = a.getValue();
//				Dust.dumpObs("  Namespace", a.getName(), aVal);
			}
		}

		Map<String, Element> continuation = new TreeMap<>();
		Map<String, String> units = new TreeMap<>();
		Map<String, Map<String, String>> contexts = new TreeMap<>();

		NodeList nl = eHtml.getElementsByTagName("*");
		int nodeCount = nl.getLength();

		for (int idx = 0; idx < nodeCount; ++idx) {
			Element e = (Element) nl.item(idx);
			String tagName = e.getTagName();

			switch ( tagName ) {
			case "ix:references":
				break;
			case "xbrli:context": {
				Map<String, String> cd = new TreeMap<>();

				String ctxId = e.getAttribute("id");
				contexts.put(ctxId, cd);

				getInfo(cd, e, "xbrli:entity");
				getInfo(cd, e, "xbrli:startDate");
				getInfo(cd, e, "xbrli:endDate");
				getInfo(cd, e, "xbrli:instant");

				Element eS = (Element) e.getElementsByTagName("xbrli:scenario").item(0);
				if ( null != eS ) {
					NodeList nlS = eS.getChildNodes();
					int dimIdx = 0;
					int dc = nlS.getLength();

					for (int i2 = 0; i2 < dc; ++i2) {
						Node dn = nlS.item(i2);
						if ( dn instanceof Element ) {
							Element m = (Element) dn;
							String dim = m.getAttribute("dimension");
							String dVal = m.getTextContent().trim();
							++dimIdx;
							cd.put("DimName_" + dimIdx, dim);
							cd.put("DimValue_" + dimIdx, dVal);
						}
					}

					if ( dimIdx > maxDimNum ) {
						maxDimNum = dimIdx;
					}
				}
			}
				break;
			case "xbrli:unit": {
				String val = getInfo(e, "xbrli:unitNumerator");
				if ( null != val ) {
					String denom = getInfo(e, "xbrli:unitDenominator");
					val = val + "/" + denom;
				} else {
					val = getInfo(e, "xbrli:measure");
				}

				units.put(e.getAttribute("id"), val);
			}
				break;
			case "ix:continuation":
				continuation.put(e.getAttribute("id"), e);
				break;
			}
		}

		if ( contexts.isEmpty() ) {
			Dust.dumpObs("  EMPTY contexts???");
		}

		if ( units.isEmpty() ) {
			Dust.dumpObs("  EMPTY units???");
		}

		int fc = 0;
		int fmtCnt = 0;
		for (int idx = 0; idx < nodeCount; ++idx) {
			Element e = (Element) nl.item(idx);
			String tag = e.getAttribute("name");

			String factId = e.getAttribute("id");
			String ctxId = e.getAttribute("contextRef");

			if ( !DustUtils.isEmpty(ctxId) ) {
				String tagName = e.getTagName();
				cntTags.add(tagName);
				++fc;

				Map<String, String> ctx = contexts.get(ctxId.trim());
				if ( null == ctx ) {
					Dust.dumpObs("  Referred context not found", ctxId);
				}

				String unit = "-";

				String unitId = e.getAttribute("unitRef");
				if ( !DustUtils.isEmpty(unitId) ) {
					unit = units.get(unitId.trim());
					if ( null == unit ) {
						Dust.dumpObs("  Referred unit not found", unitId);
						unit = "-";
					}
				}

				String fmt = e.getAttribute("format");
				if ( !DustUtils.isEmpty(fmt) ) {
					++fmtCnt;
					cntFormats.add(fmt);
				}

				String val = e.getTextContent().trim();
				boolean text = false;
				PrintWriter w;

				String[] tt = tag.split(":");
				StringBuilder sbLine = DustUtils.sbAppend(null, "\t", true, fName, ctx.get("xbrli:entity"), tt[0], tt[1], ctx.get("xbrli:startDate"), ctx.get("xbrli:endDate"), ctx.get("xbrli:instant"));
				for (int i = 1; i <= dimCount; ++i) {
					DustUtils.sbAppend(sbLine, "\t", true, ctx.get("DimName_" + i), ctx.get("DimValue_" + i));
				}

				if ( tagName.contains("nonNumeric") ) {
					w = wText;

					String lang = e.getAttribute("xml:lang");
					if ( !DustUtils.isEmpty(lang) ) {
						cntLang.add(lang);
					}

					w.print(sbLine + "\t" + lang + "\t" + fmt + "\t\"" + val.replace("\"", "\"\"").replaceAll("\\s+", " "));

					Element txtFrag = e;
					for (String contID = txtFrag.getAttribute("continuedAt"); !DustUtils.isEmpty(contID); contID = txtFrag.getAttribute("continuedAt")) {
						txtFrag = continuation.get(contID);
						w.print(" " + txtFrag.getTextContent().trim().replace("\"", "\"\"").replaceAll("\\s+", " "));
					}

					w.println("\"");
					++cntTxt;
					text = true;
				} else {
					w = wData;

					String scale = e.getAttribute("scale");
					String dec = e.getAttribute("decimals");
					String sign = e.getAttribute("sign");

					Double dVal = XbrlCoreUtils.convertToDouble(val, fmt, scale, sign);

					String origVal = "\"" + val + "\"";
					val = df.format(dVal);
					if ( val.startsWith(".") ) {
						val = "0" + val;
					} else {
						val = val.replace("-.", "-0.");
					}

					StringBuilder sbData = DustUtils.sbAppend(null, "\t", true, unit, origVal, fmt, sign, dec, scale, val);
					w.println(sbLine + "\t" + sbData);
					++cntNum;
				}

				w.flush();

				if ( null != facts ) {
					Map tf = facts.remove(factId);

					if ( null == tf ) {
						Dust.dumpObs("  Test fact not found", factId);
					} else {
						Map dim = (Map) tf.get("dimensions");

						testMatch(dim, factId, "concept", tag);
						
						for (int i = 1; i <= dimCount; ++i) {
							testMatch(dim, factId, ctx.get("DimName_" + i), ctx.get("DimValue_" + i));
						}

						if ( text ) {

						} else {
							testMatch(tf, factId, "value", val);
						}
					}
				}
			}
		}

		if ( (null != facts) && (0 < facts.size()) ) {
			Dust.dumpObs("Test facts remained", facts.size());
			for (Map.Entry<String, Map> fe : facts.entrySet()) {
				Dust.dumpObs(fe.getKey(), fe.getValue());
			}
		}

		Dust.dumpObs("  All ix content", fc, "formats", fmtCnt, "process time", System.currentTimeMillis() - ts1);
	}



	public void testMatch(Map tf, String factId, String attId, String val) {
		if ( DustUtils.isEmpty(attId) || DustUtils.isEmpty(val) ) {
			return;
		}

		Object testVal = tf.get(attId);

		if ( !DustUtils.isEqual(val, testVal) ) {
			Dust.dumpObs("  Test mismatch", factId, attId, "local", val, "json", testVal);
		}
	}

	public String getInfo(Element e, String tagName) {
		NodeList nl = e.getElementsByTagName(tagName);
		if ( 0 < nl.getLength() ) {
			String val = nl.item(0).getTextContent();
			if ( !DustUtils.isEmpty(val) ) {
				return val.trim();
			}
		}
		return null;
	}

	public boolean getInfo(Map<String, String> cd, Element e, String tagName) {
		String val = getInfo(e, tagName);
		if ( !DustUtils.isEmpty(val) ) {
			cd.put(tagName, val);
			return true;
		}
		return false;
	}

	public void dump() {
		wData.close();
		wText.close();

		cntTags.dump("Tags");
		cntFormats.dump("Formats");
		cntLang.dump("Languages");
		
		Dust.dumpObs("COMPLETE files", count, "numRows", cntNum, "txtRows", cntTxt, "maxCtxDim", maxDimNum, "size", totSize, "speed", totSize / (System.currentTimeMillis() - startTS));
	}
}
