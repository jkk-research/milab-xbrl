package com.xbrldock.poc.format;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.simple.parser.JSONParser;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.format.XbrlDockFormatConsts;
import com.xbrldock.utils.XbrlDockUtils;

@SuppressWarnings("unchecked")
public class XbrlDockFormatJson implements XbrlDockFormatConsts, XbrlDockConsts.GenAgent {

	Comparator<Map.Entry<String, Object>> dimComp = new Comparator<Map.Entry<String, Object>>() {
		@Override
		public int compare(Entry<String, Object> o1, Entry<String, Object> o2) {
			int d = XbrlDockUtils.safeCompare(o1.getKey(), o2.getKey());
			return (0 != d) ? d : XbrlDockUtils.safeCompare(o1.getValue(), o2.getValue());
		}
	};
	
	@Override
	public Object process(String cmd, Map<String, Object> params) throws Exception {
		switch ( cmd ) {
		case XDC_CMD_GEN_Process:
			loadReport((InputStream) params.get(XDC_GEN_TOKEN_source), (GenAgent) params.get(XDC_GEN_TOKEN_target));
			break;
		}
		return null;
	}

	@SuppressWarnings("unused")
	public void loadReport(InputStream in, GenAgent dataHandler) throws Exception {
		JSONParser p = new JSONParser();

		try (InputStreamReader ir = new InputStreamReader(in)) {

			Object root = p.parse(ir);

			Boolean canonicalValues = XbrlDockUtils.simpleGet(root, XDC_FMTJSON_TOKEN_documentInfo, XDC_FMTJSON_TOKEN_features,
					"xbrl:canonicalValues");
			String allowedDuplicates = XbrlDockUtils.simpleGet(root, XDC_FMTJSON_TOKEN_documentInfo, XDC_FMTJSON_TOKEN_features,
					"xbrl:allowedDuplicates");

			Collection<String> tx = XbrlDockUtils.simpleGet(root, XDC_FMTJSON_TOKEN_documentInfo, XDC_FMTJSON_TOKEN_taxonomy);
			if (null != tx) {
				for (String t : tx) {
					dataHandler.process(XDC_CMD_REP_ADD_SCHEMA, XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, t));
				}
			}

			Map<String, String> ns = XbrlDockUtils.simpleGet(root, XDC_FMTJSON_TOKEN_documentInfo, XDC_FMTJSON_TOKEN_namespaces);
			if (null != ns) {
				for (Map.Entry<String, String> ne : ns.entrySet()) {
					dataHandler.process(XDC_CMD_REP_ADD_NAMESPACE, XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, ne.getKey(), XDC_EXT_TOKEN_value , ne.getValue()));
				}
			}

			Map<String, String> unitIds = new TreeMap<>();
			ItemCreator<String> unitIDCreator = new ItemCreator<String>() {
				Map<String, Object> unitData = new TreeMap<>();

				@Override
				public String create(Object key, Object... hints) {
					unitData.clear();
					String[] info = ((String) key).split("/");

					if (1 < info.length) {
						unitData.put(XDC_FACT_TOKEN_unitNumerator, info[0]);
						unitData.put(XDC_FACT_TOKEN_unitDenominator, info[1]);
					} else {
						unitData.put(XDC_FACT_TOKEN_measure, info[0]);
					}
					unitData.put(XDC_FACT_TOKEN_unit, key);
					
					try {
						return (String) dataHandler.process(XDC_REP_SEG_Unit, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, unitData));
					} catch (Exception e) {
						return XbrlDockException.wrap(e, "Creating unit", key);
					}
				}
			};

			Map<String, String> ctxIds = new TreeMap<>();
			ItemCreator<String> ctxIDCreator = new ItemCreator<String>() {
				Map<String, Object> ctxData = new TreeMap<>();

				@Override
				public String create(Object key, Object... hints) {
					ctxData.clear();

					Map<String, Object> d = (Map<String, Object>) hints[0];
					ctxData.put(XDC_FACT_TOKEN_entity, d.remove(XDC_FACT_TOKEN_entity));

					String period = (String) d.remove(XDC_FACT_TOKEN_period);
					ctxData.put(XDC_FACT_TOKEN_period, period);

					if (!XbrlDockUtils.isEmpty(period)) {
						String[] info = period.split("/");
						if (1 < info.length) {
							ctxData.put(XDC_EXT_TOKEN_startDate, info[0]);
							ctxData.put(XDC_EXT_TOKEN_endDate, info[1]);
						} else {
							ctxData.put(XDC_FACT_TOKEN_instant, info[0]);
						}
					}

					if (!d.isEmpty()) {
						ctxData.put(XDC_FACT_TOKEN_dimensions, d);
					}

					try {
						return (String) dataHandler.process(XDC_REP_SEG_Context, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, ctxData));
					} catch (Exception e) {
						return XbrlDockException.wrap(e, "Creating context", key);
					}
				}
			};

			Map<String, Object> factData = new TreeMap<>();

			Map<String, Object> facts = XbrlDockUtils.simpleGet(root, XDC_FMTJSON_TOKEN_facts);
			for (Map.Entry<String, Object> fe : facts.entrySet()) {
				factData.clear();

				Map<String, Object> fd = (Map<String, Object>) fe.getValue();
				Map<String, Object> dim = (Map<String, Object>) fd.remove(XDC_FACT_TOKEN_dimensions);

				factData.put(XDC_EXT_TOKEN_id, fe.getKey());
				factData.put(XDC_EXT_TOKEN_value, fd.remove(XDC_EXT_TOKEN_value));
				factData.put(XDC_FACT_TOKEN_concept, dim.remove(XDC_FACT_TOKEN_concept));

				String unit = (String) dim.remove(XDC_FACT_TOKEN_unit);
				if (null == unit) {
					factData.put(XDC_FACT_TOKEN_language, dim.remove(XDC_FACT_TOKEN_language));
				} else {
					factData.put(XDC_FACT_TOKEN_decimals, fd.remove(XDC_FACT_TOKEN_decimals));

					String uid = XbrlDockUtils.safeGet(unitIds, unit, unitIDCreator);
					factData.put(XDC_FACT_TOKEN_unit, uid);
				}

				ArrayList<Map.Entry<String, Object>> e = new ArrayList<>(dim.entrySet());
				e.sort(dimComp);

				String dk = e.toString();
				String ctxId = XbrlDockUtils.safeGet(ctxIds, dk, ctxIDCreator, dim);

				factData.put(XDC_FACT_TOKEN_context, ctxId);

				dataHandler.process(XDC_REP_SEG_Fact, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, factData));
			}
		}
	}

}
