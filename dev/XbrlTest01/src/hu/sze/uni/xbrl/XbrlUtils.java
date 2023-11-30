package hu.sze.uni.xbrl;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsData;
import hu.sze.milab.dust.utils.DustUtilsFactory;
import hu.sze.milab.dust.utils.DustUtilsConsts.DustCloseableWalker;
import hu.sze.milab.xbrl.tools.XbrlToolsFactReader;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlUtils implements XbrlConsts {
	public static final Map<String, String> XML_TRANSLATE = new HashMap<>();

	public static int MAX_TAX_COL_COUNT = 600;

	static {
		XML_TRANSLATE.put("contextRef", "period");
		XML_TRANSLATE.put("unitRef", "unit");
		XML_TRANSLATE.put("name", "concept");
	}

	public static <RetType> RetType access(Object root, AccessCmd cmd, Object val, Object... path) {
		Object ret = null;

		Object curr = root;
		Object prev = null;
		Object lastKey = null;

		for (Object p : path) {
			prev = curr;
			lastKey = p;

			if ( curr instanceof ArrayList ) {
				curr = ((ArrayList) curr).get((Integer) p);
			} else if ( curr instanceof Map ) {
				curr = ((Map) curr).get(p);
			} else {
				curr = null;
			}

			if ( null == curr ) {
				break;
			}
		}

		ret = (null == curr) ? val : curr;

		if ( (cmd == AccessCmd.Set) && (null != lastKey) ) {
			if ( prev instanceof Map ) {
				((Map) prev).put(lastKey, val);
			}
		}

		return (RetType) ret;
	}

	public static void exportConceptCoverage(PrintWriter psOut, XbrlFilingManager filings, Iterable<String> ids, Iterable<String> concepts) throws Exception {
		DustDevCounter countConcepts = new DustDevCounter(true);
		DustUtilsFactory<String, DustDevCounter> data = new DustUtilsFactory<String, DustDevCounter>(true) {
			@Override
			protected DustDevCounter create(String key, Object... hints) {
				return new DustDevCounter(true);
			}
		};
		DustUtilsData.Indexer<String> expCols = new DustUtilsData.Indexer<>();

		DustUtilsData.TableReader tr = null;
		XbrlToolsFactReader fr = new XbrlToolsFactReader();

		Set<String> cols = new TreeSet<>();

		int repIdx = 0;

		psOut.print("Concept\tTotalCount");

		for (String line : ids) {
			String id = line.trim();
			++repIdx;

			cols.clear();
			tr = filings.getTableReader(id);
			fr.setTableReader(tr);

			try (DustCloseableWalker<String[]> repFacts = filings.getFacts(id)) {
				for (String[] rf : repFacts) {
					String taxonomy = tr.get(rf, "Taxonomy");
					if ( !DustUtils.isEqual("ifrs-full", taxonomy) ) {
						continue;
					}

					String concept = tr.get(rf, "Concept");

					fr.readFact(rf);

					String axisId = fr.getAxisId();
					countConcepts.add(concept + axisId);

					String cellId = repIdx + fr.getContextId();
					cols.add(cellId);

					data.get(concept).add(cellId);
				}
			}

			boolean first = true;
			for (String cellId : cols) {
				psOut.print("\t");
				if ( first ) {
					first = false;
					psOut.print(repIdx + ": " + id);
				}
				expCols.getIndex(cellId);
			}

			if ( MAX_TAX_COL_COUNT < expCols.getSize() ) {
				break;
			}
		}

		int cc = 0;
		for (String col : expCols.keys()) {
			if ( 0 == (cc++) ) {
				psOut.println();
				psOut.print("\t");
			}

			psOut.print("\t");
			psOut.print(col);
		}

		psOut.println();
		psOut.flush();

		Long[] vals = new Long[cc];

		StringBuilder sbEmpty = new StringBuilder();
		for (int i = 0; i < cc; ++i) {
			sbEmpty.append("\t");
		}
		String empty = sbEmpty.toString();

		long ts = System.currentTimeMillis();

		Iterable<String> rows = (null == concepts) ? data.keys() : concepts;

		for (String k : rows) {
			psOut.print(k);

			psOut.print("\t");
			psOut.print(DustUtils.toString(countConcepts.peek(k)));

			DustDevCounter dc = data.peek(k);

			if ( null == dc ) {
				psOut.println(empty);
			} else {
				Arrays.fill(vals, null);
				for (Map.Entry<Object, Long> e : dc) {
					String cellId = (String) e.getKey();
					int colIdx = expCols.peekIndex(cellId);
					vals[colIdx] = e.getValue();
				}

				for (int i = 0; i < cc; ++i) {
					psOut.print("\t");
					Long l = vals[i];
					if ( null != l ) {
						psOut.print(l);
					}
				}
				psOut.println();
			}
			psOut.flush();
		}

		Dust.dumpObs("time", System.currentTimeMillis() - ts, "msec.");

		// countConcepts.dump("Concept stats");
		//
		// Dust.dumpObs("Collected data");
		// for (String k : data.keys()) {
		// DustDevCounter dc = data.peek(k);
		//
		// Dust.dumpObs(k);
		// for (Map.Entry<Object, Long> e : dc) {
		// String cellId = (String) e.getKey();
		// int colIdx = expCols.peekIndex(cellId);
		//
		// Dust.dumpObs(" ", cellId, colIdx, e.getValue());
		// }
		// }

	}

}