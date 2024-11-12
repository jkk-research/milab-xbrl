package com.xbrldock.poc.format;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.poc.report.XbrlDockReportUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockFormatXmlWriter implements XbrlDockFormatConsts, XbrlDockPocConsts.ReportDataHandler {

	File targetDir;
	DecimalFormat df;

	private String repId;
	private Document xmlDoc;
	private Element eRoot;

	private Map<String, ArrayList<Element>> segData = new TreeMap<>();

	public XbrlDockFormatXmlWriter(File targetDir) {
		this.targetDir = targetDir;
		df = new DecimalFormat("#");
		df.setMaximumFractionDigits(8);
		
		XbrlDock.log(EventLevel.Trace, "Exporting reports to folder", targetDir.getPath());

	}

	@Override
	public void beginReport(String repId) {
		this.repId = repId;
		
		XbrlDock.log(EventLevel.Trace, "   ", repId);

		for (ArrayList sc : segData.values()) {
			sc.clear();
		}

		try {
			xmlDoc = XbrlDockUtilsXml.createDoc();
			xmlDoc.setXmlStandalone(true);
			eRoot = xmlDoc.createElement("xbrl");
			xmlDoc.appendChild(eRoot);

		} catch (Throwable e) {
			XbrlDockException.wrap(e, "Exporting report to XML", repId);
		}
	}

	@Override
	public void addNamespace(String ref, String id) {
		if (!"xmlns".equals(ref)) {
			ref = "xmlns:" + ref;
		}
		eRoot.setAttribute(ref, id);

	}

	@Override
	public void addTaxonomy(String tx, String type) {
		Element e = xmlDoc.createElement("link:schemaRef");
		e.setAttribute("xlink:type", XbrlDockUtils.isEmpty(type) ? "simple" : type);
		e.setAttribute("xlink:href", tx);
		eRoot.appendChild(e);
	}

	@Override
	public String processSegment(String segment, Map<String, Object> data) {
		String segIdKey = XbrlDockReportUtils.getSegmentIdKey(segment);
		String segId = (String) data.get(segIdKey);

		String sVal;

		Element e = null;
		Element e1 = null;
		Element e2 = null;
		Element e3 = null;

		switch (segment) {
		case XDC_REP_SEG_Unit:
			if (XbrlDockUtils.isEmpty(segId)) {
				break;
			}
			e = xmlDoc.createElement("xbrli:unit");
			e.setAttribute("id", segId);
			sVal = (String) data.get("measure");
			if (!XbrlDockUtils.isEmpty(sVal)) {
				e1 = xmlDoc.createElement("xbrli:measure");
				e1.setTextContent(sVal);
				e.appendChild(e1);
			} else {
				e1 = xmlDoc.createElement("xbrli:divide");

				e2 = xmlDoc.createElement("xbrli:unitNumerator");
				e3 = xmlDoc.createElement("xbrli:measure");
				e3.setTextContent((String) data.get("unitNumerator"));
				e2.appendChild(e3);
				e1.appendChild(e2);

				e2 = xmlDoc.createElement("xbrli:unitDenominator");
				e3 = xmlDoc.createElement("xbrli:measure");
				e3.setTextContent((String) data.get("unitDenominator"));
				e2.appendChild(e3);
				e1.appendChild(e2);

				e.appendChild(e1);
			}
			break;
		case XDC_REP_SEG_Context:
			e = xmlDoc.createElement("xbrli:context");
			e.setAttribute("id", segId);

			e1 = xmlDoc.createElement("xbrli:entity");
			e2 = xmlDoc.createElement("xbrli:identifier");
			e2.setAttribute(XDC_EXT_TOKEN_scheme, "http://standards.iso.org/iso/17442");
			e2.setTextContent((String) data.get("entity"));
			e1.appendChild(e2);
			e.appendChild(e1);

			e1 = xmlDoc.createElement("xbrli:period");
			sVal = (String) data.get(XDC_FACT_TOKEN_instant);

			if (XbrlDockUtils.isEmpty(sVal)) {
				e2 = xmlDoc.createElement("xbrli:startDate");
				e2.setTextContent((String) data.get(XDC_FACT_TOKEN_startDate));
				e1.appendChild(e2);
				e2 = xmlDoc.createElement("xbrli:endDate");
				e2.setTextContent((String) data.get(XDC_FACT_TOKEN_endDate));
				e1.appendChild(e2);
			} else {
				e2 = xmlDoc.createElement("xbrli:instant");
				e2.setTextContent(sVal);
				e1.appendChild(e2);
			}
			e.appendChild(e1);

			sVal = XbrlDockUtils.toString(data.get(XDC_FACT_TOKEN_dimensions));

			if (!XbrlDockUtils.isEmpty(sVal)) {
				sVal = XbrlDockUtils.getPostfix(sVal, "{");
				sVal = XbrlDockUtils.cutPostfix(sVal, "}");

				String[] dd = sVal.split(",");

				e1 = xmlDoc.createElement("xbrli:scenario");
				e.appendChild(e1);
				for (String dv : dd) {
					String[] ds = dv.split("=");
					e2 = xmlDoc.createElement("xbrli:explicitMember");
					e2.setAttribute("dimension", ds[0].trim());
					e2.setTextContent(ds[1].trim());
					e1.appendChild(e2);
				}
			}

//		   <xbrli:scenario>
//	      <xbrldi:explicitMember dimension="ifrs-full:ComponentsOfEquityAxis">ifrs-full:RetainedEarningsMember</xbrldi:explicitMember>
//	    </xbrli:scenario>

			/*
			 * <context id=
			 * "As_Of_5_31_2015_us-gaap_ValuationAllowancesAndReservesTypeAxis_us-gaap_AllowanceForDoubtfulAccountsMember">
			 * <entity> <identifier scheme="http://www.sec.gov/CIK">0000001750</identifier>
			 * <segment> <xbrldi:explicitMember
			 * dimension="us-gaap:ValuationAllowancesAndReservesTypeAxis">us-gaap:
			 * AllowanceForDoubtfulAccountsMember</xbrldi:explicitMember> </segment>
			 * </entity> <period> <instant>2015-05-31</instant> </period> </context>
			 * <context id=
			 * "Duration_6_1_2017_To_5_31_2018_us-gaap_StatementEquityComponentsAxis_us-gaap_CommonStockMember">
			 * <entity> <identifier scheme="http://www.sec.gov/CIK">0000001750</identifier>
			 * <segment> <xbrldi:explicitMember
			 * dimension="us-gaap:StatementEquityComponentsAxis">us-gaap:CommonStockMember</
			 * xbrldi:explicitMember> </segment> </entity> <period>
			 * <startDate>2017-06-01</startDate> <endDate>2018-05-31</endDate> </period>
			 * </context>
			 */
			break;
		case XDC_REP_SEG_Fact:
			String factType = (String) data.get(XDC_FACT_TOKEN_xbrldockFactType);

			if (XDC_FACT_VALTYPE_textClip.equals(factType)) {
				break;
			}

			sVal = (String) data.get(XDC_FACT_TOKEN_concept);
			e = xmlDoc.createElement(sVal);
			
			if ( !XbrlDockUtils.isEmpty(segId) ) {
				e.setAttribute("id", segId);
			}
			sVal = (String) data.get(XDC_FACT_TOKEN_context);
			e.setAttribute("contextRef", sVal);

			if (XDC_FACT_VALTYPE_text.equals(factType) || XDC_FACT_VALTYPE_string.equals(factType)) {
				sVal = (String) data.get(XDC_GEN_TOKEN_value);
				e.setTextContent(sVal);
				sVal = (String) data.get(XDC_FACT_TOKEN_language);
				e.setAttribute("lang", sVal);
			} else {
				sVal = (String) data.get(XDC_FACT_TOKEN_unit);
				e.setAttribute("unitRef", sVal);

				sVal = (String) data.get(XDC_FACT_TOKEN_decimals);
				e.setAttribute(XDC_FACT_TOKEN_decimals, sVal);

				Object val = data.get(XDC_GEN_TOKEN_value);
				if ( val instanceof BigDecimal ) {
					val = df.format((BigDecimal)val);
				}

				e.setTextContent(XbrlDockUtils.toString(val));

			}
			// <us-gaap:LongTermDebtCurrent contextRef="As_Of_5_31_2017" unitRef="Unit1"
			// decimals="-5">100000</us-gaap:LongTermDebtCurrent>

			break;
		}

		if (null != e) {
			ArrayList<Element> segContent = XbrlDockUtils.safeGet(segData, segment, ARRAY_CREATOR);
			segContent.add(e);
		}

		return XbrlDockUtils.toString(segId);
	}

	@Override
	public void endReport() {
		try {
			if ((null != eRoot) && eRoot.hasChildNodes()) {

				for (String seg : XDC_SEGMENTS) {
					ArrayList<Element> sc = segData.get(seg);
					if (null != sc) {
						for (Element e : sc) {
							eRoot.appendChild(e);
						}
					}
				}

//				xmlDoc.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"utf-8\"");
				XbrlDockUtilsXml.saveDoc(xmlDoc, new File(targetDir, repId + XDC_FEXT_XML), true);
			}
		} catch (Throwable e) {
			XbrlDockException.wrap(e, "Exporting report to XML", repId);
		}
	}
}
