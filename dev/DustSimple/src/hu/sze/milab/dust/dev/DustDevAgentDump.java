package hu.sze.milab.dust.dev;

import hu.sze.milab.dust.DustConsts;
import hu.sze.milab.dust.DustMetaConsts;
import hu.sze.milab.dust.brain.DustBrain;

public class DustDevAgentDump implements DustMetaConsts, DustConsts.MindAgent {
	
	public MindHandle hTarget;
	public String prefix;

	@Override
	public MindStatus agentExecAction(MindAction action) throws Exception {
		DustBrain.dumpHandle(prefix + " " + action, hTarget);
		return MindStatus.Accept;
	}

}
