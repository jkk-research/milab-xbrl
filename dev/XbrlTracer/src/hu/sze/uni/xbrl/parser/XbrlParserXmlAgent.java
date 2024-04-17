package hu.sze.uni.xbrl.parser;

import hu.sze.milab.dust.DustAgent;

public class XbrlParserXmlAgent extends DustAgent implements XbrlParserConsts {
	

	@Override
	protected MindHandle agentBegin() throws Exception {		

		return MIND_TAG_RESULT_READACCEPT;
	}

	@Override
	protected MindHandle agentProcess() throws Exception {

		return MIND_TAG_RESULT_READACCEPT;
	}
	
	@Override
	protected MindHandle agentEnd() throws Exception {
		return MIND_TAG_RESULT_ACCEPT;
	}
}
