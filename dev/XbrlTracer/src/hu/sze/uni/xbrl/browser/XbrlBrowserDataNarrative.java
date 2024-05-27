package hu.sze.uni.xbrl.browser;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.mvel.DustMvelUtils;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.uni.xbrl.utils.XbrlUtilsFactAccess;

public class XbrlBrowserDataNarrative extends DustAgent implements XbrlBrowserConsts {
	
	@Override
	protected MindHandle agentProcess() throws Exception {
		MindHandle pool = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_SOURCE, MISC_ATT_CONN_SOURCE);

		XbrlUtilsFactAccess fa = new XbrlUtilsFactAccess(pool, "us-gaap");
		// Assets = Liabilities and Equity
		Object expr = DustMvelUtils.compile("getNum('us-gaap:Assets') == getNum('us-gaap:Liabilities') + getNum('us-gaap:StockholdersEquity')");

		Object deiDocumentPeriodEndDate = Dust.access(MindAccess.Peek, null,pool, XBRLDOCK_ATT_POOL_TAXONOMIES, "dei", XBRLDOCK_ATT_TAXONOMY_CONCEPTS,
				"DocumentPeriodEndDate");

		Map<Object, Object> reports = Dust.access(MindAccess.Peek, Collections.EMPTY_MAP, pool, XBRLDOCK_ATT_POOL_REPORTS);
		for (Map.Entry<Object, Object> eRep : reports.entrySet()) {
			Object report = eRep.getValue();
			Map<Object, Set<Object>> mapEvtCtx = Dust.access(MindAccess.Peek, Collections.EMPTY_MAP, report, XBRLDOCK_ATT_REPORT_CONTEXTTREE);

			String repPeriod = null;
			Map<Object, Object> repFacts = Dust.access(MindAccess.Peek, Collections.EMPTY_SET, report, MISC_ATT_CONN_MEMBERMAP);
			for (Object f : repFacts.values()) {
				Object con = Dust.access(MindAccess.Peek, null, f, XBRLDOCK_ATT_FACT_CONCEPT);
				if ( deiDocumentPeriodEndDate == con ) {
					repPeriod = Dust.access(MindAccess.Peek, null, f, TEXT_ATT_TOKEN);
					break;
				}
			}

			boolean found = false;

			for (Map.Entry<Object, Set<Object>> eec : mapEvtCtx.entrySet()) {
				String ctxPeriod = Dust.access(MindAccess.Peek, "?", eec.getKey(), TEXT_ATT_TOKEN);

				for (Object ctx : eec.getValue()) {
					String ctxId = Dust.access(MindAccess.Peek, "?", ctx, TEXT_ATT_TOKEN);
					Set<Object> facts = Dust.access(MindAccess.Peek, null, ctx, XBRLDOCK_ATT_CONTEXT_FACTS);
					if ( null != facts ) {
						fa.reset(facts);

						Object ret = DustMvelUtils.evalCompiled(expr, fa);

						if ( fa.isValid() ) {
							if ( DustUtils.isEqual(repPeriod, ctxPeriod) ) {
								found = true;
								Dust.log(EVENT_TAG_TYPE_TRACE, eRep.getKey(), repPeriod, ctxPeriod, ret, fa.getAsked(), ctxId);
							}
						}
					}
				}				
			}
			if ( !found ) {
				Dust.log(EVENT_TAG_TYPE_TRACE, eRep.getKey(), repPeriod, "Balance not found");
			}
		}
		
		return super.agentEnd();
	}

}
