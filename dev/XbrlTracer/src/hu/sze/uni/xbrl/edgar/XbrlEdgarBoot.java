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
		
		MindHandle hLogFSRoot = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_AGT_FILESYSTEM);
		Dust.access(MindAccess.Set, "work/xbrl/data/sources/edgar", hLogFSRoot, RESOURCE_ATT_URL_PATH);
		
		MindHandle hZipFile = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_URL);
		Dust.access(MindAccess.Insert, hLogFSRoot, hZipFile, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		Dust.access(MindAccess.Set, "submissions_00.zip", hZipFile, RESOURCE_ATT_URL_PATH);
		
		MindHandle hTargetDir = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_URL);
		Dust.access(MindAccess.Insert, hLogFSRoot, hTargetDir, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		Dust.access(MindAccess.Set, "submissions", hTargetDir, RESOURCE_ATT_URL_PATH);
						
		MindHandle hZipEntry = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM);
		
		MindHandle hAgtZipReader = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_AGT_ZIPREADER);
		Dust.access(MindAccess.Set, hZipFile, hAgtZipReader, MISC_ATT_CONN_SOURCE);
		Dust.access(MindAccess.Set, hZipEntry, hAgtZipReader, MISC_ATT_CONN_TARGET);
		
		MindHandle hAgtUnzip = DustDevUtils.registerAgent(XBRLTEST_UNIT, hLogEdgarUnzip); 		
		Dust.access(MindAccess.Set, hTargetDir, hAgtUnzip, MISC_ATT_CONN_TARGET);
		Dust.access(MindAccess.Insert, hAgtUnzip, hZipEntry, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtJsonDOM = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_AGT_JSONDOM);
		MindHandle hJsonStream = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM);
		MindHandle hJsonData = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT);
		Dust.access(MindAccess.Set, hJsonStream, hAgtJsonDOM, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hJsonData, hAgtJsonDOM, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtJsonDOM, hJsonStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		Dust.access(MindAccess.Insert, hAgtJsonDOM, hJsonData, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		
		MindHandle hAgtCsvSax = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_AGT_CSVSAX);
		MindHandle hCsvStream = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM);
		MindHandle hCsvData = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT);
		Dust.access(MindAccess.Set, hCsvStream, hAgtCsvSax, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hCsvData, hAgtCsvSax, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Set, MISC_TAG_DIRECTION_OUT, hCsvStream, MIND_ATT_KNOWLEDGE_TAGS, MISC_TAG_DIRECTION);
		Dust.access(MindAccess.Set, RESOURCE_TAG_STREAMTYPE_TEXT, hCsvStream, MIND_ATT_KNOWLEDGE_TAGS, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hLogFSRoot, hCsvStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		Dust.access(MindAccess.Insert, hAgtCsvSax, hCsvStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		Dust.access(MindAccess.Insert, hAgtCsvSax, hCsvData, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		
		MindHandle hAgtSubProc = DustDevUtils.registerAgent(XBRLTEST_UNIT, hLogEdgarSubProc); 		
		Dust.access(MindAccess.Set, hAgtJsonDOM, hAgtSubProc, EDGARMETA_ATT_JSONDOM);
		Dust.access(MindAccess.Set, hAgtCsvSax, hAgtSubProc, EDGARMETA_ATT_CSVSAX);
		
		Dust.access(MindAccess.Insert, hAgtSubProc, hTargetDir, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		Dust.access(MindAccess.Set, hAgtZipReader, APP_ASSEMBLY_MAIN, MIND_ATT_ASSEMBLY_STARTAGENTS, KEY_ADD);
	}

}
