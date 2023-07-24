package hu.sze.milab.xbrl;

import hu.sze.milab.dust.stream.json.DustStreamJsonApiAgentMessageReader;

public class XbrlSourceAgentFilingsXbrlOrg2 extends DustStreamJsonApiAgentMessageReader implements XbrlConsts {
	
	@Override
	protected MindHandle resolveDataItem(Object data) {
		return super.resolveDataItem(data);
	}

	@Override
	protected MindHandle resolveID(String dataId, MindHandle hType) {
		return super.resolveID(dataId, hType);
	}
}
