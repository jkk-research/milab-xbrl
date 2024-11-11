package com.xbrldock.poc.format;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.poc.utils.XbrlDockPocUtilsValueConverter;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsXml;

public class XbrlDockFormatXhtml implements XbrlDockFormatConsts, XbrlDockPocConsts.ReportFormatHandler {

	private static final String[] cvtKeys = { XDC_FACT_TOKEN_scale, XDC_FACT_TOKEN_decimals, XDC_FACT_TOKEN_sign };

	String forcedLang;

	public void forceLang(String lang) {
		forcedLang = lang;
	}

	private String getInfo(Element e, String tagName) {
		NodeList nl = e.getElementsByTagName(tagName);
		if (0 < nl.getLength()) {
			String val = nl.item(0).getTextContent();
			if (!XbrlDockUtils.isEmpty(val)) {
				return val.trim();
			}
		}
		return null;
	}

	@Override
	public void loadReport(InputStream in, ReportDataHandler dataHandler) throws Exception {

		Throwable loadErr = null;

		try {
			DecimalFormat df = new DecimalFormat("#");
			df.setMaximumFractionDigits(8);

			String sVal;

			Document doc = XbrlDockUtilsXml.parseDoc(in);

			Element eHtml = doc.getDocumentElement();

			String defLang = XbrlDockUtils.isEmpty(forcedLang) ? eHtml.getAttribute("xml:lang") : forcedLang;

			NodeList nl = eHtml.getElementsByTagName("*");
			int nodeCount = nl.getLength();

			Map<String, Object> segmentData = new TreeMap<>();
			Map<String, String> ctxDim = new TreeMap<>();

			Map<String, Element> continuation = new TreeMap<>();
//			ArrayList<String> schemas = new ArrayList<>();
//			Map<String, String> namespaces = new TreeMap<>();

//			String repStart = null;
//			String repEnd = null;

			for (int idx = 0; idx < nodeCount; ++idx) {
				Element e = (Element) nl.item(idx);
				String tagName = e.getTagName();

				switch (tagName) {
				case "ix:references":
					break;
				case "link:schemaRef":
					sVal = e.getAttribute("xlink:href");
					if (!XbrlDockUtils.isEmpty(sVal)) {
						dataHandler.addTaxonomy(sVal.trim());
//						schemas.add(sVal.trim());
					}
					break;
				case "xbrli:context":
					segmentData.clear();

					segmentData.put(XDC_FACT_TOKEN_context, e.getAttribute("id"));

					sVal = getInfo(e, "xbrli:instant");
					if (XbrlDockUtils.isEmpty(sVal)) {
						String cs = getInfo(e, "xbrli:startDate");
						segmentData.put(XDC_FACT_TOKEN_startDate, cs);

//						if ( (null == repStart) || (0 > repStart.compareTo(sVal)) ) {
//							repStart = sVal;
//						}

						String ce = getInfo(e, "xbrli:endDate");
						segmentData.put(XDC_FACT_TOKEN_endDate, ce);
//						if ( (null == repEnd) || (0 < repEnd.compareTo(sVal)) ) {
//							repEnd = sVal;
//						}

//						dataHandler.addContextRange(cs, ce);

					} else {
						segmentData.put(XDC_FACT_TOKEN_instant, sVal);
//						dataHandler.addContextRange(sVal, sVal);
//						if ( (null == repStart) || (0 > repStart.compareTo(sVal)) ) {
//							repStart = sVal;
//						}
//						if ( (null == repEnd) || (0 < repEnd.compareTo(sVal)) ) {
//							repEnd = sVal;
//						}
					}

					Element eS = null;

					eS = (Element) e.getElementsByTagName("xbrli:segment").item(0);
					if (null == eS) {
						segmentData.put(XDC_FACT_TOKEN_entity, getInfo(e, "xbrli:entity"));
						eS = (Element) e.getElementsByTagName("xbrli:scenario").item(0);
					} else {
						NodeList nn = e.getElementsByTagName("xbrli:entity");
						if (0 < nl.getLength()) {
							Element ee = (Element) nn.item(0);
							sVal = getInfo(ee, "xbrli:identifier");
							segmentData.putIfAbsent(XDC_FACT_TOKEN_entity, sVal);
						}
					}

					if (null != eS) {
						NodeList nlS = eS.getChildNodes();
						int dc = nlS.getLength();
						ctxDim.clear();

						for (int i2 = 0; i2 < dc; ++i2) {
							Node dn = nlS.item(i2);
							if (dn instanceof Element) {
								Element m = (Element) dn;
								String dim = m.getAttribute("dimension");
								String dVal = m.getTextContent().trim();

								ctxDim.put(dim, dVal);
							}
						}

						if (!ctxDim.isEmpty()) {
							segmentData.put(XDC_FACT_TOKEN_dimensions, ctxDim);
						}
					}

					dataHandler.processSegment(XDC_REP_SEG_Context, segmentData);

					break;
				case "xbrli:unit":
					segmentData.clear();

					segmentData.put(XDC_FACT_TOKEN_unit, e.getAttribute("id"));

					sVal = getInfo(e, "xbrli:unitNumerator");
					if (XbrlDockUtils.isEmpty(sVal)) {
						segmentData.put(XDC_FACT_TOKEN_measure, getInfo(e, "xbrli:measure"));
					} else {
						segmentData.put(XDC_FACT_TOKEN_unitNumerator, sVal);
						segmentData.put(XDC_FACT_TOKEN_unitDenominator, getInfo(e, "xbrli:unitDenominator"));
					}

					dataHandler.processSegment(XDC_REP_SEG_Unit, segmentData);
					break;
				case "ix:continuation":
					continuation.put(e.getAttribute("id"), e);
					break;
				}
			}

			NamedNodeMap headAtts = eHtml.getAttributes();
			for (int i = headAtts.getLength(); i-- > 0;) {
				Node n = headAtts.item(i);

				String attName = n.getNodeName();
				if (attName.startsWith("xmlns")) {
					sVal = n.getNodeValue();
					if (!XbrlDockUtils.isEmpty(sVal)) {
						int si = attName.indexOf(":");
						String ref = attName.substring(si + 1);

						dataHandler.addNamespace(ref, sVal);

//						namespaces.put(ref, sVal);
					}
				}
			}

			for (int idx = 0; idx < nodeCount; ++idx) {
				Element e = (Element) nl.item(idx);
				String ctxId = e.getAttribute("contextRef");

				if (!XbrlDockUtils.isEmpty(ctxId)) {
					segmentData.clear();

					segmentData.put(XDC_FACT_TOKEN_context, ctxId);
					segmentData.put(XDC_FACT_TOKEN_unit, e.getAttribute("unitRef"));
					segmentData.put(XDC_FACT_TOKEN_concept, e.getAttribute("name"));

					String valOrig = e.getTextContent().trim();
					String fmtCode = e.getAttribute("format");

					if (XbrlDockUtils.isEmpty(fmtCode)) {
						String tn = e.getTagName();

						if (tn.contains("nonFraction")) {
							fmtCode = "ixt4:num-dot-decimal";
						}
					}

					if (!XbrlDockUtils.isEmpty(fmtCode)) {
						segmentData.put(XDC_FACT_TOKEN_xbrldockOrigValue, valOrig);
						segmentData.put(XDC_GEN_TOKEN_value, valOrig);
						segmentData.put(XDC_FACT_TOKEN_format, fmtCode);
						for (String xt : cvtKeys) {
							Object v = e.getAttribute(xt);
							if (null != v) {
								segmentData.put(xt, v);
							}
						}
						XbrlDockPocUtilsValueConverter.convertValue(segmentData);
					} else {
						Element txtFrag = e;
						boolean last = false;
						StringBuilder merge = null;

						String txtLang = eHtml.getAttribute("xml:lang");
						if (XbrlDockUtils.isEmpty(txtLang)) {
							txtLang = defLang;
						}
						if (!XbrlDockUtils.isEmpty(txtLang)) {
							segmentData.put(XDC_FACT_TOKEN_language, txtLang);
						}

						do {
							merge = XbrlDockUtils.sbAppend(merge, " ", false, valOrig);

							String contID = txtFrag.getAttribute("continuedAt");
							last = XbrlDockUtils.isEmpty(contID);

							if (!last) {
								txtFrag = continuation.get(contID.trim());
								if (null == txtFrag) {
									last = true;
								} else {
									valOrig = txtFrag.getTextContent().trim();
								}
							}
						} while (!last);

						String txtVal = XbrlDockUtils.toString(merge);
						segmentData.put(XDC_GEN_TOKEN_value, txtVal);
						if (txtVal.length() > XbrlDockFormatUtils.TXT_CLIP_LENGTH) {
							segmentData.put(XDC_FACT_TOKEN_xbrldockFactType, XDC_FACT_VALTYPE_text);
							String id = dataHandler.processSegment(XDC_REP_SEG_Fact, segmentData);
							segmentData.put(XDC_EXT_TOKEN_id, id);
							segmentData.put(XDC_GEN_TOKEN_value, txtVal.substring(0, XbrlDockFormatUtils.TXT_CLIP_LENGTH - 3) + "...");
							segmentData.put(XDC_FACT_TOKEN_xbrldockFactType, XDC_FACT_VALTYPE_textClip);
						} else {
							segmentData.put(XDC_FACT_TOKEN_xbrldockFactType, XDC_FACT_VALTYPE_string);
						}
					}

					dataHandler.processSegment(XDC_REP_SEG_Fact, segmentData);
				}
			}
		} catch (Throwable tl) {
			loadErr = tl;
		} finally {

			if (null != loadErr) {
				throw (Exception) loadErr;
			}
		}
	}

}
