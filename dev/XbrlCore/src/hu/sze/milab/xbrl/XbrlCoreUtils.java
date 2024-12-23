package hu.sze.milab.xbrl;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.stream.DustStreamUrlCache;
import hu.sze.milab.dust.stream.xml.DustStreamXmlDocumentGraphLoader;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.xbrl.test.XbrlTaxonomyLoader;

public class XbrlCoreUtils implements XbrlConsts {

	public static boolean convertValue(Map<XbrlFactDataInfo, Object> data) {
		String error = null;
		XbrlFactDataType ft = XbrlFactDataType.empty;
		Object valObj = null;

		try {
			String valOrig = (String) data.get(XbrlFactDataInfo.OrigValue);

			String fmtCode = (String) data.get(XbrlFactDataInfo.Format);
			fmtCode = DustUtils.getPostfix(fmtCode, ":");

			if ( fmtCode.startsWith("num") ) {
				ft = XbrlFactDataType.number;
				String scale = (String) data.get(XbrlFactDataInfo.Scale);
				String decimals = (String) data.get(XbrlFactDataInfo.Dec);
				String sign = (String) data.get(XbrlFactDataInfo.Sign);

				valObj = convertToNumber(valOrig, fmtCode, scale, decimals, sign);
			} else if ( fmtCode.startsWith("fixed") ) {
				String pf = DustUtils.getPostfix(fmtCode, "-").toLowerCase();
				switch ( pf ) {
				case "zero":
					valObj = BigDecimal.ZERO;
					ft = XbrlFactDataType.number;
					break;
				case "empty":
					ft = XbrlFactDataType.empty;
					break;
				case "false":
					valObj = Boolean.FALSE;
					ft = XbrlFactDataType.bool;
					break;
				case "true":
					valObj = Boolean.TRUE;
					ft = XbrlFactDataType.bool;
					break;
				}
			} else if ( fmtCode.startsWith("date") ) {
				ft = XbrlFactDataType.date;
				valObj = convertToDate(valOrig, fmtCode);
			}

		} catch (Throwable e) {
			error = "Conversion exception " + e.toString();
		}

		data.put(XbrlFactDataInfo.Value, valObj);
		data.put(XbrlFactDataInfo.Type, ft);
		if ( null != error ) {
			data.put(XbrlFactDataInfo.Err, error);
			return false;
		} else {
			return true;
		}
	}

	public static Date convertToDate(String val, String fmtCode) throws Exception {
		Date ret = null;

		String expr = fmtCode.replace("date-", "");

		boolean named = expr.contains("monthname");
		SimpleDateFormat fmt;

		if ( named ) {
			String locale = DustUtils.getPostfix(expr, "-");
			fmt = new SimpleDateFormat("yyyy-MMMM-dd", Locale.forLanguageTag(locale));
			expr = DustUtils.cutPostfix(expr, "-");
		} else {
			fmt = new SimpleDateFormat("yyyy-MM-dd");
		}

		expr = "(?<" + expr.replace("-", ">\\d+)\\W+(?<") + ">\\d+)";

		if ( named ) {
			expr = expr.replace("monthname>\\d", "month>\\w");
		}

		Pattern pt = Pattern.compile(expr);

		Matcher m = pt.matcher(val);

		if ( m.matches() ) {
			String yr = m.group("year");
			if ( 2 == yr.length() ) {
				yr = ((50 < Integer.parseInt(yr)) ? "19" : "20") + yr;
			}
			String normVal = DustUtils.sbAppend(null, "-", false, yr, m.group("month"), m.group("day")).toString();
			ret = fmt.parse(normVal);
		}

		return ret;
	}

	public static Double convertToDouble(String val, String fmt, String scale, String sign) {
		char decSep = '.';
		Double dVal = 0.0;
		boolean unitSep = false;

		if ( fmt.contains("zero") || fmt.contains("numdash") ) {
			dVal = 0.0;
		} else {
			if ( !DustUtils.isEmpty(val) ) {
				if ( fmt.contains("comma") && !fmt.contains("numcommadot") ) {
					decSep = ',';
				} else if ( fmt.contains("unit") ) {
					unitSep = true;
					decSep = 0;
				}

				StringBuilder sbVal = new StringBuilder();

				for (int i = 0; i < val.length(); ++i) {
					char c = val.charAt(i);

					if ( Character.isDigit(c) || (('-' == c) && (0 == i)) || (c == decSep) ) {
						sbVal.append((c == decSep) ? '.' : c);
					} else if ( unitSep && Character.isLetter(c) ) {
						sbVal.append('.');
						unitSep = false;
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
		}
		return dVal;
	}

//	static BigDecimal DEF_UNIT_DIV = new BigDecimal(100);

	public static BigDecimal convertToNumber(String val, String fmt, String scale, String decimals, String sign) {
		char decSep = '.';
		BigDecimal dVal = null;
		boolean unitSep = false;

		if ( fmt.contains("zero") || fmt.contains("numdash") ) {
			dVal = BigDecimal.ZERO;
		} else {
			if ( !DustUtils.isEmpty(val) ) {
				if ( fmt.contains("comma") && !fmt.contains("numcommadot") ) {
					decSep = ',';
				} else if ( fmt.contains("unit") ) {
					unitSep = true;
					decSep = 0;
				}

				StringBuilder sbVal = new StringBuilder();
				StringBuilder sbFrac = new StringBuilder();
				StringBuilder sbApp = sbVal;

				for (int i = 0; i < val.length(); ++i) {
					char c = val.charAt(i);

					if ( Character.isDigit(c) || (('-' == c) && (0 == i)) ) {
						sbApp.append(c);
					} else if ( (unitSep && !Character.isDigit(c) && !Character.isWhitespace(c)) || (c == decSep) ) {
						sbApp = sbFrac;
					}
				}

				BigDecimal dUnit = null;

				if ( 0 < sbFrac.length() ) {
					if ( unitSep ) {
						dUnit = new BigDecimal(sbFrac.toString());
						dUnit = dUnit.movePointLeft(2);
					} else {
						sbVal.append(".").append(sbFrac);
					}
				}

				dVal = new BigDecimal(sbVal.toString());
				if ( null != dUnit ) {
					dVal = dVal.add(dUnit);
				}

				if ( !DustUtils.isEmpty(scale) && !"0".equals(scale) ) {
					dVal = dVal.movePointRight(Integer.valueOf(scale));
				}

				if ( "-".equals(sign) ) {
					dVal = dVal.negate();
				}

				if ( !DustUtils.isEmpty(decimals) && !"0".equals(decimals) && !"INF".equals(decimals) ) {
					dVal = dVal.setScale(Integer.valueOf(decimals), RoundingMode.FLOOR);
				}
			}
		}

		return dVal;
	}

	public static String getTaxonomyItemID(Element e) {
		String fromId = e.getAttribute("xlink:href");
		fromId = DustUtils.getPostfix(fromId, "#");
		fromId = DustUtils.getPostfix(fromId, "_");
		return fromId;
	}

	public static XbrlTaxonomyLoader readTaxonomy(DustStreamUrlCache urlCache, File taxFolder, String targetTaxonomy) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

		Dust.dumpObs("Reading taxonomy", taxFolder.getName());

		File txMeta = new File(taxFolder, "META-INF");

		File fCat = new File(txMeta, "catalog.xml");
		Document catalog = db.parse(fCat);
		File fTaxPack = new File(txMeta, "taxonomyPackage.xml");
		Element taxPack = db.parse(fTaxPack).getDocumentElement();

		DustUrlResolver urlResolver = new DustUrlResolver(txMeta);
		NodeList nl = catalog.getElementsByTagName("rewriteURI");
		for (int ni = nl.getLength(); ni-- > 0;) {
			NamedNodeMap atts = nl.item(ni).getAttributes();
			urlResolver.uriRewrite.put(atts.getNamedItem("uriStartString").getNodeValue(), atts.getNamedItem("rewritePrefix").getNodeValue());
		}

		DustStreamXmlDocumentGraphLoader xmlLoader = new DustStreamXmlDocumentGraphLoader(urlCache);

		XbrlTaxonomyLoader taxonomyCollector = new XbrlTaxonomyLoader(urlResolver, targetTaxonomy);
		taxonomyCollector.getFolderCoverage().setSeen(fCat, fTaxPack);

		nl = taxPack.getElementsByTagName("tp:entryPointDocument");
		for (int ni = 0; ni < nl.getLength(); ++ni) {
			String url = nl.item(ni).getAttributes().getNamedItem("href").getNodeValue();
			xmlLoader.loadDocument(url, taxonomyCollector);
		}

		return taxonomyCollector;
	}
}
