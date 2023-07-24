package hu.sze.uni.xbrl;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

	public void load(File f) throws Exception {

		String fName = f.getName();
		Dust.dumpObs("Reading", fName);

		if ( null == dbf ) {
			dbf = DocumentBuilderFactory.newInstance();
		}

		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(f);

		Element eHtml = doc.getDocumentElement();

		NodeList nl;

		NamedNodeMap nnm = eHtml.getAttributes();

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
			getInfo(cd, e, "xbrli:entity");
			getInfo(cd, e, "xbrli:startDate");
			getInfo(cd, e, "xbrli:endDate");
			getInfo(cd, e, "xbrli:instant");

			contexts.put(e.getAttribute("id"), cd);
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

		nl = eHtml.getElementsByTagName("*");
		int fc = 0;
		int fmtCnt = 0;
		for (int idx = 0; idx < nl.getLength(); ++idx) {
			Element e = (Element) nl.item(idx);
			String tag = e.getAttribute("name");

			String ctxId = e.getAttribute("contextRef");
			if ( !DustUtils.isEmpty(ctxId) ) {

				cntTags.add(e.getTagName());
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

				String lang = e.getAttribute("xml:lang");
				if ( !DustUtils.isEmpty(lang) ) {
					cntLang.add(lang);
				}

				String dec = e.getAttribute("decimals");
				String scale = e.getAttribute("scale");

				String val = e.getTextContent();

				// name id contextRef identifier instant startDate endDate
				// ComponentsOfEquityAxis unitRef VALUE scale decimals format NumValue
				StringBuilder sbLine = DustUtils.sbAppend(null, ",", true, fName, tag, ctxId, ctx, val, unit, fmt, dec, scale);

//				Dust.dumpObs(sbLine);
			}
		}

		Dust.dumpObs("  All ix content", fc, "formats", fmtCnt);
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
		cntTags.dump("Tags");
		cntFormats.dump("Formats");
		cntLang.dump("Languages");
	}
}
