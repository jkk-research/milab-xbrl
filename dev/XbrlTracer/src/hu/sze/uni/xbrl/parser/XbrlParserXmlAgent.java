package hu.sze.uni.xbrl.parser;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsFile;
import hu.sze.milab.dust.utils.DustUtilsNarrative;

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
		String sep = Dust.access(MindAccess.Peek, "", MIND_TAG_CONTEXT_SELF, MISC_ATT_GEN_SEP_ITEM) + File.separator;
		String fileName = DustUtils.getPostfix(filePath, sep);

		boolean test = DustDevUtils.chkTag(MIND_TAG_CONTEXT_SELF, DEV_TAG_TEST);
		boolean hash = DustDevUtils.chkTag(MIND_TAG_CONTEXT_SELF, MISC_TAG_DBLHASH);

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

			NodeList topNodes;
			NodeList nl1;

			Map<String, Element> continuation = new TreeMap<>();
			Map<String, String> units = new TreeMap<>();
			Map<String, Map<String, String>> contexts = new TreeMap<>();

			Map<String, String> dd = new TreeMap<>();

//			DustDevCounter cc = new DustDevCounter("html toc", true);

			topNodes = eHtml.getChildNodes();
			for (int idx = 0; idx < topNodes.getLength(); ++idx) {
				Node item = topNodes.item(idx);
				if ( item instanceof Element ) {
					Element e = (Element) item;
					String id = e.getAttribute(XML_ATT_ID);
					Map<String, String> cd;

					String tagName = e.getTagName();

					tagName = DustUtils.getPostfix(tagName, ":");

//					cc.add(tagName);

					switch ( tagName ) {
					case "context":
						nl1 = e.getElementsByTagName("*");
						cd = new TreeMap<>();
						dd.clear();

						for (int ii = 0; ii < nl1.getLength(); ++ii) {
							Element ec = (Element) nl1.item(ii);
							String et = ec.getTagName();
							et = DustUtils.getPostfix(et, ":");
							
							FactFldCommon ffc = null;

							switch ( et ) {
							case "identifier":
								ffc = FactFldCommon.EntityId;
								break;
							case "startDate":
								ffc = FactFldCommon.StartDate;
								break;
							case "endDate":
								ffc = FactFldCommon.EndDate;
								break;
							case "instant":
								ffc = FactFldCommon.Instant;
								break;

							case "explicitMember":
								dd.put(ec.getAttribute("dimension"), ec.getTextContent().trim());
								continue;

							default:
								continue;
							}

							String ev = ec.getTextContent();

							if ( null != ev ) {
								ev = ev.trim();
								if ( !DustUtils.isEmpty(ev) ) {
									if ( null != ffc ) {
										et = ffc.name();
									}
									cd.put(et, ev);
								}
							}
						}

						cd.put(FactFldCommon.Dimensions.name(), (dd.isEmpty()) ? "" : JSONValue.toJSONString(dd));
						contexts.put(id, cd);

						break;
					case "unit":
						nl1 = e.getElementsByTagName("*");
						String meas = null;
						String denom = null;

						boolean readDenom = false;

						for (int ii = 0; ii < nl1.getLength(); ++ii) {
							Node ic = nl1.item(ii);
							if ( ic instanceof Element ) {
								Element ec = (Element) ic;
								String et = ec.getTagName();
								et = DustUtils.getPostfix(et, ":");

								switch ( et ) {
								case "unitDenominator":
									readDenom = true;
									break;
								case "measure":
									String ev = ec.getTextContent().trim();
									ev = DustUtils.getPostfix(ev, ":");
									if ( readDenom ) {
										denom = ev;
									} else {
										meas = ev;
									}
									break;
								}
							}
						}

						if ( null != denom ) {
							meas = meas + "/" + denom;
						}
						units.put(id, meas);

						break;
					case "continuation":
						continuation.put(id, e);
						break;
					}
				}
			}
//			Dust.log(EVENT_TAG_TYPE_TRACE, cc.toString());

			if ( contexts.isEmpty() ) {
				Dust.log(EVENT_TAG_TYPE_WARNING, filePath, "EMPTY contexts???");
			} else {
//				Dust.log(EVENT_TAG_TYPE_TRACE, "  Contexts", contexts.size());
			}

			if ( units.isEmpty() ) {
				Dust.log(EVENT_TAG_TYPE_WARNING, filePath, "EMPTY units???");
			} else {
//				Dust.log(EVENT_TAG_TYPE_TRACE, "  Units", units.size());
			}

			if ( !test ) {
				String fileId = DustUtils.cutPostfix(fileName, ".");
				if ( hash ) {
					fileId = DustUtilsFile.addHash2(fileId);
				}

				hRowData = getOutRow(fileId, true);
				hRowText = getOutRow(fileId, false);
			}

			int factCount = 0;
			Set<String> missingCtx = new TreeSet<>();
			Set<String> missingUnit = new TreeSet<>();
			for (int idx = 0; idx < topNodes.getLength(); ++idx) {
				Node item = topNodes.item(idx);
				if ( item instanceof Element ) {
					Element e = (Element) topNodes.item(idx);

					String ctxId = e.getAttribute("contextRef");

					if ( !DustUtils.isEmpty(ctxId) ) {
						++factCount;

						String factId = e.getAttribute(XML_ATT_ID);
						if ( DustUtils.isEmpty(factId) ) {
							factId = "__" + factCount;
						}

						String val = e.getTextContent().trim();
						int vlen = val.length();
						String dec = e.getAttribute("decimals");

						FactType factType = DustUtils.isEmpty(dec) ? (vlen < STRING_LIMIT) ? FactType.String : FactType.Text : FactType.Numeric;

						Map<String, String> ctx = contexts.get(ctxId.trim());
						if ( null == ctx ) {
							missingCtx.add(ctxId);
						}

						initRow(hRowData, fileName, ctxId, ctx, factId, factType, e);

						if ( FactType.Numeric == factType ) {
							String unit = "-";

							String unitId = e.getAttribute("unitRef");
							if ( !DustUtils.isEmpty(unitId) ) {
								unit = units.get(unitId.trim());
								if ( null == unit ) {
									Dust.log(EVENT_TAG_TYPE_WARNING, "  Referred unit not found", unitId);
									missingUnit.add(unitId);
									unit = "-";
								}
							}
						
							setRowData(hRowData, FactFldData.UnitId, unitId);
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
								initRow(hRowText, fileName, ctxId, ctx, factId, factType, e);

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

								setRowData(hRowText, FactFldText.Value, DustStreamUtils.csvEscape(val, true));

								Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, hRowText);
							}

							String clipVal = (factType == FactType.Text) ? val.substring(0, STRING_LIMIT - 3) + "..." : val;
							setRowData(hRowData, FactFldData.OrigValue, DustStreamUtils.csvEscape(clipVal, true));
							setRowData(hRowData, FactFldData.Value, Integer.toString(vlen));
						}

						Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, hRowData);
					}
				}
			}
			
			Dust.log(EVENT_TAG_TYPE_INFO, filePath, "Fact count", factCount);
			
			if ( !missingCtx.isEmpty() ) {
				Dust.log(EVENT_TAG_TYPE_WARNING, filePath, "Referred context not found", missingCtx);
			}
			if ( !missingUnit.isEmpty() ) {
				Dust.log(EVENT_TAG_TYPE_WARNING, filePath, "Referred unit not found", missingUnit);
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

	public void initRow(MindHandle hRow, String fileName, String ctxId, Map<String, String> ctx, Object factId, FactType factType, Element e) {
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
		setRowData(hRow, FactFldCommon.CtxId, ctxId);
		setRowData(hRow, FactFldCommon.FactId, DustUtils.toString(factId));
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
