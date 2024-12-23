package hu.sze.uni.xbrl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.simple.JSONValue;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.dev.DustDevUtils;
import hu.sze.milab.dust.mvel.DustMvelConsts;
import hu.sze.milab.dust.utils.DustUtils;

public class XbrlPoolLoaderAgent extends DustAgent implements XbrlConsts, DustMvelConsts {

	Pattern pt = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
	DustDevCounter reps = new DustDevCounter("reports", false);

	@SuppressWarnings("unchecked")
	@Override
	protected MindHandle agentProcess() throws Exception {
		MindHandle pool = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_TARGET);

		DustUtils.EnumMap values = new DustUtils.EnumMap(Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, MISC_ATT_CONN_MEMBERMAP), true, FactFldCommon.class, FactFldData.class,
				FactFldText.class);

		Map<Object, Object> tm = new HashMap<>();
		String id;

		String repId = id = values.get(FactFldCommon.File);
		MindHandle report = getItem(tm, pool, XBRLDOCK_ATT_POOL_REPORTS, id, XBRLDOCK_ASP_REPORT);
		reps.add(repId);

		String factId = id = values.get(FactFldCommon.FactId);
		MindHandle fact = getItem(tm, report, MISC_ATT_CONN_MEMBERMAP, id, XBRLDOCK_ASP_FACT);
		Dust.access(MindAccess.Set, report, fact, MISC_ATT_CONN_OWNER);

		id = values.get(FactFldCommon.EntityId);
		MindHandle entity = getItem(tm, pool, XBRLDOCK_ATT_POOL_ENTITIES, id, XBRLDOCK_ASP_ENTITY);

		id = values.get(FactFldCommon.CtxId);
		MindHandle ctx = Dust.access(MindAccess.Peek, null, report, XBRLDOCK_ATT_REPORT_CONTEXTS, id);
		MindHandle event;
		if ( null == ctx ) {
			ctx = getItem(tm, report, XBRLDOCK_ATT_REPORT_CONTEXTS, id, XBRLDOCK_ASP_CONTEXT);

			Dust.access(MindAccess.Set, entity, ctx, XBRLDOCK_ATT_CONTEXT_ENTITY);

			String d1 = values.get(FactFldCommon.Instant);
			String d2 = null;
			String eventId = d1;
			if ( DustUtils.isEmpty(eventId) ) {
				eventId = (d1 = values.get(FactFldCommon.StartDate)) + "/" + (d2 = values.get(FactFldCommon.EndDate));
			}

			MindHandle cal = Dust.access(MindAccess.Peek, null, pool, XBRLDOCK_ATT_POOL_CALENDAR);
			event = Dust.access(MindAccess.Peek, null, cal, MISC_ATT_CONN_MEMBERMAP, eventId);
			if ( null == event ) {
				event = getItem(tm, cal, MISC_ATT_CONN_MEMBERMAP, eventId, EVENT_ASP_EVENT);

				setTime(repId, factId, event, d1, EVENT_ATT_EVENT_START);
				if ( null != d2 ) {
					setTime(repId, factId, event, d2, EVENT_ATT_EVENT_END);
				}
			}

			Dust.access(MindAccess.Set, event, ctx, XBRLDOCK_ATT_CONTEXT_EVENT);

			Dust.access(MindAccess.Insert, ctx, report, XBRLDOCK_ATT_REPORT_CONTEXTTREE, event);

			String dims = values.get(FactFldCommon.Dimensions);
			if ( !DustUtils.isEmpty(dims) ) {
				Map<String, String> dm = (Map<String, String>) JSONValue.parse(dims);
				for (Map.Entry<String, String> de : dm.entrySet()) {
					String dimId = de.getKey();
					String taxId = DustUtils.cutPostfix(dimId, ":");
					MindHandle tax = getItem(tm, pool, XBRLDOCK_ATT_POOL_TAXONOMIES, taxId, XBRLDOCK_ASP_TAXONOMY);
					MindHandle hDim = getItem(null, tax, XBRLDOCK_ATT_TAXONOMY_DIMENSIONS, DustUtils.getPostfix(dimId, ":"), MIND_ASP_TAG);

					String dimItemId = de.getValue();
					taxId = DustUtils.cutPostfix(dimItemId, ":");
					tax = getItem(tm, pool, XBRLDOCK_ATT_POOL_TAXONOMIES, taxId, XBRLDOCK_ASP_TAXONOMY);
					MindHandle hDimItem = getItem(null, tax, XBRLDOCK_ATT_TAXONOMY_DIMITEMS, DustUtils.getPostfix(dimItemId, ":"), MIND_ASP_TAG);

					Dust.access(MindAccess.Set, hDim, hDimItem, MISC_ATT_CONN_PARENT);
					DustDevUtils.setTag(ctx, hDim, hDimItem);
				}
			} else {
				DustDevUtils.setTag(ctx, MISC_TAG_ROOT);
			}
		} else {
			event = Dust.access(MindAccess.Peek, null, ctx, XBRLDOCK_ATT_CONTEXT_EVENT);
		}

		Dust.access(MindAccess.Insert, fact, ctx, XBRLDOCK_ATT_CONTEXT_FACTS);

		String ns = values.get(FactFldCommon.TagNamespace);
		MindHandle taxonomy = getItem(tm, pool, XBRLDOCK_ATT_POOL_TAXONOMIES, ns, XBRLDOCK_ASP_TAXONOMY);

		String conceptId = values.get(FactFldCommon.TagId);
		MindHandle concept = getItem(tm, taxonomy, XBRLDOCK_ATT_TAXONOMY_CONCEPTS, conceptId, XBRLDOCK_ASP_CONCEPT);
		Dust.access(MindAccess.Set, concept, fact, XBRLDOCK_ATT_FACT_CONCEPT);

		Map<Object, Object> fm = Dust.access(MindAccess.Peek, Collections.EMPTY_MAP, MIND_TAG_CONTEXT_SELF, XBRLDOCK_ATT_POOLLOADER_FACTMAP);
		for (Map.Entry<Object, Object> fe : fm.entrySet()) {
			String val = values.get(fe.getKey());
			if ( !DustUtils.isEmpty(val) ) {
				Dust.access(MindAccess.Set, val, fact, fe.getValue());
			}
		}

		String val = values.get(FactFldData.OrigValue);

		if ( !DustUtils.isEmpty(val) ) {
			Dust.access(MindAccess.Set, val, fact, DEV_ATT_HINT);

			FactType ft = FactType.valueOf(values.get(FactFldCommon.Type));
			switch ( ft ) {
			case Numeric:
				val = values.get(FactFldData.Value);
				Double dv = Double.valueOf(val);
				Dust.access(MindAccess.Set, dv, fact, MISC_ATT_VARIANT_VALUE);

				id = values.get(FactFldData.UnitId);
				MindHandle unit = getItem(tm, pool, XBRLDOCK_ATT_POOL_MEASUREUNITS, id, XBRLDOCK_ASP_MEASUREUNIT);
				Dust.access(MindAccess.Set, unit, fact, XBRLDOCK_ATT_FACT_MEASUREUNIT);
				break;
			case String:
				Dust.access(MindAccess.Set, val, fact, TEXT_ATT_TOKEN);
				break;
			case Text:
				Dust.access(MindAccess.Set, val, fact, TEXT_ATT_PLAIN_TEXT);
				break;
			}

			String cid = ns + ":" + conceptId;
			Object ta = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, cid);
			if ( null != ta ) {
				Object t = tm.get(ta);
				if ( null != t ) {
					Dust.access(MindAccess.Set, val, t, MISC_ATT_GEN_EXTMAP, cid);
				}
			}
		}
		return MIND_TAG_RESULT_READACCEPT;
	}

	@Override
	protected MindHandle agentEnd() throws Exception {

		Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_TARGET);

		return super.agentEnd();
	}

	public boolean setTime(String repId, String factId, MindHandle event, String d1, MindHandle hEvtMember) {
		if ( !pt.matcher(d1).matches() ) {

			Dust.log(EVENT_TAG_TYPE_WARNING, "Weird time value", repId, factId, d1);

			return false;
		}

//		if ( d1.startsWith("203") ) {
//			Dust.log(EVENT_TAG_TYPE_WARNING, "High time value", repId, factId, d1);
//
//		}

		MindHandle time = DustDevUtils.newHandle(XBRLTEST_UNIT, EVENT_ASP_TIME, d1);
		Dust.access(MindAccess.Set, d1, time, TEXT_ATT_TOKEN);
		Dust.access(MindAccess.Set, time, event, hEvtMember);

		return true;
	}

	public MindHandle getItem(Map<Object, Object> tm, MindHandle parent, MindHandle member, String id, MindHandle asp) {
		MindHandle ret = Dust.access(MindAccess.Peek, null, parent, member, id);
		if ( null == ret ) {
			ret = DustDevUtils.newHandle(XBRLTEST_UNIT, asp, "Report " + id);
			Dust.access(MindAccess.Set, id, ret, TEXT_ATT_TOKEN);
			Dust.access(MindAccess.Set, ret, parent, member, id);
		}
		if ( null != tm ) {
			tm.put(asp, ret);
		}

		return ret;
	}

}
