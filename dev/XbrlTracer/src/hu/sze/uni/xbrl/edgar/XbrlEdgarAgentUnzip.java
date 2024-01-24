package hu.sze.uni.xbrl.edgar;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;

public class XbrlEdgarAgentUnzip extends DustAgent implements XbrlEdgarConsts {
	String path;
	
	@Override
	public MindHandle agentProcess() throws Exception {
		String name = Dust.access(MIND_TAG_CONTEXT_TARGET, MIND_TAG_ACCESS_PEEK, "", RESOURCE_ATT_URL_PATH);
		
		if ( name.endsWith(DUST_EXT_JSON) ) {
			ZipArchiveEntry zipArchiveEntry = Dust.access(MIND_TAG_CONTEXT_TARGET, MIND_TAG_ACCESS_PEEK, null, RESOURCE_ASP_STREAM);
		}

		return MIND_TAG_RESULT_READACCEPT;
	}
}
