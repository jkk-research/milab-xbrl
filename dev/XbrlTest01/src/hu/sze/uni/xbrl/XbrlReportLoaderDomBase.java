package hu.sze.uni.xbrl;

import java.io.File;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.xbrl.XbrlCoreUtils;
import hu.sze.milab.xbrl.XbrlConsts.XbrlFactDataInfo;
import hu.sze.milab.xbrl.XbrlConsts.XbrlFactDataType;

@SuppressWarnings("rawtypes")
public abstract class XbrlReportLoaderDomBase implements XbrlConsts {

	public static final String POSTFIX_TXT = "_Txt.csv";
	public static final String POSTFIX_VAL = "_Val.csv";

	enum XbrlElements {
		Context, Unit, Continuation, Footnote, DimCount, DefLang,
	}
	
	Map<XbrlFactDataInfo, String> cvtKeys = new HashMap<>();

	DocumentBuilderFactory dbf;
	SimpleDateFormat DFMT_ISO = new SimpleDateFormat(FMT_DATE);
	
	public XbrlReportLoaderDomBase() {
		cvtKeys.put(XbrlFactDataInfo.Scale, "scale");
		cvtKeys.put(XbrlFactDataInfo.Dec, "decimals");
		cvtKeys.put(XbrlFactDataInfo.Sign, "sign");
	}

	public void load(File f) throws Exception {
		Throwable loadErr = null;
		Map xbrlElements = new HashMap();

		try {
			DecimalFormat df = new DecimalFormat("#");
			df.setMaximumFractionDigits(8);

			Dust.dumpObs("Reading", f.getCanonicalPath());

			if ( null == dbf ) {
				dbf = DocumentBuilderFactory.newInstance();
			}

			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(f);

			int dimCount = 0;

			Element eHtml = doc.getDocumentElement();
			
			String defLang = eHtml.getAttribute("xml:lang");
			Dust.access(xbrlElements, MindAccess.Set, defLang, XbrlElements.DefLang);


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
					Dust.access(xbrlElements, MindAccess.Set, cd, XbrlElements.Context, ctxId);

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

								if ( dimIdx > dimCount ) {
									dimCount = dimIdx;
								}
							}
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

					Dust.access(xbrlElements, MindAccess.Set, val, XbrlElements.Unit, e.getAttribute("id"));
				}
					break;
				case "ix:continuation":
					Dust.access(xbrlElements, MindAccess.Set, e, XbrlElements.Continuation, e.getAttribute("id"));
					break;
				}
			}

			Dust.access(xbrlElements, MindAccess.Set, dimCount, XbrlElements.DimCount);
			Map<XbrlFactDataInfo, Object> data = new HashMap<>();

			for (int idx = 0; idx < nodeCount; ++idx) {
				Element e = (Element) nl.item(idx);
				String ctxId = e.getAttribute("contextRef");

				if ( !DustUtils.isEmpty(ctxId) ) {

					String valOrig = e.getTextContent().trim();
					String fmtCode = e.getAttribute("format");
					
					if ( DustUtils.isEmpty(fmtCode) ) {
						String tn = e.getTagName();
						
						if ( tn.contains("nonFraction") ) {
							fmtCode = "ixt4:num-dot-decimal";
						}
					}
					
					data.clear();

					if ( !DustUtils.isEmpty(fmtCode) ) {
						data.put(XbrlFactDataInfo.OrigValue, valOrig);
						data.put(XbrlFactDataInfo.Format, fmtCode);
						for ( Map.Entry<XbrlFactDataInfo, String> ce : cvtKeys.entrySet() ) {
							Object v = e.getAttribute(ce.getValue());
							if ( null != v ) {
								data.put(ce.getKey(), v);
							}
						}
						XbrlCoreUtils.convertValue(data);
						storeValue(xbrlElements, e, data);
					} else {
						Element txtFrag = e;
						boolean last = false;
						do {
							String contID = txtFrag.getAttribute("continuedAt");
							last = DustUtils.isEmpty(contID);
							data.put(XbrlFactDataInfo.OrigValue, valOrig);

							storeText(xbrlElements, e, data, txtFrag == e, last);

							if ( !last ) {
								txtFrag = Dust.access(xbrlElements, MindAccess.Peek, null, XbrlElements.Continuation, contID.trim());
								valOrig = txtFrag.getTextContent().trim();
							}
						} while (!last);
					}
				}
			}
		} catch (Throwable tl) {
			loadErr = tl;
		} finally {
			loadComplete(xbrlElements, loadErr);
			
			if ( null != loadErr ) {
				throw (Exception) loadErr;
			}
		}
	}

	protected abstract void storeText(Object xbrlElements, Element e, Map<XbrlFactDataInfo, Object> data, boolean first, boolean last) throws Exception;

	protected abstract void storeValue(Object xbrlElements, Element e, Map<XbrlFactDataInfo, Object> data) throws Exception;

	protected abstract void loadComplete(Object xbrlElements, Throwable err);

	private String getInfo(Element e, String tagName) {
		NodeList nl = e.getElementsByTagName(tagName);
		if ( 0 < nl.getLength() ) {
			String val = nl.item(0).getTextContent();
			if ( !DustUtils.isEmpty(val) ) {
				return val.trim();
			}
		}
		return null;
	}

	private boolean getInfo(Map<String, String> cd, Element e, String tagName) {
		String val = getInfo(e, tagName);
		if ( !DustUtils.isEmpty(val) ) {
			cd.put(tagName, val);
			return true;
		}
		return false;
	}

	public static void createSplitCsv(File xhtml, File targetDir, String fnPrefix, int textCut) throws Exception {
		XbrlReportLoaderDomBase loader = new XbrlReportLoaderDomBase() {
			PrintStream psVal;
			PrintStream psText;
			StringBuilder sbTxtFrag = new StringBuilder();
			int txtLen;

			PrintStream startLine(Object xbrlElements, Element e, boolean valStream) throws Exception {
				PrintStream ps = valStream ? psVal : psText;
				int dimCount = Dust.access(xbrlElements, MindAccess.Peek, 0, XbrlElements.DimCount);

				if ( null == ps ) {
					targetDir.mkdirs();
					ps = new PrintStream(new File(targetDir, fnPrefix + (valStream ? POSTFIX_VAL : POSTFIX_TXT)));
					
					if ( valStream ) {
						psVal = ps;
					} else {
						psText = ps;
					}

					writeFirstLine(valStream, ps, dimCount);
				}

				writeLineLead(xbrlElements, e, ps, dimCount);

				return ps;
			}

			public void writeLineLead(Object xbrlElements, Element e, PrintStream ps, int dimCount) {
				String ctxId = e.getAttribute("contextRef");
				Map<String, String> ctx = Dust.access(xbrlElements, MindAccess.Peek, null, XbrlElements.Context, ctxId.trim());
				if ( null == ctx ) {
					Dust.dumpObs("  Referred context not found", ctxId);
				}

				String tag = e.getAttribute("name");
				String[] tt = tag.split(":");
				StringBuilder sbLine = DustUtils.sbAppend(null, "\t", true, ctx.get("xbrli:entity"), tt[0], tt[1], ctx.get("xbrli:startDate"), ctx.get("xbrli:endDate"), ctx.get("xbrli:instant"));
				for (int i = 1; i <= dimCount; ++i) {
					DustUtils.sbAppend(sbLine, "\t", true, ctx.get("DimName_" + i), ctx.get("DimValue_" + i));
				}
				
				ps.print(sbLine);
			}

			public void writeFirstLine(boolean valStream, PrintStream ps, int dimCount) {
				ps.print("Entity\tTaxonomy\tConcept\tStartDate\tEndDate\tInstant");

				for (int i = 1; i <= dimCount; ++i) {
					ps.print("\tAxis_" + i + "\tDim_" + i);
				}

				ps.println(valStream ? "\tOrigValue\tUnit\tFormat\tSign\tDec\tScale\tType\tValue\tErr" : "\tLanguage\tValue");
			}

			@Override
			protected void storeText(Object xbrlElements, Element e, Map<XbrlFactDataInfo, Object> data, boolean first, boolean last) throws Exception {
				PrintStream ps = first ? startLine(xbrlElements, e, false) : psText;
				
				if ( first ) {
					String lang = e.getAttribute("xml:lang");
					if ( DustUtils.isEmpty(lang) ) {
						lang = Dust.access(xbrlElements, MindAccess.Set, null, XbrlElements.DefLang);
					}
					ps.print("\t" + lang + "\t\"");
					sbTxtFrag.setLength(0);
					txtLen = 0;
				} else {
					ps.print(" ");
				}

				String valStr = (String) data.get(XbrlFactDataInfo.OrigValue);
				String escapedLine = csvEscape(valStr, false);
				ps.print(escapedLine);
				
				int el = valStr.length();
				txtLen += el;
				
				int fl = sbTxtFrag.length();
				if ( fl < textCut ) {
					if ( 0 != fl ) {
						sbTxtFrag.append(" ");
						++fl;
					}
					int ac = textCut - fl;
					sbTxtFrag.append( (ac < el) ? valStr.substring(0, ac) : valStr );
				}
				
				if ( last ) {
					ps.println("\"");
					data.clear();
					String txt = sbTxtFrag.toString();
					data.put(XbrlFactDataInfo.OrigValue, txt);		

					if (txtLen < textCut) {
						data.put(XbrlFactDataInfo.Type, XbrlFactDataType.string);
						data.put(XbrlFactDataInfo.Value, csvEscape(txt, true));		
					} else {
						data.put(XbrlFactDataInfo.Type, XbrlFactDataType.text);
						data.put(XbrlFactDataInfo.Value, "Txt len: " + txtLen);			
					}
					
					storeValue(xbrlElements, e, data);
				}
			}

			public String csvEscape(String valStr, boolean addQuotes) {
				String ret = valStr.replace("\"", "\"\"").replaceAll("\\s+", " ");
				
				if ( addQuotes ) {
					ret = "\"" + ret + "\"";
				}
				
				return ret;
			}

			@Override
			protected void storeValue(Object xbrlElements, Element e, Map<XbrlFactDataInfo, Object> data) throws Exception {
				PrintStream ps = startLine(xbrlElements, e, true);

				String unit = "-";

				String unitId = e.getAttribute("unitRef");
				if ( !DustUtils.isEmpty(unitId) ) {
					unit = Dust.access(xbrlElements, MindAccess.Peek, null, XbrlElements.Unit, unitId.trim());
					if ( null == unit ) {
						Dust.dumpObs("  Referred unit not found", unitId);
						unit = "-";
					}
				}

//				String fmt = e.getAttribute("format");
//				String scale = e.getAttribute("scale");
//				String dec = e.getAttribute("decimals");
//				String sign = e.getAttribute("sign");
				
				String valOrig = (String) data.get(XbrlFactDataInfo.OrigValue);
				Object type = data.get(XbrlFactDataInfo.Type);
				Object value = data.get(XbrlFactDataInfo.Value);
				Object err = data.get(XbrlFactDataInfo.Err);
				
				Object fmt = data.get(XbrlFactDataInfo.Format);
				Object scale = data.get(XbrlFactDataInfo.Scale);
				Object dec = data.get(XbrlFactDataInfo.Dec);
				Object sign = data.get(XbrlFactDataInfo.Sign);
				
				if ( (null != value) && (null != type) ) {
					switch ( (XbrlFactDataType) type) {
					case date:
						value = DFMT_ISO.format((Date) value);
						break;
					case empty:
						value = "";
						break;
					case number:
						value = ((BigDecimal)value).toPlainString();
						break;
					default:
						break;
					
					}
				}
				
				StringBuilder sbData = DustUtils.sbAppend(null, "\t", true, csvEscape(valOrig, true), unit, fmt, sign, dec, scale, type, value, err);
				ps.println("\t" + sbData);
			}

			@Override
			protected void loadComplete(Object xbrlElements, Throwable err) {
				if ( null != psVal ) {
					psVal.flush();
					psVal.close();
				}
				if ( null != psText ) {
					psText.flush();
					psText.close();
				}
			}

		};

		loader.load(xhtml);
	}

}
