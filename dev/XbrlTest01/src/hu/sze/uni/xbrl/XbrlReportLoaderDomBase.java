package hu.sze.uni.xbrl;

import java.io.File;
import java.io.PrintStream;
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

@SuppressWarnings("rawtypes")
public abstract class XbrlReportLoaderDomBase implements XbrlConsts {

	public static final String POSTFIX_TXT = "_Txt.csv";
	public static final String POSTFIX_VAL = "_Val.csv";

	enum XbrlElements {
		Context, Unit, Continuation, Footnote, DimCount, DefLang,
	}

	DocumentBuilderFactory dbf;
	SimpleDateFormat DFMT_ISO = new SimpleDateFormat(FMT_DATE);

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

			for (int idx = 0; idx < nodeCount; ++idx) {
				Element e = (Element) nl.item(idx);
				String ctxId = e.getAttribute("contextRef");

				if ( !DustUtils.isEmpty(ctxId) ) {

					String valOrig = e.getTextContent().trim();
					String valErr = null;
					String valStr = null;
					Object valObj = null;
					String fmt = e.getAttribute("format");
					
					if ( DustUtils.isEmpty(fmt) ) {
						String tn = e.getTagName();
						
						if ( tn.contains("nonFraction") ) {
							fmt = "ixt4:num-dot-decimal";
						}
					}

					String fmtCode = "";

					if ( !DustUtils.isEmpty(fmt) ) {
						int sep = fmt.indexOf(":");
						fmtCode = fmt.substring(sep + 1);
					}

					boolean storeValue = true;

					if ( fmtCode.startsWith("num") ) {
						String scale = e.getAttribute("scale");
						String sign = e.getAttribute("sign");

						try {
							Double dVal = XbrlCoreUtils.convertToDouble(valOrig, fmt, scale, sign);
							valObj = dVal;

							valStr = df.format(dVal);
							if ( valStr.startsWith(".") ) {
								valStr = "0" + valStr;
							} else {
								valStr = valStr.replace("-.", "-0.");
							}
						} catch (Throwable t) {
							valErr = t.toString();
						}
					} else if ( fmtCode.startsWith("fixed") ) {
						String pf = DustUtils.getPostfix(fmtCode, "-").toLowerCase();
						switch ( pf ) {
						case "zero":
							valObj = 0.0;
							valStr = "0";
							break;
						case "empty":
							valObj = null;
							valStr = "";
							break;
						case "false":
							valObj = Boolean.FALSE;
							valStr = "false";
							break;
						case "true":
							valObj = Boolean.TRUE;
							valStr = "true";
							break;
						}
					} else if ( fmtCode.startsWith("date") ) {
						String dfmt = null;

						if ( fmtCode.contains("monthname") ) {
							valErr = "No converter for " + fmtCode;
						} else {
							switch ( fmtCode ) {
							case "date-day-month-year":
								dfmt = "dd-MM-yyyy";
								break;
							case "date-month-day-year":
								dfmt = "MM-dd-yyyy";
								break;
							case "date-year-month-day":
								dfmt = "yyyy-MM-dd";
								break;
							}
						}

						if ( null != dfmt ) {
							SimpleDateFormat fmtDate = new SimpleDateFormat(dfmt);
							try {
								Date d = fmtDate.parse(valOrig);
								valObj = d;

								valStr = DFMT_ISO.format(d);
							} catch (Throwable t) {
								valErr = t.toString();
							}
						}
					} else {
						storeValue = false;

						Element txtFrag = e;
						boolean last = false;
						do {
							String contID = txtFrag.getAttribute("continuedAt");
							last = DustUtils.isEmpty(contID);

							storeText(xbrlElements, e, valOrig, txtFrag == e, last);

							if ( !last ) {
								txtFrag = Dust.access(xbrlElements, MindAccess.Peek, null, XbrlElements.Continuation, contID.trim());
								valOrig = txtFrag.getTextContent().trim();
							}
						} while (!last);
					}

					if ( storeValue ) {
						storeValue(xbrlElements, e, valOrig, valStr, valObj, valErr);
					}
				}
			}
		} catch (Throwable tl) {
			loadErr = tl;
		} finally {
			loadComplete(xbrlElements, loadErr);
		}
	}

	protected abstract void storeText(Object xbrlElements, Element e, String valStr, boolean first, boolean last) throws Exception;

	protected abstract void storeValue(Object xbrlElements, Element e, String valOrig, String valStr, Object valObj, String valErr) throws Exception;

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

				ps.println(valStream ? "\tOrigValue\tUnit\tFormat\tSign\tDec\tScale\tValue\tErr" : "\tLanguage\tValue");
			}

			@Override
			protected void storeText(Object xbrlElements, Element e, String valStr, boolean first, boolean last) throws Exception {
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
					storeValue(xbrlElements, e, sbTxtFrag.toString(), "Txt len: " + txtLen, null, "");
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
			protected void storeValue(Object xbrlElements, Element e, String valOrig, String valStr, Object valObj, String valErr) throws Exception {
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

				String fmt = e.getAttribute("format");
				String scale = e.getAttribute("scale");
				String dec = e.getAttribute("decimals");
				String sign = e.getAttribute("sign");

				StringBuilder sbData = DustUtils.sbAppend(null, "\t", true, csvEscape(valOrig, true), unit, fmt, sign, dec, scale, valStr, valErr);
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
