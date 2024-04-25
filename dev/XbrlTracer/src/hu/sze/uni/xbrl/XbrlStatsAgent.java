package hu.sze.uni.xbrl;

import java.util.Map;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.utils.DustUtils;

@SuppressWarnings("rawtypes")
public class XbrlStatsAgent extends DustAgent implements XbrlConsts {
	
	DustDevCounter files = new DustDevCounter("Files", true);

	@Override
	protected MindHandle agentBegin() throws Exception {
		Dust.access(MindAccess.Set, new DustDevCounter("NSE taxonomies", true), MIND_TAG_CONTEXT_SELF, DUST_ATT_IMPL_DATA);
		return MIND_TAG_RESULT_READACCEPT;
	}

	@Override
	protected MindHandle agentProcess() throws Exception {
		DustDevCounter cnt = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, DUST_ATT_IMPL_DATA);
		
		Map values = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, MISC_ATT_CONN_MEMBERMAP);

		Object ns = values.get("TagNamespace");
		Object tag = values.get("TagId");
		Object type = values.get("Type");
		Object dim = values.get("Dimensions");
		Object file = values.get("File");
		
		String key = ns + "\t" + tag + "\t" + type;
		cnt.add(key + "\t[All]");
		
		if ( DustUtils.isEmpty((String)dim) ) {
			cnt.add(key + "\t[Root]");
		}
		
		files.add(file + "\t[All]");
		files.add(file + "\t" + type);
		
		return MIND_TAG_RESULT_READACCEPT;
	}

	@Override
	protected MindHandle agentEnd() throws Exception {
		DustDevCounter cnt = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, DUST_ATT_IMPL_DATA);
		
		System.out.println(cnt.toString());
		System.out.println("----");
		System.out.println(files.toString());
		
		return MIND_TAG_RESULT_ACCEPT;
	}
}
