package hu.sze.uni.xbrl.edgar;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustDevUtils;
import hu.sze.milab.dust.machine.DustMachineTempUtils;
import hu.sze.milab.dust.stream.zip.DustZipAgentReader;

public class XbrlEdgarBoot implements XbrlEdgarConsts {
	
	public static void boot(String[] launchParams) throws Exception {
		DustMachineTempUtils.test();
		
		zipRead();
	}
		
	public static void zipRead() throws Exception {
		MindHandle hZipFile = DustDevUtils.registerHandle("0", RESOURCE_ASP_URL);
		Dust.access(hZipFile, MIND_TAG_ACCESS_SET, "work/xbrl/data/sources/edgar/submissions_00.zip", RESOURCE_ATT_URL_PATH);
		
		MindHandle hTargetDir = DustDevUtils.registerHandle("0", RESOURCE_ASP_URL);
		Dust.access(hTargetDir, MIND_TAG_ACCESS_SET, "work/xbrl/data/sources/edgar/submissions", RESOURCE_ATT_URL_PATH);
				
		MindHandle hLogZipReader = DustDevUtils.registerLogic("0", DustZipAgentReader.class.getCanonicalName());
		MindHandle hAgtZipReader = DustDevUtils.registerAgent("0", hLogZipReader);
		
		MindHandle hZipEntry = DustDevUtils.registerHandle("0", RESOURCE_ASP_STREAM);
		
		Dust.access(hAgtZipReader, MIND_TAG_ACCESS_SET, hZipFile, MISC_ATT_CONN_SOURCE);
		Dust.access(hAgtZipReader, MIND_TAG_ACCESS_SET, hZipEntry, MISC_ATT_CONN_TARGET);
		
		MindHandle hLogUnzip = DustDevUtils.registerLogic("0", XbrlEdgarAgentUnzip.class.getCanonicalName());
		MindHandle hAgtUnzip = DustDevUtils.registerAgent("0", hLogUnzip); 
		
		Dust.access(hAgtUnzip, MIND_TAG_ACCESS_SET, hTargetDir, MISC_ATT_CONN_TARGET);

		Dust.access(hZipEntry, MIND_TAG_ACCESS_INSERT, hAgtUnzip, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		Dust.access(APP_ASSEMBLY_MAIN, MIND_TAG_ACCESS_SET, hAgtZipReader, MIND_ATT_ASSEMBLY_STARTAGENTS, KEY_ADD);
	}

}
