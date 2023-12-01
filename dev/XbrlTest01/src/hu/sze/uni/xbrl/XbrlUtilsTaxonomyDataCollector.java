package hu.sze.uni.xbrl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsData;
import hu.sze.milab.dust.utils.DustUtilsFactory;
import hu.sze.milab.dust.utils.DustUtilsConsts.DustCloseableWalker;
import hu.sze.milab.xbrl.tools.XbrlToolsCurrencyConverter;
import hu.sze.milab.xbrl.tools.XbrlToolsFactReader;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlUtilsTaxonomyDataCollector {
	int cutColCount;
	String taxonomy;
	DustDevCounter countConcepts = new DustDevCounter(true);
	DustUtilsData.Indexer<String> expCols = new DustUtilsData.Indexer<>();

	ArrayList<String> headReport = new ArrayList<>();

	DustUtilsFactory<String, Map> values = new DustUtilsFactory.Simple<String, Map>(true, HashMap.class);

	public XbrlUtilsTaxonomyDataCollector(XbrlFilingManager filings, Iterable<String> ids, int cutColCount, String taxonomy, Collection<String> concepts, XbrlToolsCurrencyConverter cCvt)
			throws Exception {
		this.cutColCount = cutColCount;
		this.taxonomy = taxonomy;

		DustUtilsData.TableReader tr = null;
		XbrlToolsFactReader fr = new XbrlToolsFactReader();

		Set<String> cols = new TreeSet<>();
		Map mFact = (null == cCvt) ? null : new HashMap<>();

		int repIdx = 0;

		for (String line : ids) {
			String id = line.trim();
			++repIdx;

			cols.clear();
			tr = filings.getTableReader(id);

			try (DustCloseableWalker<String[]> repFacts = filings.getFacts(id)) {
				fr.setTableReader(tr);

				for (String[] rf : repFacts) {
					String tx = tr.get(rf, "Taxonomy");
					if ( !DustUtils.isEqual(taxonomy, tx) ) {
						continue;
					}

					String concept = tr.get(rf, "Concept");
					if ( (null != concepts) && !concepts.contains(concept) ) {
						continue;
					}

					fr.readFact(rf);

					if ( fr.isPrimary() ) {
						String axisId = fr.getAxisId();
						countConcepts.add(concept + axisId);

						String cellId = repIdx + fr.getContextId();
						cols.add(cellId);

						String value = tr.get(rf, "Value");
						if ( null != cCvt ) {
//							if ("ShareOfProfitLossOfAssociatesAndJointVenturesAccountedForUsingEquityMethod".equals(concept)) {
//								DustUtils.breakpoint();
//							}
							tr.get(rf, mFact);
							Double cv = cCvt.optConvert(mFact);

							if ( (null != cv) && !cv.isNaN() ) {
								value = (String) mFact.get("cvtValue");
							}
						}

						values.get(concept).put(cellId, value);
					}
				}
			}

			boolean first = true;
			for (String cellId : cols) {
				if ( first ) {
					first = false;
					headReport.add(repIdx + ": " + id);
				} else {
					headReport.add("");
				}
				expCols.getIndex(cellId);
			}

			if ( cutColCount < expCols.getSize() ) {
				break;
			}
		}
	}

	public Long getTotalCount(String concept) {
		return countConcepts.peek(concept);
	}

	public int getColumnCount() {
		return expCols.getSize();
	}

	public String getColumnHead(int colIdx, boolean context) {
		return context ? expCols.getKey(colIdx) : headReport.get(colIdx);
	}

	public String getValue(int colIdx, String concept) {
		Map<String, String> m = values.peek(concept);
		
		if (null == m) {
			return "";
		} else {
			String ctxId = expCols.getKey(colIdx);
			return m.getOrDefault(ctxId, "");
		}
	}
}