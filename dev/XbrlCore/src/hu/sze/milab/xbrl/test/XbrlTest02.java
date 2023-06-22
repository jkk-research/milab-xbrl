package hu.sze.milab.xbrl.test;

import java.io.File;
import java.io.PrintStream;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.brain.DustImpl;
import hu.sze.milab.dust.dev.DustDevAgentXmlWriter;
import hu.sze.milab.dust.stream.DustStreamConsts;
import hu.sze.milab.dust.stream.DustStreamXmlAgent;

public class XbrlTest02 implements DustStreamConsts {

	public static void main(String[] args) throws Exception {
		DustImpl.main(args);
		
		String data = System.getProperty("user.home") + "/work/xbrl/data";
		File dataDir = new File(data);
		
		File out = new File("out");
		out.mkdirs();
		
		DustStreamXmlAgent aXml = new DustStreamXmlAgent();
		
		aXml.agentExecAction(MindAction.Init);
		
		MindHandle target = Dust.createHandle();
		Dust.access(MIND_ATT_AGENT_SELF, MindAccess.Set, target, MISC_ATT_CONN_TARGET);
		
//		DustDevAgentDump dump = new DustDevAgentDump();
//		dump.prefix = "Commit dump";
		
		DustDevAgentXmlWriter dump = new DustDevAgentXmlWriter();
		dump.hTarget = target;
//		dump.ps = System.out;
		dump.ps = new PrintStream(new File(out, "xmlTest.txt"));

		MindHandle listener = Dust.createHandle();
		Dust.access(listener, MindAccess.Set, dump, BRAIN_ATT_ACTOR_INSTANCE);
		Dust.access(target, MindAccess.Set, listener, MIND_ATT_KNOWLEDGE_LISTENERS);

		for ( String fn : args ) {
			File f = new File(dataDir, fn);
			Dust.access(MIND_ATT_AGENT_SELF, MindAccess.Set, f, STREAM_ATT_STREAM_FILE);
			aXml.agentExecAction(MindAction.Process);
		}

		aXml.agentExecAction(MindAction.Release);
		
		dump.ps.flush();
		dump.ps.close();

	}

}
