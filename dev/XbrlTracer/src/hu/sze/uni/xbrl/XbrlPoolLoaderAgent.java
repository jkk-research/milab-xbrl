package hu.sze.uni.xbrl;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.dev.DustDevUtils;
import hu.sze.milab.dust.utils.DustUtils;

public class XbrlPoolLoaderAgent extends DustAgent implements XbrlConsts {

	@Override
	protected MindHandle agentProcess() throws Exception {
		MindHandle pool = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_TARGET);

		DustUtils.EnumMap values = new DustUtils.EnumMap(Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, MISC_ATT_CONN_MEMBERMAP), FactFldCommon.class, FactFldData.class, FactFldText.class);

		String id;
		
		id = values.get(FactFldCommon.File);
		MindHandle report = Dust.access(MindAccess.Peek, null, pool, XBRLDOCK_ATT_POOL_REPORTS, id);
		if ( null == report ) {
			report = DustDevUtils.newHandle(pool, XBRLDOCK_ASP_REPORT, "Report " + id);
			Dust.access(MindAccess.Set, id, report, TEXT_ATT_TOKEN);
			Dust.access(MindAccess.Set, report, pool, XBRLDOCK_ATT_POOL_REPORTS, id);
		}

		id = values.get(FactFldCommon.FactId);
		MindHandle fact = Dust.access(MindAccess.Peek, null, report, MISC_ATT_CONN_MEMBERMAP, id);
		if ( null == fact ) {
			fact = DustDevUtils.newHandle(report, XBRLDOCK_ASP_FACT, "Fact " + id);
			Dust.access(MindAccess.Set, id, fact, TEXT_ATT_TOKEN);
			Dust.access(MindAccess.Set, report, pool, XBRLDOCK_ATT_POOL_REPORTS, id);
		}
		

		id = values.get(FactFldCommon.CtxId);
		MindHandle ctx = Dust.access(MindAccess.Peek, null, report, XBRLDOCK_ATT_REPORT_CONTEXTS, id);
		MindHandle event;
		if ( null == ctx ) {
			ctx = DustDevUtils.newHandle(pool, XBRLDOCK_ASP_CONTEXT, "Context " + id);
			Dust.access(MindAccess.Set, id, ctx, TEXT_ATT_TOKEN);
			Dust.access(MindAccess.Set, ctx, report, XBRLDOCK_ATT_REPORT_CONTEXTS, id);
			
			String d1 = values.get(FactFldCommon.Instant);
			String d2 = null;
			String eventId = d1;
			if ( null == eventId ) {
				eventId = (d1 = values.get(FactFldCommon.StartDate)) + " - " + (d2 = values.get(FactFldCommon.EndDate));
			}
			
			event = Dust.access(MindAccess.Peek, null, pool, XBRLDOCK_ATT_POOL_CALENDAR, MISC_ATT_CONN_MEMBERMAP, eventId);
			if ( null == event ) {
				event = DustDevUtils.newHandle(pool, EVENT_ASP_EVENT, "Event " + eventId);
				Dust.access(MindAccess.Set, eventId, event, TEXT_ATT_TOKEN);
				Dust.access(MindAccess.Set, event, pool, XBRLDOCK_ATT_POOL_CALENDAR, MISC_ATT_CONN_MEMBERMAP, eventId);
				
				MindHandle time;
				time = DustDevUtils.newHandle(pool, EVENT_ASP_EVENT, d1);
				Dust.access(MindAccess.Set, d1, time, TEXT_ATT_TOKEN);
				Dust.access(MindAccess.Set, time, event, EVENT_ATT_EVENT_START);
				
				if ( null != d2 ) {
					time = DustDevUtils.newHandle(pool, EVENT_ASP_EVENT, d2);
					Dust.access(MindAccess.Set, d2, time, TEXT_ATT_TOKEN);
					Dust.access(MindAccess.Set, time, event, EVENT_ATT_EVENT_END);					
				}
			}
			
			Dust.access(MindAccess.Set, event, ctx, XBRLDOCK_ATT_CONTEXT_EVENT);
		} else {
			event = Dust.access(MindAccess.Peek, null, ctx, XBRLDOCK_ATT_CONTEXT_EVENT);
		}

		return MIND_TAG_RESULT_READACCEPT;
	}

}
