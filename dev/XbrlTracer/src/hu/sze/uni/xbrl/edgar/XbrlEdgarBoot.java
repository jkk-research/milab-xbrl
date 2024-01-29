package hu.sze.uni.xbrl.edgar;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.dev.DustDevUtils;
import hu.sze.milab.dust.machine.DustMachineTempUtils;

public class XbrlEdgarBoot implements XbrlEdgarConsts {
	
	public static void boot(String[] launchParams) throws Exception {
		DustMachineTempUtils.test();
		
		zipRead();
	}
		
	public static void zipRead() throws Exception {
		MindHandle hLogEdgarUnzip = DustDevUtils.registerLogic(EDGARMETA_UNIT, XbrlEdgarAgentUnzip.class.getCanonicalName());
		MindHandle hLogEdgarSubProc = DustDevUtils.registerLogic(EDGARMETA_UNIT, XbrlEdgarAgentProcessSubmissions.class.getCanonicalName());
		
		MindHandle hLogFSRoot = DustDevUtils.registerAgent(EDGARMETA_UNIT, RESOURCE_SRV_FILESYSTEM);
		Dust.access(hLogFSRoot, MIND_TAG_ACCESS_SET, "work/xbrl/data/sources/edgar", RESOURCE_ATT_URL_PATH);
		
		MindHandle hZipFile = DustDevUtils.registerHandle(EDGARMETA_UNIT, RESOURCE_ASP_URL);
		Dust.access(hZipFile, MIND_TAG_ACCESS_INSERT, hLogFSRoot, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		Dust.access(hZipFile, MIND_TAG_ACCESS_SET, "submissions_00.zip", RESOURCE_ATT_URL_PATH);
		
		MindHandle hTargetDir = DustDevUtils.registerHandle(EDGARMETA_UNIT, RESOURCE_ASP_URL);
		Dust.access(hTargetDir, MIND_TAG_ACCESS_INSERT, hLogFSRoot, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		Dust.access(hTargetDir, MIND_TAG_ACCESS_SET, "submissions", RESOURCE_ATT_URL_PATH);
						
		MindHandle hZipEntry = DustDevUtils.registerHandle(EDGARMETA_UNIT, RESOURCE_ASP_STREAM);
		
		MindHandle hAgtZipReader = DustDevUtils.registerAgent(EDGARMETA_UNIT, RESOURCE_AGT_ZIPREADER);
		Dust.access(hAgtZipReader, MIND_TAG_ACCESS_SET, hZipFile, MISC_ATT_CONN_SOURCE);
		Dust.access(hAgtZipReader, MIND_TAG_ACCESS_SET, hZipEntry, MISC_ATT_CONN_TARGET);
		
		MindHandle hAgtUnzip = DustDevUtils.registerAgent(EDGARMETA_UNIT, hLogEdgarUnzip); 		
		Dust.access(hAgtUnzip, MIND_TAG_ACCESS_SET, hTargetDir, MISC_ATT_CONN_TARGET);
		Dust.access(hZipEntry, MIND_TAG_ACCESS_INSERT, hAgtUnzip, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtJsonDOM = DustDevUtils.registerAgent(EDGARMETA_UNIT, RESOURCE_AGT_JSON_DOM);
		MindHandle hJsonStream = DustDevUtils.registerHandle(EDGARMETA_UNIT, RESOURCE_ASP_STREAM);
		MindHandle hJsonData = DustDevUtils.registerHandle(EDGARMETA_UNIT, MISC_ASP_VARIANT);
		Dust.access(hAgtJsonDOM, MIND_TAG_ACCESS_SET, hJsonStream, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(hAgtJsonDOM, MIND_TAG_ACCESS_SET, hJsonData, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(hJsonStream, MIND_TAG_ACCESS_INSERT, hAgtJsonDOM, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		Dust.access(hJsonData, MIND_TAG_ACCESS_INSERT, hAgtJsonDOM, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		
		MindHandle hAgtSubProc = DustDevUtils.registerAgent(EDGARMETA_UNIT, hLogEdgarSubProc); 		
		Dust.access(hAgtSubProc, MIND_TAG_ACCESS_SET, hAgtJsonDOM, EDGARMETA_ATT_JSONDOM);
		Dust.access(hTargetDir, MIND_TAG_ACCESS_INSERT, hAgtSubProc, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		Dust.access(APP_ASSEMBLY_MAIN, MIND_TAG_ACCESS_SET, hAgtZipReader, MIND_ATT_ASSEMBLY_STARTAGENTS, KEY_ADD);
	}

}
