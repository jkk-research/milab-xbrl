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
import com.xbrldock.utils.XbrlDockUtils;

@SuppressWarnings("unchecked")
public class XbrlDockFormatJson implements XbrlDockFormatConsts, XbrlDockConsts.ReportFormatHandler {

	Comparator<Map.Entry<String, Object>> dimComp = new Comparator<Map.Entry<String, Object>>() {
		@Override
		public int compare(Entry<String, Object> o1, Entry<String, Object> o2) {
			int d = XbrlDockUtils.safeCompare(o1.getKey(), o2.getKey());
			return (0 != d) ? d : XbrlDockUtils.safeCompare(o1.getValue(), o2.getValue());
		}
	};

	@SuppressWarnings("unused")
	@Override
	public void loadReport(InputStream in, ReportDataHandler dataHandler) throws Exception {
		JSONParser p = new JSONParser();

		try (InputStreamReader ir = new InputStreamReader(in)) {

			Object root = p.parse(ir);

			Boolean canonicalValues = XbrlDockUtils.simpleGet(root, JsonKeys.documentInfo, JsonKeys.features,
					"xbrl:canonicalValues");
			String allowedDuplicates = XbrlDockUtils.simpleGet(root, JsonKeys.documentInfo, JsonKeys.features,
					"xbrl:allowedDuplicates");

			Collection<String> tx = XbrlDockUtils.simpleGet(root, JsonKeys.documentInfo, JsonKeys.taxonomy);
			if (null != tx) {
				for (String t : tx) {
					dataHandler.addTaxonomy(t);
				}
			}

			Map<String, String> ns = XbrlDockUtils.simpleGet(root, JsonKeys.documentInfo, JsonKeys.namespaces);
			if (null != ns) {
				for (Map.Entry<String, String> ne : ns.entrySet()) {
					dataHandler.addNamespace(ne.getKey(), ne.getValue());
				}
			}

			Map<String, String> unitIds = new TreeMap<>();
			ItemCreator<String> unitIDCreator = new ItemCreator<String>() {
				Map<XbrlFactKeys, Object> unitData = new TreeMap<>();

				@Override
				public String create(Object key, Object... hints) {
					unitData.clear();
					String[] info = ((String) key).split("/");

					if (1 < info.length) {
						unitData.put(XbrlFactKeys.unitNumerator, info[0]);
						unitData.put(XbrlFactKeys.unitDenominator, info[1]);
					} else {
						unitData.put(XbrlFactKeys.measure, info[0]);
					}
					unitData.put(XbrlFactKeys.unit, key);

					return dataHandler.processSegment(XbrlReportSegment.Unit, unitData);
				}
			};

			Map<String, String> ctxIds = new TreeMap<>();
			ItemCreator<String> ctxIDCreator = new ItemCreator<String>() {
				Map<XbrlFactKeys, Object> ctxData = new TreeMap<>();

				@Override
				public String create(Object key, Object... hints) {
					ctxData.clear();

					Map<String, Object> d = (Map<String, Object>) hints[0];
					ctxData.put(XbrlFactKeys.entity, d.remove(XbrlFactKeys.entity.name()));

					String period = (String) d.remove(XbrlFactKeys.period.name());
					ctxData.put(XbrlFactKeys.period, period);

					if (!XbrlDockUtils.isEmpty(period)) {
						String[] info = period.split("/");
						if (1 < info.length) {
							ctxData.put(XbrlFactKeys.startDate, info[0]);
							ctxData.put(XbrlFactKeys.endDate, info[1]);
						} else {
							ctxData.put(XbrlFactKeys.instant, info[0]);
						}
					}

					if (!d.isEmpty()) {
						ctxData.put(XbrlFactKeys.dimensions, d);
					}

					return dataHandler.processSegment(XbrlReportSegment.Context, ctxData);
				}
			};

			Map<XbrlFactKeys, Object> factData = new TreeMap<>();

			Map<String, Object> facts = XbrlDockUtils.simpleGet(root, JsonKeys.facts);
			for (Map.Entry<String, Object> fe : facts.entrySet()) {
				factData.clear();

				Map<String, Object> fd = (Map<String, Object>) fe.getValue();
				Map<String, Object> dim = (Map<String, Object>) fd.remove(XbrlFactKeys.dimensions.name());

				factData.put(XbrlFactKeys.id, fe.getKey());
				factData.put(XbrlFactKeys.value, fd.remove(XbrlFactKeys.value.name()));
				factData.put(XbrlFactKeys.concept, dim.remove(XbrlFactKeys.concept.name()));

				String unit = (String) dim.remove(XbrlFactKeys.unit.name());
				if (null == unit) {
					factData.put(XbrlFactKeys.language, dim.remove(XbrlFactKeys.language.name()));
				} else {
					factData.put(XbrlFactKeys.decimals, fd.remove(XbrlFactKeys.decimals.name()));

					String uid = XbrlDockUtils.safeGet(unitIds, unit, unitIDCreator);
					factData.put(XbrlFactKeys.unit, uid);
				}

				ArrayList<Map.Entry<String, Object>> e = new ArrayList<>(dim.entrySet());
				e.sort(dimComp);

				String dk = e.toString();
				String ctxId = XbrlDockUtils.safeGet(ctxIds, dk, ctxIDCreator, dim);

				factData.put(XbrlFactKeys.context, ctxId);

				dataHandler.processSegment(XbrlReportSegment.Fact, factData);
			}
		}
	}

}
