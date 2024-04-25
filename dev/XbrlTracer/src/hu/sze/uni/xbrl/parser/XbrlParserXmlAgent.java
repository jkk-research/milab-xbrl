package hu.sze.uni.xbrl.parser;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.TreeMap;

import org.json.simple.JSONValue;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.dev.DustDevUtils;
import hu.sze.milab.dust.stream.DustStreamUtils;
import hu.sze.milab.dust.stream.xml.DustXmlUtils;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsFile;
import hu.sze.milab.dust.utils.DustUtilsNarrative;
import hu.sze.uni.xbrl.XbrlUtils;

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
		String filePath = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, RESOURCE_ATT_URL_PATH);
		String fileName = DustUtils.getPostfix(filePath, File.separator);

		boolean test = DustDevUtils.chkTag(MIND_TAG_CONTEXT_SELF, DEV_TAG_TEST);

		DecimalFormat df = new DecimalFormat("#");
		df.setMaximumFractionDigits(8);

		MindHandle hRowData = null;
		MindHandle hRowText = null;

		try {
			Document doc = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, DUST_ATT_IMPL_DATA);
			Element eHtml = doc.getDocumentElement();

			Map<String, String> namespaces = new TreeMap<>();

			NamedNodeMap nnm = eHtml.getAttributes();
			for (int idx = 0; idx < nnm.getLength(); ++idx) {
				Attr a = (Attr) nnm.item(idx);
				if ( a.getName().startsWith(XML_NSPREFIX) ) {
					namespaces.put(a.getName().substring(XML_NSPREFIX.length()), a.getValue());
				}
			}

			NodeList nl;

			Map<String, Element> continuation = new TreeMap<>();
			Map<String, String> units = new TreeMap<>();
			Map<String, Map<String, String>> contexts = new TreeMap<>();

			nl = eHtml.getElementsByTagName(XBRLTOKEN_CONTEXT);
			for (int idx = 0; idx < nl.getLength(); ++idx) {
				Element e = (Element) nl.item(idx);
				Map<String, String> cd = new TreeMap<>();

				String ctxId = e.getAttribute(XML_ATT_ID);
				contexts.put(ctxId, cd);

				XbrlUtils.loadCtxFields(e, cd);

				Element eS = (Element) e.getElementsByTagName(XBRLTOKEN_SCENARIO).item(0);
				if ( null != eS ) {
					NodeList nlS = eS.getChildNodes();
					Map<String, String> dims = null;

					for (int i2 = 0; i2 < nlS.getLength(); ++i2) {
						Node ii = nlS.item(i2);

						if ( ii instanceof Element ) {
							Element m = (Element) ii;
							String dim = m.getAttribute(XBRLTOKEN_DIMENSION);
							String dVal = m.getTextContent().trim();

							if ( null == dims ) {
								dims = new TreeMap<>();
							}
							dims.put(dim, dVal);
						}
					}

					cd.put(FactFldCommon.Dimensions.name(), (null == dims) ? "" : JSONValue.toJSONString(dims));
				}
			}

			if ( contexts.isEmpty() ) {
				Dust.log(EVENT_TAG_TYPE_WARNING, "  EMPTY contexts???");
			} else {
//				Dust.log(EVENT_TAG_TYPE_TRACE, "  Contexts", contexts.size());
			}

			nl = eHtml.getElementsByTagName(XBRLTOKEN_UNIT);
			for (int idx = 0; idx < nl.getLength(); ++idx) {
				Element e = (Element) nl.item(idx);

				String val = DustXmlUtils.getInfo(e, XBRLTOKEN_UNIT_NUM);
				if ( null != val ) {
					String denom = DustXmlUtils.getInfo(e, XBRLTOKEN_UNIT_DENOM);
					val = val + "/" + denom;
				} else {
					val = DustXmlUtils.getInfo(e, XBRLTOKEN_MEASURE);
				}

				units.put(e.getAttribute(XML_ATT_ID), val);
			}

			if ( units.isEmpty() ) {
				Dust.log(EVENT_TAG_TYPE_WARNING, "  EMPTY units???");
			} else {
//				Dust.log(EVENT_TAG_TYPE_TRACE, "  Units", units.size());
			}

			nl = eHtml.getElementsByTagName(XBRLTOKEN_CONTINUATION);
			for (int idx = 0; idx < nl.getLength(); ++idx) {
				Element e = (Element) nl.item(idx);
				continuation.put(e.getAttribute(XML_ATT_ID), e);
			}

			if ( !test ) {
				String fileId = DustUtils.cutPostfix(fileName, ".");
				fileId = DustUtilsFile.addHash2(fileId);

				hRowData = getOutRow(fileId, true);
				hRowText = getOutRow(fileId, false);
			}

			nl = eHtml.getElementsByTagName("*");
			int factId = 0;
			for (int idx = 0; idx < nl.getLength(); ++idx) {
				Element e = (Element) nl.item(idx);

//				String factId = e.getAttribute(XML_ATT_ID);
				String ctxId = e.getAttribute("contextRef");

				if ( !DustUtils.isEmpty(ctxId) ) {
					++factId;

					String val = e.getTextContent().trim();
					int vlen = val.length();
					String dec = e.getAttribute("decimals");

					FactType factType = DustUtils.isEmpty(dec) ? (vlen < STRING_LIMIT) ? FactType.String : FactType.Text : FactType.Numeric;

//					MindHandle hRow = dataFact ? hRowData : hRowText;

					Map<String, String> ctx = contexts.get(ctxId.trim());
					if ( null == ctx ) {
						Dust.log(EVENT_TAG_TYPE_WARNING, "  Referred context not found", ctxId);
					}

					initRow(hRowData, fileName, ctx, factId, factType, e);

					if ( FactType.Numeric == factType ) {
						String unit = "-";

						String unitId = e.getAttribute("unitRef");
						if ( !DustUtils.isEmpty(unitId) ) {
							unit = units.get(unitId.trim());
							if ( null == unit ) {
								Dust.log(EVENT_TAG_TYPE_WARNING, "  Referred unit not found", unitId);
								unit = "-";
							}
						}

						setRowData(hRowData, FactFldData.Unit, unit);
						setRowData(hRowData, FactFldData.OrigValue, DustStreamUtils.csvEscape(val, true));
						setRowData(hRowData, FactFldData.Dec, dec);

						try {
							Double dVal = Double.valueOf(val);
							val = df.format(dVal);
							setRowData(hRowData, FactFldData.Value, val);
						} catch (Throwable err) {
							setRowData(hRowData, FactFldData.Error, err.toString());
						}
					} else {
						if ( FactType.Text == factType ) {
							initRow(hRowText, fileName, ctx, factId, factType, e);

							String lang = e.getAttribute("xml:lang");
							if ( !DustUtils.isEmpty(lang) ) {
								cntLang.add(lang);
								setRowData(hRowText, FactFldText.Language, lang);
							}

							Element txtFrag = e;
							for (String contID = txtFrag.getAttribute("continuedAt"); !DustUtils.isEmpty(contID); contID = txtFrag.getAttribute("continuedAt")) {
								txtFrag = continuation.get(contID);
								val = val + " " + txtFrag.getTextContent().trim();
							}

//						String txt = DustStreamUtils.csvEscape(val, true);
							setRowData(hRowText, FactFldText.Value, val);

							Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, hRowText);
						}
						
						String clipVal = (factType == FactType.Text) ? val.substring(0, STRING_LIMIT - 3) + "..." : val;
						setRowData(hRowData, FactFldData.OrigValue, clipVal);
						setRowData(hRowData, FactFldData.Value, Integer.toString(vlen));
					}

					Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, hRowData);
				}
			}
		} catch (Throwable t) {
			DustException.swallow(t, "reading xml", Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, RESOURCE_ATT_URL_PATH));
			return MIND_TAG_RESULT_REJECT;
		} finally {
			if ( null != hRowData ) {
				Dust.access(MindAccess.Commit, MIND_TAG_ACTION_END, hRowData);
			}
			if ( null != hRowText ) {
				Dust.access(MindAccess.Commit, MIND_TAG_ACTION_END, hRowText);
			}
		}

		return MIND_TAG_RESULT_ACCEPT;
	}

	public void initRow(MindHandle hRow, String fileName, Map<String, String> ctx, int factId, FactType factType, Element e) {
		String tagName = e.getTagName();
		String[] tt = tagName.split(":");
		String fmt = e.getAttribute("format");

		Dust.access(MindAccess.Reset, null, hRow, MISC_ATT_CONN_MEMBERMAP);

		if ( null != ctx ) {
			for (Map.Entry<String, String> ce : ctx.entrySet()) {
				setRowData(hRow, ce.getKey(), ce.getValue());
			}
		}

		setRowData(hRow, FactFldCommon.File, fileName);
		setRowData(hRow, FactFldCommon.FactIdx, DustUtils.toString(factId));
		setRowData(hRow, FactFldCommon.TagNamespace, tt[0]);
		setRowData(hRow, FactFldCommon.TagId, tt[1]);
		setRowData(hRow, FactFldCommon.Type, factType.name());
		setRowData(hRow, FactFldCommon.Format, fmt);
	}

	public void setRowData(MindHandle hRow, Enum<?> key, String val) {
		setRowData(hRow, key.name(), val);
	}

	public void setRowData(MindHandle hRow, String key, String val) {
		Dust.access(MindAccess.Set, val, hRow, MISC_ATT_CONN_MEMBERMAP, key);
	}

	public MindHandle getOutRow(String fileId, boolean data) {
		String postfix = data ? "_Data.csv" : "_Text.csv";

		MindHandle hWriter = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, data ? XBRLDOCK_ATT_XMLLOADER_ROWDATA : XBRLDOCK_ATT_XMLLOADER_ROWTEXT);
		MindHandle hRow = Dust.access(MindAccess.Peek, null, hWriter, RESOURCE_ATT_PROCESSOR_DATA);

		if ( null == Dust.access(MindAccess.Peek, null, hRow, MISC_ATT_CONN_MEMBERARR) ) {
			for (FactFldCommon fcm : FactFldCommon.values()) {
				Dust.access(MindAccess.Insert, fcm.name(), hRow, MISC_ATT_CONN_MEMBERARR, KEY_ADD);
			}

			for (Enum<?> e : (data ? FactFldData.values() : FactFldText.values())) {
				Dust.access(MindAccess.Insert, e.name(), hRow, MISC_ATT_CONN_MEMBERARR, KEY_ADD);
			}
		}

		Dust.access(MindAccess.Set, fileId + postfix, hWriter, RESOURCE_ATT_PROCESSOR_STREAM, TEXT_ATT_TOKEN);
		Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, hWriter, RESOURCE_ATT_PROCESSOR_STREAM);

		Dust.access(MindAccess.Commit, MIND_TAG_ACTION_BEGIN, hRow);

		return hRow;
	}

	@Override
	protected MindHandle agentEnd() throws Exception {
		return MIND_TAG_RESULT_ACCEPT;
	}
}
