package com.xbrldock.poc.format;

import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.xbrldock.format.XbrlDockFormatConsts;
import com.xbrldock.format.XbrlDockFormatUtils;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.poc.utils.XbrlDockPocUtilsValueConverter;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsXml;

public class XbrlDockFormatXhtml implements XbrlDockFormatConsts, XbrlDockPocConsts.ReportFormatHandler {

	private static final String[] cvtKeys = { XDC_FACT_TOKEN_scale, XDC_FACT_TOKEN_decimals, XDC_FACT_TOKEN_sign };

	private String nsXbrli = "xbrli";
	String forcedLang;

	public void forceLang(String lang) {
		forcedLang = lang;
	}

	@Override
	public void loadReport(InputStream in, ReportDataHandler dataHandler) throws Exception {

		Throwable loadErr = null;

		try {
			String sVal;

			Document doc = XbrlDockUtilsXml.parseDoc(in);
			Element eHtml = doc.getDocumentElement();
			
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
					}
				}
			}

			String defLang = XbrlDockUtils.isEmpty(forcedLang) ? eHtml.getAttribute("xml:lang") : forcedLang;

			Map<String, Object> segmentData = new TreeMap<>();
			Map<String, String> ctxDim = new TreeMap<>();

			Map<String, Element> continuation = new TreeMap<>();

			NodeList nl = eHtml.getElementsByTagName("*");
			int nodeCount = nl.getLength();

			for (int idx = 0; idx < nodeCount; ++idx) {
				Element e = (Element) nl.item(idx);
				String tagName = e.getTagName();

				switch (tagName) {
				case "ix:references":
					break;
				case "ix:continuation":
					continuation.put(e.getAttribute("id"), e);
					break;
				case "link:schemaRef":
					sVal = e.getAttribute("xlink:href");
					if (!XbrlDockUtils.isEmpty(sVal)) {
						String lt = e.getAttribute("xlink:type");
						dataHandler.addTaxonomy(sVal.trim(), lt);
					}
					break;
				case "xbrli:context":
					segmentData.clear();

					segmentData.put(XDC_FACT_TOKEN_context, e.getAttribute("id"));

					sVal = XbrlDockUtilsXml.getInfo(e, nsXbrli, "instant");
					if (XbrlDockUtils.isEmpty(sVal)) {
						String cs = XbrlDockUtilsXml.getInfo(e, nsXbrli, "startDate");
						segmentData.put(XDC_EXT_TOKEN_startDate, cs);

						String ce = XbrlDockUtilsXml.getInfo(e, nsXbrli, "endDate");
						segmentData.put(XDC_EXT_TOKEN_endDate, ce);
					} else {
						segmentData.put(XDC_FACT_TOKEN_instant, sVal);
					}

					Element eS = null;

					eS = (Element) e.getElementsByTagName("xbrli:segment").item(0);
					if (null == eS) {
						segmentData.put(XDC_FACT_TOKEN_entity, XbrlDockUtilsXml.getInfo(e, nsXbrli, "entity"));
						eS = (Element) e.getElementsByTagName("xbrli:scenario").item(0);
					} else {
						NodeList nn = e.getElementsByTagName("xbrli:entity");
						if (0 < nl.getLength()) {
							Element ee = (Element) nn.item(0);
							sVal = XbrlDockUtilsXml.getInfo(ee, nsXbrli, "identifier");
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

					sVal = XbrlDockUtilsXml.getInfo(e, nsXbrli, "unitNumerator");
					if (XbrlDockUtils.isEmpty(sVal)) {
						segmentData.put(XDC_FACT_TOKEN_measure, XbrlDockUtilsXml.getInfo(e, nsXbrli, "measure"));
					} else {
						segmentData.put(XDC_FACT_TOKEN_unitNumerator, sVal);
						segmentData.put(XDC_FACT_TOKEN_unitDenominator, XbrlDockUtilsXml.getInfo(e, nsXbrli, "unitDenominator"));
					}

					dataHandler.processSegment(XDC_REP_SEG_Unit, segmentData);
					break;
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
						segmentData.put(XDC_EXT_TOKEN_value, valOrig);
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
						segmentData.put(XDC_EXT_TOKEN_value, txtVal);
						if (txtVal.length() > XbrlDockFormatUtils.TXT_CLIP_LENGTH) {
							segmentData.put(XDC_FACT_TOKEN_xbrldockFactType, XDC_FACT_VALTYPE_text);
							String id = dataHandler.processSegment(XDC_REP_SEG_Fact, segmentData);
							segmentData.put(XDC_EXT_TOKEN_id, id);
							segmentData.put(XDC_EXT_TOKEN_value, txtVal.substring(0, XbrlDockFormatUtils.TXT_CLIP_LENGTH - 3) + "...");
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
