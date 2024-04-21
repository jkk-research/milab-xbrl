package hu.sze.uni.xbrl.parser;

import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.stream.xml.DustXmlUtils;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsNarrative;

public class XbrlParserXmlAgent extends DustAgent implements XbrlParserConsts {
	
	DustUtilsNarrative.Counter cntFormats = new DustUtilsNarrative.Counter(true);
	DustUtilsNarrative.Counter cntTags = new DustUtilsNarrative.Counter(true);
	DustUtilsNarrative.Counter cntLang = new DustUtilsNarrative.Counter(true);

	@Override
	protected MindHandle agentBegin() throws Exception {		

		return MIND_TAG_RESULT_READACCEPT;
	}

	@Override
	protected MindHandle agentProcess() throws Exception {
		MindHandle hDataWriter = null;
		MindHandle hTextWriter = null;

		try {
			Document doc = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, DUST_ATT_IMPL_DATA);
			
			SimpleDateFormat fmtTimestamp = new SimpleDateFormat(DUST_FMT_TIMESTAMP);

			PrintWriter wData;
			PrintWriter wText;

			String filePath = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, RESOURCE_ATT_URL_PATH);
			String fileName = DustUtils.getPostfix(filePath, File.separator);
			filePath = DustUtils.cutPostfix(filePath, ".");
			
			long tsl = System.currentTimeMillis();
			String ts = fmtTimestamp.format(tsl);
			hDataWriter = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, XBRLDOCK_ATT_XMLLOADER_ROWDATA);
			wData = createOutStream(hDataWriter, filePath, ts, "_Data.csv");
			wData.println("File,EntityId,StartDate,EndDate,Instant,DimName_1,DimValue_1,DimName_2,DimValue_2,TagNamespace,TagId,Unit,OrigValue,Format,Sign,Dec,Scale,RealValue");
			wData.flush();

			hTextWriter = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, XBRLDOCK_ATT_XMLLOADER_ROWTEXT);
			wText = createOutStream(hTextWriter, filePath, ts, "_Text.csv");
			wText.println("File,EntityId,StartDate,EndDate,Instant,DimName_1,DimValue_1,DimName_2,DimValue_2,TagNamespace,TagId,Language,Format,Value");
			wText.flush();

			Element eHtml = doc.getDocumentElement();

			NodeList nl;

			NamedNodeMap nnm = eHtml.getAttributes();

			DecimalFormat df = new DecimalFormat("#");
			df.setMaximumFractionDigits(8);
			
			DustUtilsNarrative.Indexer<String> dims = new DustUtilsNarrative.Indexer<>();
			int maxDimNum = 0;

			Map<String, Element> continuation = new TreeMap<>();
			Map<String, String> units = new TreeMap<>();
			Map<String, Map<String, String>> contexts = new TreeMap<>();

			for (int idx = 0; idx < nnm.getLength(); ++idx) {
				Attr a = (Attr) nnm.item(idx);
				if ( a.getName().startsWith("xmlns:") ) {
					String aVal = a.getValue();
					Dust.log(EVENT_TAG_TYPE_TRACE, "  Namespace", a.getName(), aVal);
				}
			}
			
			

			Element eRefs = (Element) eHtml.getElementsByTagName("ix:references").item(0);
			Dust.log(EVENT_TAG_TYPE_TRACE, "  References", eRefs.getChildNodes().getLength());

			Element eRes = (Element) eHtml.getElementsByTagName("ix:resources").item(0);
			nl = eRes.getElementsByTagName("xbrli:context");
			for (int idx = 0; idx < nl.getLength(); ++idx) {
				Element e = (Element) nl.item(idx);

				Map<String, String> cd = new TreeMap<>();

				String ctxId = e.getAttribute("id");
				contexts.put(ctxId, cd);

				DustXmlUtils.optLoadInfo(cd, e, "xbrli:entity");
				DustXmlUtils.optLoadInfo(cd, e, "xbrli:startDate");
				DustXmlUtils.optLoadInfo(cd, e, "xbrli:endDate");
				DustXmlUtils.optLoadInfo(cd, e, "xbrli:instant");

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
				Dust.log(EVENT_TAG_TYPE_TRACE, "  EMPTY contexts???");
			} else {
				Dust.log(EVENT_TAG_TYPE_TRACE, "  Contexts", contexts.size());
			}

			nl = eRes.getElementsByTagName("xbrli:unit");
			for (int idx = 0; idx < nl.getLength(); ++idx) {
				Element e = (Element) nl.item(idx);

				String val = DustXmlUtils.getInfo(e, "xbrli:unitNumerator");
				if ( null != val ) {
					String denom = DustXmlUtils.getInfo(e, "xbrli:unitDenominator");
					val = val + "/" + denom;
				} else {
					val = DustXmlUtils.getInfo(e, "xbrli:measure");
				}

				units.put(e.getAttribute("id"), val);
			}

			if ( units.isEmpty() ) {
				Dust.log(EVENT_TAG_TYPE_TRACE, "  EMPTY units???");
			} else {
				Dust.log(EVENT_TAG_TYPE_TRACE, "  Units", units.size());
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
						Dust.log(EVENT_TAG_TYPE_TRACE, "  Referred context not found", ctxId);
					}

					String unit = "-";

					String unitId = e.getAttribute("unitRef");
					if ( !DustUtils.isEmpty(unitId) ) {
						unit = units.get(unitId.trim());
						if ( null == unit ) {
							Dust.log(EVENT_TAG_TYPE_TRACE, "  Referred unit not found", unitId);
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
						
//						String val2 = e.toString().trim();

						Element txtFrag = e;
						for (String contID = txtFrag.getAttribute("continuedAt"); !DustUtils.isEmpty(contID); contID = txtFrag.getAttribute("continuedAt")) {
							txtFrag = continuation.get(contID);
							val = val + " " + txtFrag.getTextContent().trim();
//							val2 = val2 + " " + txtFrag.toString().trim();
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
					
					StringBuilder sbLine = DustUtils.sbAppend(null, ",", true, fileName, ctx.get("xbrli:entity"), ctx.get("xbrli:startDate"), ctx.get("xbrli:endDate"), ctx.get("xbrli:instant"), da1,
							dv1, da2, dv2, tt[0], tt[1]);

					w.println(sbLine + "," + sbData);
					w.flush();

				}
			}
		} catch (Throwable t) {
			DustException.swallow(t, "reading xml", Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, RESOURCE_ATT_URL_PATH));
			return MIND_TAG_RESULT_REJECT;
		} finally {
			if ( null != hDataWriter ) {
				Dust.access(MindAccess.Commit, MIND_TAG_ACTION_END, hDataWriter, RESOURCE_ATT_PROCESSOR_STREAM);
			}
			if ( null != hTextWriter ) {
				Dust.access(MindAccess.Commit, MIND_TAG_ACTION_END, hTextWriter, RESOURCE_ATT_PROCESSOR_STREAM);
			}
		}

		return MIND_TAG_RESULT_ACCEPT;
	}

	public PrintWriter createOutStream(MindHandle hWriter, String filePath, String ts, String postfix) {
		Dust.access(MindAccess.Set, filePath + "_" + ts + postfix, hWriter, RESOURCE_ATT_PROCESSOR_STREAM, RESOURCE_ATT_URL_PATH);
		Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, hWriter, RESOURCE_ATT_PROCESSOR_STREAM);
		return Dust.access(MindAccess.Peek, null, hWriter, RESOURCE_ATT_PROCESSOR_STREAM, DUST_ATT_IMPL_DATA);
	}
	
	@Override
	protected MindHandle agentEnd() throws Exception {
		return MIND_TAG_RESULT_ACCEPT;
	}
}
