package hu.sze.uni.xbrl.edgar;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.stream.zip.DustZipAgentReader;

public class DustEdgarBoot implements XbrlEdgarConsts {
	
	public static void boot(String[] launchParams) throws Exception {
		zipRead();
	}
		
	public static void zipRead() throws Exception {
		MindHandle hLogZipReader = Dust.recall("0:");
		Dust.access(hLogZipReader, MIND_TAG_ACCESS_SET, MIND_ASP_LOGIC, MIND_ATT_KNOWLEDGE_PRIMARYASPECT);

		MindHandle hNatZipReader = Dust.recall("0:");
		Dust.access(hNatZipReader, MIND_TAG_ACCESS_SET, DUST_ASP_NATIVELOGIC, MIND_ATT_KNOWLEDGE_PRIMARYASPECT);
		
		Dust.access(hNatZipReader, MIND_TAG_ACCESS_SET, hLogZipReader, DUST_ATT_NATIVELOGIC_LOGIC);
		Dust.access(hNatZipReader, MIND_TAG_ACCESS_SET, DustZipAgentReader.class.getCanonicalName(), DUST_ATT_NATIVELOGIC_IMPLEMENTATION);
		
		Dust.access(APP_MODULE_MAIN, MIND_TAG_ACCESS_SET, hNatZipReader, DUST_ATT_MODULE_NATIVELOGICS, KEY_ADD);

		MindHandle hAgtZipReader = Dust.recall("0:");
		Dust.access(hAgtZipReader, MIND_TAG_ACCESS_SET, MIND_ASP_AGENT, MIND_ATT_KNOWLEDGE_PRIMARYASPECT);

		Dust.access(hAgtZipReader, MIND_TAG_ACCESS_SET, hLogZipReader, MIND_ATT_AGENT_LOGIC);

		Dust.access(APP_ASSEMBLY_MAIN, MIND_TAG_ACCESS_SET, hAgtZipReader, MIND_ATT_ASSEMBLY_STARTAGENTS, KEY_ADD);
	}



}
