package hu.sze.uni.xbrl;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.ParseException;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlHandlerJson implements XbrlConsts, ContentHandler {
	public static final String KEY_DIM = "dimensions";
	public static final String KEY_UNIT = "unit";
	public static final String KEY_ENTITY = "entity";
	public static final String KEY_PERIOD = "period";
	public static final String KEY_VALUE = "value";

	private final SimpleDateFormat SDF_PERIOD = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	XbrlListener listener;
	XbrlUtilsDataCollector dc;

	Object beginRead;
	boolean readingFacts;

	Map info = new HashMap();
	Map fact;

	XbrlUtilsFactory<String, Map> contexts = new XbrlUtilsFactory<String, Map>(true) {
		@Override
		protected Map create(String key, Object... hints) {
			Map ret = new TreeMap<>();
			ret.put(KEY_XBRL_ID, "c-" + (contexts.size() + 1));
			ret.put(KEY_XBRL_ENTITY, hints[0]);

			try {
				String[] period = ((String) hints[1]).split("/");
				if ( 1 == period.length ) {
					ret.put(KEY_XBRL_INSTANT, SDF_PERIOD.parse(period[0]));
				} else {
					ret.put(KEY_XBRL_STARTDATE, SDF_PERIOD.parse(period[0]));
					ret.put(KEY_XBRL_ENDDATE, SDF_PERIOD.parse(period[1]));
				}
			} catch (java.text.ParseException e) {
				e.printStackTrace();
			}

			listener.handleXbrlInfo(XbrlInfoType.Context, ret);

			return ret;
		}
	};

	XbrlUtilsFactory<String, Map> units = new XbrlUtilsFactory<String, Map>(true) {
		@Override
		protected Map create(String key, Object... hints) {
			Map ret = new TreeMap<>();
			ret.put(KEY_XBRL_ID, "u-" + (units.size() + 1));

			String[] split = ((String) key).split("/");
			if ( 1 == split.length ) {
				ret.put(KEY_XBRL_UNIT_MEASURE, split[0]);
			} else {
				ret.put(KEY_XBRL_UNIT_NUMERATOR, split[0]);
				ret.put(KEY_XBRL_UNIT_DENOMINATOR, split[1]);
			}

			listener.handleXbrlInfo(XbrlInfoType.Unit, ret);

			return ret;
		}
	};

	public void setListener(XbrlListener listener) {
		this.listener = listener;
	}

	@Override
	public boolean endArray() throws ParseException, IOException {
		if ( null != dc ) {
			optCloseCollector();
		}
		return true;
	}

	@Override
	public void endJSON() throws ParseException, IOException {
	}

	@Override
	public boolean endObject() throws ParseException, IOException {
		if ( null != dc ) {
			optCloseCollector();
		}
		return true;
	}

	public void optCloseCollector() {
		if ( !dc.close() ) {
			dc = null;

			if ( null != listener ) {
				if ( !readingFacts ) {
					listener.handleDocInfo(info);
				} else {
					Map dim = (Map) fact.get(KEY_DIM);
					if ( null != dim ) {
						String unit = (String) dim.get(KEY_UNIT);

						if ( null != unit ) {
							unit = XbrlUtils.access(units.get(unit), AccessCmd.Peek, null, "id");
							dim.put(KEY_XBRL_UNITREF, unit);
							dim.remove(KEY_UNIT);
						}

						String entity = (String) dim.remove(KEY_ENTITY);
						String period = (String) dim.remove(KEY_PERIOD);

						String ctx = XbrlUtils.access(contexts.get(entity + " " + period, entity, period), AccessCmd.Peek, null, "id");
						dim.put(KEY_XBRL_UNITREF, ctx);
					}

					listener.handleXbrlInfo(XbrlInfoType.Fact, fact);
				}
			}
		}
	}

	@Override
	public boolean endObjectEntry() throws ParseException, IOException {
		return true;
	}

	@Override
	public boolean primitive(Object value) throws ParseException, IOException {
		if ( null != dc ) {
			dc.putValue(value);
		}
		return true;
	}

	@Override
	public boolean startArray() throws ParseException, IOException {
		if ( null != dc ) {
			dc.open(ContainerType.Arr);
		}
		return true;
	}

	@Override
	public void startJSON() throws ParseException, IOException {
	}

	@Override
	public boolean startObject() throws ParseException, IOException {
		if ( null != dc ) {
			dc.open(ContainerType.Map);
		} else {
			if ( readingFacts ) {
				if ( null == fact ) {
					beginRead = fact = new HashMap();
					return true;
				} else {
					fact.clear();
					beginRead = fact;
				}
			}

			if ( null != beginRead ) {
				dc = new XbrlUtilsDataCollector();
				dc.init(beginRead);
				beginRead = null;
			}
		}
		return true;
	}

	@Override
	public boolean startObjectEntry(String key) throws ParseException, IOException {
		if ( null != dc ) {
			dc.setKey(key);
		} else {
			if ( "documentInfo".equals(key) ) {
				beginRead = info;
			} else if ( "facts".equals(key) ) {
				readingFacts = true;
			}
		}

		return true;
	}
}