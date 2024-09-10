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

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.poc.XbrlDockPoc;
import com.xbrldock.poc.utils.XbrlDockPocUtilsValueConverter;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsXml;

public class XbrlDockFormatXhtml implements XbrlDockFormatConsts, XbrlDockConsts.ReportFormatHandler {

	

	XbrlToken[] cvtKeys = { XbrlToken.scale, XbrlToken.decimals, XbrlToken.sign };

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

			Document doc = XbrlDockUtilsXml.parseDoc(in, XbrlDockPoc.URL_CACHE);

			Element eHtml = doc.getDocumentElement();

			String defLang = XbrlDockUtils.isEmpty(forcedLang) ? eHtml.getAttribute("xml:lang") : forcedLang;

			NamedNodeMap atts = eHtml.getAttributes();

			for (int i = atts.getLength(); i-- > 0;) {
				Node n = atts.item(i);

				String attName = n.getNodeName();
				if (attName.startsWith("xmlns")) {
					sVal = n.getNodeValue();
					if (!XbrlDockUtils.isEmpty(sVal)) {
						int si = attName.indexOf(":");

						dataHandler.addNamespace(attName.substring(si + 1), sVal);
					}
				}
			}

			NodeList nl = eHtml.getElementsByTagName("*");
			int nodeCount = nl.getLength();

			Map<XbrlToken, Object> segmentData = new TreeMap<>();
			Map<String, String> ctxDim = new TreeMap<>();

			Map<String, Element> continuation = new TreeMap<>();

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
					}
					break;
				case "xbrli:context":
					segmentData.clear();

					segmentData.put(XbrlToken.id, e.getAttribute("id"));

					sVal = getInfo(e, "xbrli:instant");
					if (XbrlDockUtils.isEmpty(sVal)) {
						segmentData.put(XbrlToken.startDate, getInfo(e, "xbrli:startDate"));
						segmentData.put(XbrlToken.endDate, getInfo(e, "xbrli:endDate"));
					} else {
						segmentData.put(XbrlToken.instant, sVal);
					}

					Element eS = null;

					eS = (Element) e.getElementsByTagName("xbrli:segment").item(0);
					if (null == eS) {
						segmentData.put(XbrlToken.entity, getInfo(e, "xbrli:entity"));
						eS = (Element) e.getElementsByTagName("xbrli:scenario").item(0);
					} else {
						NodeList nn = e.getElementsByTagName("xbrli:entity");
						if (0 < nl.getLength()) {
							sVal = getInfo((Element) nn.item(0), "xbrli:identifier");
							segmentData.putIfAbsent(XbrlToken.entity, sVal);
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
							segmentData.put(XbrlToken.dimensions, ctxDim);
						}
					}

					dataHandler.processSegment(XbrlReportSegment.Context, segmentData);

					break;
				case "xbrli:unit":
					segmentData.clear();

					sVal = getInfo(e, "xbrli:unitNumerator");
					if (XbrlDockUtils.isEmpty(sVal)) {
						segmentData.put(XbrlToken.measure, getInfo(e, "xbrli:measure"));
					} else {
						segmentData.put(XbrlToken.unitNumerator, sVal);
						segmentData.put(XbrlToken.unitDenominator, getInfo(e, "xbrli:unitDenominator"));
					}

					dataHandler.processSegment(XbrlReportSegment.Unit, segmentData);
					break;
				case "ix:continuation":
					continuation.put(e.getAttribute("id"), e);
					break;
				}
			}

			for (int idx = 0; idx < nodeCount; ++idx) {
				Element e = (Element) nl.item(idx);
				String ctxId = e.getAttribute("contextRef");

				if (!XbrlDockUtils.isEmpty(ctxId)) {
					segmentData.clear();

					String valOrig = e.getTextContent().trim();
					String fmtCode = e.getAttribute("format");

					if (XbrlDockUtils.isEmpty(fmtCode)) {
						String tn = e.getTagName();

						if (tn.contains("nonFraction")) {
							fmtCode = "ixt4:num-dot-decimal";
						}
					}

					if (!XbrlDockUtils.isEmpty(fmtCode)) {
						segmentData.put(XbrlToken.xbrldockOrigValue, valOrig);
						segmentData.put(XbrlToken.value, valOrig);
						segmentData.put(XbrlToken.format, fmtCode);
						for (XbrlToken xt : cvtKeys) {
							Object v = e.getAttribute(xt.name());
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
							segmentData.put(XbrlToken.language, txtLang);
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

						segmentData.put(XbrlToken.value, XbrlDockUtils.toString(merge));
						segmentData.put(XbrlToken.xbrldockFactType, XbrlFactDataType.text);
					}

					dataHandler.processSegment(XbrlReportSegment.Fact, segmentData);
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
