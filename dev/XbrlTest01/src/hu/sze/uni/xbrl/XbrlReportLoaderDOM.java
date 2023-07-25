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
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.utils.DustUtils;

public class XbrlReportLoaderDOM implements XbrlConsts {

	DocumentBuilderFactory dbf;
	XbrlUtilsCounter cntFormats = new XbrlUtilsCounter(true);
	XbrlUtilsCounter cntTags = new XbrlUtilsCounter(true);
	XbrlUtilsCounter cntLang = new XbrlUtilsCounter(true);

	DecimalFormat df = new DecimalFormat("#");
	SimpleDateFormat fmtTimestamp = new SimpleDateFormat(FMT_TIMESTAMP);

	PrintWriter wData;
	PrintWriter wText;

	public XbrlReportLoaderDOM(String name) throws Exception {
		long tsl = System.currentTimeMillis();
		String ts = fmtTimestamp.format(tsl);
		wData = new PrintWriter("work/" + name + "_" + ts + "_Data.csv");
		wData.println("File,EntityId,StartDate,EndDate,Instant,DimName_1,DimValue_1,DimName_2,DimValue_2,TagNamespace,TagId,Unit,OrigValue,Format,Sign,Dec,Scale,RealValue");
		wData.flush();
		wText = new PrintWriter("work/" + name + "_" + ts + "_Text.csv");
		wText.println("File,EntityId,StartDate,EndDate,Instant,DimName_1,DimValue_1,DimName_2,DimValue_2,TagNamespace,TagId,Language,Format,Value");
		wText.flush();
	}

	public void load(File f) throws Exception {

		int maxDimNum = 0;

		df.setMaximumFractionDigits(8);

		String fName = f.getName();
		Dust.dumpObs("Reading", fName);

		File fTest = new File("work/" + DustUtils.replacePostfix(fName, ".", "json"));

		Map<String, Map> facts = null;

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

		NodeList nl;

		NamedNodeMap nnm = eHtml.getAttributes();

		DustUtils.Indexer<String> dims = new DustUtils.Indexer<>();

		Map<String, Element> continuation = new TreeMap<>();
		Map<String, String> units = new TreeMap<>();
		Map<String, Map<String, String>> contexts = new TreeMap<>();

		for (int idx = 0; idx < nnm.getLength(); ++idx) {
			Attr a = (Attr) nnm.item(idx);
			if ( a.getName().startsWith("xmlns:") ) {
				String aVal = a.getValue();
				Dust.dumpObs("  Namespace", a.getName(), aVal);
			}
		}

		Element eRefs = (Element) eHtml.getElementsByTagName("ix:references").item(0);
		Dust.dumpObs("  References", eRefs.getChildNodes().getLength());

		Element eRes = (Element) eHtml.getElementsByTagName("ix:resources").item(0);
		nl = eRes.getElementsByTagName("xbrli:context");
		for (int idx = 0; idx < nl.getLength(); ++idx) {
			Element e = (Element) nl.item(idx);

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
				int dc = nlS.getLength();
				if ( dc > maxDimNum ) {
					maxDimNum = dc;
				}

				for (int i2 = 0; i2 < nlS.getLength(); ++i2) {
					Element m = (Element) nlS.item(i2);
					String dim = m.getAttribute("dimension");
					String dVal = m.getTextContent().trim();

					@SuppressWarnings("unused")
					int i = dims.getIndex(dim);

					cd.put("dimAxis_" + i2, dim);
					cd.put("dimVal_" + i2, dVal);
				}
			}
		}

		if ( contexts.isEmpty() ) {
			Dust.dumpObs("  EMPTY contexts???");
		} else {
			Dust.dumpObs("  Contexts", contexts.size());
		}

		nl = eRes.getElementsByTagName("xbrli:unit");
		for (int idx = 0; idx < nl.getLength(); ++idx) {
			Element e = (Element) nl.item(idx);

			String val = getInfo(e, "xbrli:unitNumerator");
			if ( null != val ) {
				String denom = getInfo(e, "xbrli:unitDenominator");
				val = val + "/" + denom;
			} else {
				val = getInfo(e, "xbrli:measure");
			}

			units.put(e.getAttribute("id"), val);
		}

		if ( units.isEmpty() ) {
			Dust.dumpObs("  EMPTY units???");
		} else {
			Dust.dumpObs("  Units", units.size());
		}

		nl = eHtml.getElementsByTagName("ix:continuation");
		for (int idx = 0; idx < nl.getLength(); ++idx) {
			Element e = (Element) nl.item(idx);
			continuation.put(e.getAttribute("id"), e);
		}

		nl = eHtml.getElementsByTagName("*");
		int fc = 0;
		int fmtCnt = 0;
		for (int idx = 0; idx < nl.getLength(); ++idx) {
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

				PrintWriter w;

				StringBuilder sbData;
				boolean text = false;

				if ( tagName.contains("nonNumeric") ) {
					w = wText;
					String lang = e.getAttribute("xml:lang");
					if ( !DustUtils.isEmpty(lang) ) {
						cntLang.add(lang);
					}
					
//					String val2 = e.toString().trim();

					Element txtFrag = e;
					for (String contID = txtFrag.getAttribute("continuedAt"); !DustUtils.isEmpty(contID); contID = txtFrag.getAttribute("continuedAt")) {
						txtFrag = continuation.get(contID);
						val = val + " " + txtFrag.getTextContent().trim();
//						val2 = val2 + " " + txtFrag.toString().trim();
					}

					sbData = DustUtils.sbAppend(null, ",", true, lang, fmt, "\"" + val.replace("\"", "\"\"").replace("\n", " ") + "\"");
					text = true;
				} else {
					w = wData;
					char decSep = '.';
					Double dVal = null;

					String scale = e.getAttribute("scale");
					String dec = e.getAttribute("decimals");
					String sign = e.getAttribute("sign");

					if ( fmt.contains("zero") ) {
						dVal = 0.0;
					} else {
						if ( fmt.contains("comma") ) {
							decSep = ',';
						}

						StringBuilder sbVal = new StringBuilder();

						for (int i = 0; i < val.length(); ++i) {
							char c = val.charAt(i);

							if ( Character.isDigit(c) || (('-' == c) && (0 == i)) || (c == decSep) ) {
								sbVal.append(c);
							}
						}

						dVal = Double.valueOf(sbVal.toString());

						if ( !DustUtils.isEmpty(scale) ) {
							dVal *= Math.pow(10, Double.valueOf(scale));
						}

						if ( "-".equals(sign) ) {
							dVal = -dVal;
						}
					}

					String origVal = "\"" + val + "\"";
					val = df.format(dVal);
					if ( val.startsWith(".") ) {
						val = "0" + val;
					} else {
						val = val.replace("-.", "-0.");
					}
					sbData = DustUtils.sbAppend(null, ",", true, unit, origVal, fmt, sign, dec, scale, val);
				}

				// name id contextRef identifier instant startDate endDate
				// ComponentsOfEquityAxis unitRef VALUE scale decimals format NumValue
				String[] tt = tag.split(":");
				String da1 = ctx.get("dimAxis_0");
				String dv1 = ctx.get("dimVal_0");
				String da2 = ctx.get("dimAxis_1");
				String dv2 = ctx.get("dimVal_1");
				
				StringBuilder sbLine = DustUtils.sbAppend(null, ",", true, fName, ctx.get("xbrli:entity"), ctx.get("xbrli:startDate"), ctx.get("xbrli:endDate"), ctx.get("xbrli:instant"), da1,
						dv1, da2, dv2, tt[0], tt[1]);

				w.println(sbLine + "," + sbData);
				w.flush();

				if ( null != facts ) {
					Map tf = facts.remove(factId);

					if ( null == tf ) {
						Dust.dumpObs("  Test fact not found", factId);
					} else {
						Map dim = (Map) tf.get("dimensions");
						
						testMatch(dim, factId, "concept", tag);
						testMatch(dim, factId, da1, dv1);
						testMatch(dim, factId, da2, dv2);

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
			for ( Map.Entry<String, Map> fe : facts.entrySet()) {
				Dust.dumpObs(fe.getKey(), fe.getValue());
			}
		}

		Dust.dumpObs("  All ix content", fc, "formats", fmtCnt, "dimensions", dims, "max dim num in context", maxDimNum);
	}

	public void testMatch(Map tf, String factId, String attId, String val) {
		if ( DustUtils.isEmpty(attId) || DustUtils.isEmpty(val)) {
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
				return val;
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
	}
}
