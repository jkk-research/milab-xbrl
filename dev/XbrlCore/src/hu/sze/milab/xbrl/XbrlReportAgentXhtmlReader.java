package hu.sze.milab.xbrl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts;
import hu.sze.milab.dust.utils.DustUtilsFactory;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlReportAgentXhtmlReader implements XbrlConsts, DustConsts.MindAgent {
	private static final String CSV_SEP = ",";

	private static final String COL_VALUE = "Value";
	private static final String COL_NUMVAL = "NumValue";
	private static final String COL_ERR = "Error";

	private static final String[] FLD_COMMON = { "Source", "concept", "format", "contextRef", "instant", "startDate", "endDate", "axis1", "dim1", "axis1", "dim2", "axis3", "dim3" };

	private static final String[] FLD_DATA = { "unitRef", "unit", COL_VALUE, "scale", "decimals", COL_NUMVAL, COL_ERR };

	private static final String[] FLD_TEXT = { "xml:lang", "continuedAt", COL_VALUE };

	private static final String DIM_COL = "xbrldi:explicitMember";
	private static final String CONTEXT_ELEMENT = "xbrli:context";
	private static final String UNIT_ELEMENT = "xbrli:unit";

	private XbrlInfoType readInfoType;
	private String readCtxId;
	private String readKey;

	private Map<String, String> mapData = new HashMap<>();

	DustUtilsFactory<String, Map> contexts = new DustUtilsFactory.Simple<String, Map>(true, HashMap.class);
	DustUtilsFactory<String, Map> units = new DustUtilsFactory.Simple<String, Map>(true, HashMap.class);
	ArrayList<String> axes = new ArrayList<>();

	PrintStream psData;
	PrintStream psText;

	@Override
	public MindStatus agentExecAction(MindAction action) throws Exception {
		String name = Dust.access(MindContext.Message, MindAccess.Peek, null, TEXT_ATT_NAME);
		String strVal;

		switch ( action ) {
		case Init:
//			String fn = Dust.access(MindContext.Self, MindAccess.Peek, null, STREAM_ATT_STREAM_PATH);
//			long ts = System.currentTimeMillis();
//
//			for (String n : FLD_COMMON) {
//				if ( null == psData ) {
//					psData = new PrintStream(fn + "_" + ts + "_data.csv");
//					psText = new PrintStream(fn + "_" + ts + "_text.csv");
//				} else {
//					psData.print(CSV_SEP);
//					psText.print(CSV_SEP);
//				}
//				psData.print(n);
//				psText.print(n);
//			}
//			for (String n : FLD_DATA) {
//				psData.print(CSV_SEP);
//				psData.print(n);
//			}
//			for (String n : FLD_TEXT) {
//				psText.print(CSV_SEP);
//				psText.print(n);
//			}
//			psData.println();
//			psText.println();

			break;
		case Begin:
			if ( (readInfoType == XbrlInfoType.Context) || (readInfoType == XbrlInfoType.Unit) ) {
				readKey = name;
			} else if ( name.equals(CONTEXT_ELEMENT) ) {
				readInfoType = XbrlInfoType.Context;
				readKey = name;
			} else if ( name.equals(UNIT_ELEMENT) ) {
				readInfoType = XbrlInfoType.Unit;
				readKey = name;
			} else if ( name.startsWith("ix:") ) {
				if ( name.equals("ix:header") || name.equals("ix:references")
						|| name.equals("ix:resources") || name.equals("") ) {
				} else {
					readInfoType = XbrlInfoType.Fact;
					mapData.clear();
				}
//			} else {
//				Dust.dumpObs("Element", name);
			}

			break;
		case Process:
			if ( null != readInfoType ) {
				Object itemType = Dust.access(MindContext.Message, MindAccess.Peek, null, MIND_ATT_KNOWLEDGE_TAGS);
				strVal = Dust.access(MindContext.Message, MindAccess.Peek, null, MISC_ATT_VARIANT_VALUE);

				switch ( readInfoType ) {
				case Unit:
					break;
				case Context:
					if ( itemType == XmlData.Attribute ) {
						if ( readKey.equals(CONTEXT_ELEMENT) && "id".equals(name) ) {
							readCtxId = strVal;
						} else if ( readKey.equals(DIM_COL) && "dimension".equals(name) ) {
							readKey = strVal;
						}
					} else if ( itemType == XmlData.Content ) {
						contexts.get(readCtxId).put(readKey.split(":")[1], strVal);
						readKey = null;
					}

					break;
				case Fact:
					if ( itemType == XmlData.Attribute ) {
						if ( "contextRef".equals(name) ) {
							Map mm = contexts.peek(strVal);
							if ( null == mm ) {
								Dust.dumpObs("Context ref not resolved", strVal);
							} else {
								mapData.putAll(mm);
							}
						}
					} else if ( itemType == XmlData.Content ) {
						name = COL_VALUE;
					}

					mapData.put(name, strVal);
					break;
				}
			}
			break;
		case End:
			if ( null != readInfoType ) {
				boolean reset = false;

				switch ( readInfoType ) {
				case Unit:
					reset = name.equals(UNIT_ELEMENT);
					break;
				case Context:
					reset = name.equals(CONTEXT_ELEMENT);
					break;
				case Fact:
					reset = true;
					Double val = 0.0;
					boolean data = false;

					if ( "ixt4:num-comma-decimal".equals(mapData.getOrDefault("format", "ixt4:fixed-zero")) ) {
						data = true;
						strVal = mapData.get(COL_VALUE);
						if ( null != strVal ) {
							try {
								val = Double.parseDouble(strVal);
							} catch (Throwable e) {
								mapData.put(COL_ERR, e.toString());
							}
							strVal = mapData.get("scale");
							if ( null != strVal ) {
								int scale = Integer.parseInt(strVal);
								if ( 0 < scale ) {
									double sc = Math.pow(10, scale);
									val = val * sc;
								}
							}
						}
					}

					PrintStream ps = null;

					for (String n : FLD_COMMON) {
						if ( null == ps ) {
							ps = System.out;
						} else {
							ps.print(CSV_SEP);
						}
						ps.print(mapData.get(n));
					}

					for (String n : (data ? FLD_DATA : FLD_TEXT)) {
						ps.print(CSV_SEP);
						ps.print(mapData.get(n));
					}

					ps.println();
					break;
				}

				if ( reset ) {
					readInfoType = null;
					readKey = null;
				}
			}
			break;
		default:
			break;
		}

		return MindStatus.ReadAccept;
	}
}
