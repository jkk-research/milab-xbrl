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
import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.poc.report.XbrlDockReportUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockFormatAgentXmlWriter implements XbrlDockFormatConsts, XbrlDockConsts.GenAgent {

	public static class DataHandler implements XbrlDockPocConsts.ReportDataHandler {
		XbrlDockFormatAgentXmlWriter writer;

		public DataHandler(File targetDir) {
			writer = new XbrlDockFormatAgentXmlWriter();
			XbrlDockUtils.optCallNoEx(writer, XDC_CMD_GEN_Init, targetDir);
		}

		@Override
		public void beginReport(String repId) {
			XbrlDockUtils.optCallNoEx(writer, XDC_CMD_GEN_Begin, repId);
		}

		@Override
		public void addNamespace(String ref, String id) {
			XbrlDockUtils.optCallNoEx(writer, XDC_CMD_REP_ADD_NAMESPACE, ref, id);
		}

		@Override
		public void addTaxonomy(String tx, String type) {
			XbrlDockUtils.optCallNoEx(writer, XDC_CMD_REP_ADD_SCHEMA, tx, type);
		}

		@Override
		public String processSegment(String segment, Map<String, Object> data) {
			return XbrlDockUtils.optCallNoEx(writer, segment, data);
		}

		@Override
		public void endReport() {
			XbrlDockUtils.optCallNoEx(writer, XDC_CMD_GEN_End);
		}
	}

	File targetDir;
	DecimalFormat df;

	private String repId;
	private Document xmlDoc;
	private Element eRoot;

	private String nsTag = "xbrli";
	private String entityIdSchema = "http://standards.iso.org/iso/17442";

	private boolean segmentAdded;
	private Map<String, ArrayList<Element>> segData = new TreeMap<>();

	@Override
	public Object process(String cmd, Object... params) throws Exception {
		switch (cmd) {
		case XDC_CMD_GEN_Init:
			this.targetDir = (File) params[0];
			df = new DecimalFormat("#");
			df.setMaximumFractionDigits(8);

			XbrlDock.log(EventLevel.Trace, "Exporting reports to folder", targetDir.getPath());
			break;
		case XDC_CMD_GEN_Begin:
			beginReport((String) params[0]);
			break;
		case XDC_CMD_REP_ADD_NAMESPACE:
			addNamespace((String) params[0], (String) params[1]);
			break;
		case XDC_CMD_REP_ADD_SCHEMA:
			addTaxonomy((String) params[0], (String) params[1]);
			break;
		case XDC_REP_SEG_Unit:
		case XDC_REP_SEG_Context:
		case XDC_REP_SEG_Fact:
			return processSegment(cmd, (Map<String, Object>) params[0]);
		case XDC_CMD_GEN_End:
			endReport();
			break;
		}
		return null;
	}

//	@Override
	public void beginReport(String repId) {
		this.repId = repId;

		XbrlDock.log(EventLevel.Trace, "   ", repId);

		segmentAdded = false;
		for (ArrayList sc : segData.values()) {
			sc.clear();
		}

		try {
			xmlDoc = XbrlDockUtilsXml.createDoc();
			xmlDoc.setXmlStandalone(true);
			eRoot = xmlDoc.createElement(XDC_FMTXML_TOKEN_xbrl);
			xmlDoc.appendChild(eRoot);

		} catch (Throwable e) {
			XbrlDockException.wrap(e, "Exporting report to XML", repId);
		}
	}

//	@Override
	public void addNamespace(String ref, String id) {
		if (!XDC_FMTXML_TOKEN_xmlns.equals(ref)) {
			ref = XDC_FMTXML_TOKEN_xmlns + ":" + ref;
		}
		eRoot.setAttribute(ref, id);

	}

//	@Override
	public void addTaxonomy(String tx, String type) {
		Element e = xmlDoc.createElement("link:schemaRef");
		e.setAttribute("xlink:type", XbrlDockUtils.isEmpty(type) ? "simple" : type);
		e.setAttribute("xlink:href", tx);
		eRoot.appendChild(e);
	}

//	@Override
	public String processSegment(String segment, Map<String, Object> data) {
		String segIdKey = XbrlDockReportUtils.getSegmentIdKey(segment);
		String segId = (String) data.get(segIdKey);

		String sVal;

		Element e = null;
		Element e1 = null;
		Element e2 = null;

		switch (segment) {
		case XDC_REP_SEG_Unit:
			if (XbrlDockUtils.isEmpty(segId)) {
				break;
			}
			e = XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_unit, null, segId);
			
			sVal = (String) data.get(XDC_FACT_TOKEN_measure);
			if (!XbrlDockUtils.isEmpty(sVal)) {
				XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_measure, e, null).setTextContent(sVal);
			} else {
				e1 = XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_divide, e, null);

				e2 = XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_unitNumerator, e1, null);
				XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_measure, e2, null).setTextContent((String) data.get(XDC_FACT_TOKEN_unitNumerator));

				e2 = XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_unitDenominator, e1, null);
				XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_measure, e2, null).setTextContent((String) data.get(XDC_FACT_TOKEN_unitDenominator));
			}
			break;
		case XDC_REP_SEG_Context:
			e = XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_context, null, segId);
			e1 = XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_entity, e, null);
			e2 = XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_EXT_TOKEN_identifier, e1, null);

			e2.setAttribute(XDC_EXT_TOKEN_scheme, entityIdSchema);
			e2.setTextContent((String) data.get(XDC_FACT_TOKEN_entity));

			e1 = XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_period, e, null);
			sVal = (String) data.get(XDC_FACT_TOKEN_instant);

			if (XbrlDockUtils.isEmpty(sVal)) {
				XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_EXT_TOKEN_startDate, e1, null).setTextContent((String) data.get(XDC_EXT_TOKEN_startDate));
				XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_EXT_TOKEN_endDate, e1, null).setTextContent((String) data.get(XDC_EXT_TOKEN_endDate));
			} else {
				XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_instant, e1, null).setTextContent(sVal);
			}

			sVal = XbrlDockUtils.toString(data.get(XDC_FACT_TOKEN_dimensions));

			if (!XbrlDockUtils.isEmpty(sVal)) {
				sVal = XbrlDockUtils.getPostfix(sVal, "{");
				sVal = XbrlDockUtils.cutPostfix(sVal, "}");

				String[] dd = sVal.split(",");

				e1 = XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FACT_TOKEN_scenario, e, null);
				for (String dv : dd) {
					String[] ds = dv.split("=");
					e2 = XbrlDockUtilsXml.createElement(xmlDoc, nsTag, XDC_FMTXML_TOKEN_explicitMember, e1, null);
					e2.setAttribute(XDC_FMTXML_TOKEN_dimension, ds[0].trim());
					e2.setTextContent(ds[1].trim());
				}
			}

			break;
		case XDC_REP_SEG_Fact:
			String factType = (String) data.get(XDC_FACT_TOKEN_xbrldockFactType);

			if (XDC_FACT_VALTYPE_textClip.equals(factType)) {
				break;
			}

			sVal = (String) data.get(XDC_FACT_TOKEN_concept);
			e = XbrlDockUtilsXml.createElement(xmlDoc, nsTag, sVal, null, segId);

			XbrlDockUtilsXml.optSet(e, null, XDC_FMTXML_TOKEN_contextRef, data.get(XDC_FACT_TOKEN_context));

			String factValue;

			if (XDC_FACT_VALTYPE_text.equals(factType) || XDC_FACT_VALTYPE_string.equals(factType)) {
				XbrlDockUtilsXml.optSet(e, null, XDC_EXT_TOKEN_lang, data.get(XDC_FACT_TOKEN_language));
				factValue = (String) data.get(XDC_EXT_TOKEN_value);
			} else {
				XbrlDockUtilsXml.optSet(e, null, XDC_FMTXML_TOKEN_unitRef, data.get(XDC_FACT_TOKEN_unit));
				XbrlDockUtilsXml.optSet(e, null, XDC_FACT_TOKEN_decimals, data.get(XDC_FACT_TOKEN_decimals));

				Object val = data.get(XDC_EXT_TOKEN_value);
				factValue = (val instanceof BigDecimal) ? df.format((BigDecimal) val) : XbrlDockUtils.toString(val);
			}
			e.setTextContent(XbrlDockUtils.toString(factValue));

			break;
		}

		if (null != e) {
			ArrayList<Element> segContent = XbrlDockUtils.safeGet(segData, segment, ARRAY_CREATOR);
			segContent.add(e);
			segmentAdded = true;
		}

		return XbrlDockUtils.toString(segId);
	}

//	@Override
	public void endReport() {
		try {
			if ((null != eRoot) && (segmentAdded || eRoot.hasChildNodes())) {
				for (String seg : XDC_SEGMENTS) {
					ArrayList<Element> sc = segData.get(seg);
					if (null != sc) {
						for (Element e : sc) {
							eRoot.appendChild(e);
						}
					}
				}
				XbrlDockUtilsXml.saveDoc(xmlDoc, new File(targetDir, repId + XDC_FEXT_XML), 2);
			}
		} catch (Throwable e) {
			XbrlDockException.wrap(e, "Exporting report to XML", repId);
		}
	}
}
