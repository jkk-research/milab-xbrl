package com.xbrldock.poc.format;

import java.io.Reader;
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
	public void loadReport(Reader in, ReportDataHandler dataHandler) throws Exception {
		JSONParser p = new JSONParser();

		Object root = p.parse(in);

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
			Map<XbrlToken, Object> unitData = new TreeMap<>();

			@Override
			public String create(Object key, Object... hints) {
				unitData.clear();
				String[] info = ((String) key).split("/");

				if (1 < info.length) {
					unitData.put(XbrlToken.unitNumerator, info[0]);
					unitData.put(XbrlToken.unitDenominator, info[1]);
				} else {
					unitData.put(XbrlToken.measure, info[0]);
				}
				unitData.put(XbrlToken.unit, key);

				return dataHandler.processSegment(ReportSegment.Unit, unitData);
			}
		};

		Map<String, String> ctxIds = new TreeMap<>();
		ItemCreator<String> ctxIDCreator = new ItemCreator<String>() {
			Map<XbrlToken, Object> ctxData = new TreeMap<>();

			@Override
			public String create(Object key, Object... hints) {
				ctxData.clear();

				Map<String, Object> d = (Map<String, Object>) hints[0];
				ctxData.put(XbrlToken.entity, d.remove(XbrlToken.entity.name()));

				String period = (String) d.remove(XbrlToken.period.name());
				ctxData.put(XbrlToken.period, period);

				if (!XbrlDockUtils.isEmpty(period)) {
					String[] info = period.split("/");
					if (1 < info.length) {
						ctxData.put(XbrlToken.startDate, info[0]);
						ctxData.put(XbrlToken.endDate, info[1]);
					} else {
						ctxData.put(XbrlToken.instant, info[0]);
					}
				}

				if (!d.isEmpty()) {
					ctxData.put(XbrlToken.dimensions, d);
				}

				return dataHandler.processSegment(ReportSegment.Context, ctxData);
			}
		};

		Map<XbrlToken, Object> factData = new TreeMap<>();

		Map<String, Object> facts = XbrlDockUtils.simpleGet(root, JsonKeys.facts);
		for (Map.Entry<String, Object> fe : facts.entrySet()) {
			factData.clear();

			Map<String, Object> fd = (Map<String, Object>) fe.getValue();
			Map<String, Object> dim = (Map<String, Object>) fd.remove(XbrlToken.dimensions.name());

			factData.put(XbrlToken.id, fe.getKey());
			factData.put(XbrlToken.value, fd.remove(XbrlToken.value.name()));
			factData.put(XbrlToken.concept, dim.remove(XbrlToken.concept.name()));

			String unit = (String) dim.remove(XbrlToken.unit.name());
			if (null == unit) {
				factData.put(XbrlToken.language, dim.remove(XbrlToken.language.name()));
			} else {
				factData.put(XbrlToken.decimals, fd.remove(XbrlToken.decimals.name()));

				String uid = XbrlDockUtils.safeGet(unitIds, unit, unitIDCreator);
				factData.put(XbrlToken.unit, uid);
			}

			ArrayList<Map.Entry<String, Object>> e = new ArrayList<>(dim.entrySet());
			e.sort(dimComp);

			String dk = e.toString();
			String ctxId = XbrlDockUtils.safeGet(ctxIds, dk, ctxIDCreator, dim);

			factData.put(XbrlToken.context, ctxId);

			dataHandler.processSegment(ReportSegment.Fact, factData);
		}
	}

}
