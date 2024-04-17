package hu.sze.uni.xbrl.esg.india;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.dev.DustDevUtils;

public class XbrlEsgIndiaBoot implements XbrlEsgIndiaConsts {
	
	public static void boot(String[] launchParams) throws Exception {
//		DustMachineTempUtils.test();
		
		zipRead();
	}
		
	public static void zipRead() throws Exception {
		
		MindHandle hAgtIndiaRoot = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_FILESYSTEM);
		Dust.access(MindAccess.Set, "/Users/lkedves/work/xbrl/20240416_ESG_India", hAgtIndiaRoot, RESOURCE_ATT_URL_PATH);
		
		MindHandle hDataCsvStream = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report list CSV stream");

		Dust.access(MindAccess.Set, "CF-BRSR-equities-16-Apr-2024.csv", hDataCsvStream, RESOURCE_ATT_URL_PATH);
		DustDevUtils.setTag(hDataCsvStream, MISC_TAG_DIRECTION_IN, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataCsvStream, RESOURCE_TAG_STREAMTYPE_TEXT, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtIndiaRoot, hDataCsvStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataCsvRow = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Report list row");
		
		MindHandle hAgtCsvSax = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_CSVSAX);
		Dust.access(MindAccess.Set, hDataCsvStream, hAgtCsvSax, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataCsvRow, hAgtCsvSax, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtCsvSax, hDataCsvStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		
		MindHandle hDataCacheItem = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Cache item");

		MindHandle hAgtPopulateCacheInfo = DustDevUtils.newHandle(XBRLTEST_UNIT, MIND_NAR_POPULATE, "Init cache item");
		Dust.access(MindAccess.Set, hDataCacheItem, hAgtPopulateCacheInfo, MISC_ATT_CONN_TARGET);
		Dust.access(MindAccess.Set, MISC_ATT_CONN_MEMBERMAP, hAgtPopulateCacheInfo, MIND_ATT_POPULATE_ROOTATT);
		Dust.access(MindAccess.Set, "**XBRL", hAgtPopulateCacheInfo, MISC_ATT_CONN_MEMBERMAP, RESOURCE_ATT_URL_PATH);
		Dust.access(MindAccess.Set, "!DustUtils.getPostfix(get('**XBRL'), '/')", hAgtPopulateCacheInfo, MISC_ATT_CONN_MEMBERMAP, TEXT_ATT_TOKEN);
		Dust.access(MindAccess.Insert, hAgtPopulateCacheInfo, hDataCsvRow, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtCacheRoot = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_FILESYSTEM);
		Dust.access(MindAccess.Set, "/Users/lkedves/work/xbrl/20240416_ESG_India/store", hAgtCacheRoot, RESOURCE_ATT_URL_PATH);
		
		MindHandle hAgtCachePopulate = DustDevUtils.registerAgent(XBRLTEST_UNIT, NET_NAR_DOWNLOAD);
		MindHandle hDataWebRequest = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_URL, "Download request");
		Dust.access(MindAccess.Insert, hAgtCachePopulate, hDataWebRequest, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		
		MindHandle hDataReportDownloadStream = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report stream");
		DustDevUtils.setTag(hDataReportDownloadStream, MISC_TAG_DIRECTION_OUT, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataReportDownloadStream, RESOURCE_TAG_STREAMTYPE_RAW, RESOURCE_TAG_STREAMTYPE);

		MindHandle hAgtReportCache = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_NAR_CACHE, "Report Cache");
		DustDevUtils.setTag(hAgtReportCache, MISC_TAG_DBLHASH);
		Dust.access(MindAccess.Set, hAgtCacheRoot, hAgtReportCache, MISC_ATT_CONN_SOURCE);
		Dust.access(MindAccess.Set, hDataWebRequest, hAgtReportCache, RESOURCE_ATT_CACHE_REQUEST);
		Dust.access(MindAccess.Set, hDataReportDownloadStream, hAgtReportCache, MISC_ATT_CONN_TARGET);
		Dust.access(MindAccess.Insert, hAgtReportCache, hDataCacheItem, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		
		MindHandle hDataReportContent = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Parsed report");

		MindHandle hAgtXmlDom = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_NAR_XMLDOM, "XML reader");
		Dust.access(MindAccess.Set, hDataReportDownloadStream, hAgtXmlDom, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataReportContent, hAgtXmlDom, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtXmlDom, hDataReportDownloadStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		
		
		
		MindHandle hDataReportStream = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report content CSV stream");

		DustDevUtils.setTag(hDataReportStream, MISC_TAG_DIRECTION_OUT, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataReportStream, RESOURCE_TAG_STREAMTYPE_TEXT, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtCacheRoot, hDataReportStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataReportRow = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "CSV data");

		
		Dust.access(MindAccess.Set, hDataCsvStream, APP_ASSEMBLY_MAIN, MIND_ATT_ASSEMBLY_STARTAGENTS, KEY_ADD);
	}

}
