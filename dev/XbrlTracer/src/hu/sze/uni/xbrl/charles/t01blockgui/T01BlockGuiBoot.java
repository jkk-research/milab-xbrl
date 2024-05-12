package hu.sze.uni.xbrl.charles.t01blockgui;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.dev.DustDevUtils;
import hu.sze.milab.dust.machine.DustMachineTempUtils;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsFile;
import hu.sze.uni.xbrl.XbrlConsts;
import hu.sze.uni.xbrl.XbrlHandles;
import hu.sze.uni.xbrl.XbrlPoolLoaderAgent;
import hu.sze.uni.xbrl.gui.XbrlGuiFrameAgent;
import hu.sze.uni.xbrl.parser.XbrlParserXmlAgent;

public class T01BlockGuiBoot implements XbrlConsts {

	static {
		try {
			DustMachineTempUtils.initFromInterfaces(XbrlHandles.class);
		} catch (IllegalAccessException e) {
			DustException.wrap(e);
		}
	}

	public static void boot(String[] launchParams) throws Exception {

		blockGui();
	}

	public static void blockGui() throws Exception {
//		DustTestBootSimple.boot(null);

		DustDevUtils.registerNative(XBRLDOCK_NAR_XMLLOADER, XBRLDOCK_UNIT, APP_MODULE_MAIN, XbrlParserXmlAgent.class.getName());
		DustDevUtils.registerNative(XBRLDOCK_NAR_POOLLOADER, XBRLDOCK_UNIT, APP_MODULE_MAIN, XbrlPoolLoaderAgent.class.getName());
		DustDevUtils.registerNative(XBRLDOCK_NAR_GUI_FRAME, XBRLDOCK_UNIT, APP_MODULE_MAIN, XbrlGuiFrameAgent.class.getName());

		// Set work root folder

		MindHandle hAgtT01Root = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_FILESYSTEM, "Test01 data root");
		Dust.access(MindAccess.Set, "chd/t01", hAgtT01Root, RESOURCE_ATT_URL_PATH);

		// reading the report list CSV

		MindHandle hDataReportStream = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report list stream");

		Dust.access(MindAccess.Set, "MicrosoftReports.txt", hDataReportStream, TEXT_ATT_TOKEN);
		DustDevUtils.setTag(hDataReportStream, MISC_TAG_DIRECTION_IN, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataReportStream, RESOURCE_TAG_STREAMTYPE_TEXT, RESOURCE_TAG_STREAMTYPE);

		Dust.access(MindAccess.Insert, hAgtT01Root, hDataReportStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataReportRow = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Report list row");
		Dust.access(MindAccess.Insert, "ReportURL", hDataReportRow, MISC_ATT_CONN_MEMBERARR, KEY_ADD);

		MindHandle hAgtCsvSax = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_CSVSAX, "Report list reader");
		Dust.access(MindAccess.Set, ",", hAgtCsvSax, MISC_ATT_GEN_SEP_ITEM);
		Dust.access(MindAccess.Set, hDataReportStream, hAgtCsvSax, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataReportRow, hAgtCsvSax, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtCsvSax, hDataReportStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		// Report cache

		MindHandle hAgtReportCacheRoot = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_FILESYSTEM, "Filesystem report cache root");
		Dust.access(MindAccess.Set, hAgtT01Root, hAgtReportCacheRoot, MISC_ATT_CONN_PARENT);
		Dust.access(MindAccess.Set, "reports", hAgtReportCacheRoot, TEXT_ATT_TOKEN);

		MindHandle hDataReportContentStream = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report content stream");
		DustDevUtils.setTag(hDataReportContentStream, MISC_TAG_DIRECTION_IN, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataReportContentStream, RESOURCE_TAG_STREAMTYPE_RAW, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtReportCacheRoot, hDataReportContentStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataReportCacheItem = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Cache item");
		Dust.access(MindAccess.Set, hDataReportContentStream, hDataReportCacheItem, MISC_ATT_CONN_TARGET);

		MindHandle hAgtReportCacheInfoExpr = DustDevUtils.registerAgent(XBRLTEST_UNIT, EXPR_NAR_POPULATE, "Init cache item");
		Dust.access(MindAccess.Set, DustUtils.class, hAgtReportCacheInfoExpr, EXPR_ATT_EXPRESSION_STATIC, "DustUtils");
		Dust.access(MindAccess.Set, hDataReportCacheItem, hAgtReportCacheInfoExpr, MISC_ATT_CONN_TARGET);
		Dust.access(MindAccess.Set, MISC_ATT_CONN_MEMBERMAP, hAgtReportCacheInfoExpr, MISC_ATT_GEN_TARGET_ATT);
		Dust.access(MindAccess.Set, "get('ReportURL')", hAgtReportCacheInfoExpr, MISC_ATT_CONN_MEMBERMAP, RESOURCE_ATT_URL_PATH);
		Dust.access(MindAccess.Set, "DustUtils.getPostfix(get('ReportURL'), 'data/')", hAgtReportCacheInfoExpr, MISC_ATT_CONN_MEMBERMAP, TEXT_ATT_TOKEN);

		MindHandle hDataReportDownloadStream = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report download stream");
		DustDevUtils.setTag(hDataReportDownloadStream, MISC_TAG_DIRECTION_OUT, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataReportDownloadStream, RESOURCE_TAG_STREAMTYPE_RAW, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtReportCacheRoot, hDataReportDownloadStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataDownloadRequest = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_URL, "Download request");
		Dust.access(MindAccess.Set, hDataReportContentStream, hDataDownloadRequest, MISC_ATT_CONN_SOURCE);
		Dust.access(MindAccess.Set, hDataReportDownloadStream, hDataDownloadRequest, MISC_ATT_CONN_TARGET);

		MindHandle hAgtDownloadThrottle = DustDevUtils.registerAgent(XBRLTEST_UNIT, EVENT_NAR_THROTTLE, "Cache throttle");
		Dust.access(MindAccess.Set, 200L, hAgtDownloadThrottle, EVENT_ATT_TIME_MILLI);
		Dust.access(MindAccess.Insert, hAgtDownloadThrottle, hDataDownloadRequest, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtDownload = DustDevUtils.registerAgent(XBRLTEST_UNIT, NET_NAR_DOWNLOAD, "Cache downloader");
		Dust.access(MindAccess.Insert, hAgtDownload, hDataDownloadRequest, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
//		DustDevUtils.setTag(hAgtDownload, DEV_TAG_TEST);

		Dust.access(MindAccess.Insert, "User-Agent: Szechenyi Istvan University kedves.lorand.laszlo@sze.hu", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
		Dust.access(MindAccess.Insert, "Accept-Encoding: gzip, deflate", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);

		MindHandle hAgtReportCache = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_CACHE, "Report Cache");
		Dust.access(MindAccess.Set, hDataDownloadRequest, hAgtReportCache, RESOURCE_ATT_CACHE_REQUEST);
		Dust.access(MindAccess.Insert, hAgtReportCache, hDataReportCacheItem, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		// Process report XML and generate Data / Text csv

		MindHandle hDataReportContent = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Parsed report");

		MindHandle hAgtXmlDom = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_XMLDOM, "XML reader");
		Dust.access(MindAccess.Set, hDataReportContentStream, hAgtXmlDom, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataReportContent, hAgtXmlDom, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtXmlDom, hDataReportContentStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtFactCacheRoot = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_FILESYSTEM, "Filesystem DATA cache root");
		Dust.access(MindAccess.Set, hAgtT01Root, hAgtFactCacheRoot, MISC_ATT_CONN_PARENT);
		Dust.access(MindAccess.Set, "data", hAgtFactCacheRoot, TEXT_ATT_TOKEN);

		MindHandle hDataFactRowData = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Report CSV row data");
		MindHandle hDataFactStreamData = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report Data CSV stream");
		DustDevUtils.setTag(hDataFactStreamData, MISC_TAG_DIRECTION_OUT, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataFactStreamData, RESOURCE_TAG_STREAMTYPE_TEXT, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtFactCacheRoot, hDataFactStreamData, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtCsvFactWriterData = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_CSVSAX, "CSV Data writer");
		Dust.access(MindAccess.Set, hDataFactStreamData, hAgtCsvFactWriterData, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataFactRowData, hAgtCsvFactWriterData, RESOURCE_ATT_PROCESSOR_DATA);

		MindHandle hDataFactRowText = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Report CSV row text");
		MindHandle hDataFactStreamText = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report Text CSV stream");
		DustDevUtils.setTag(hDataFactStreamText, MISC_TAG_DIRECTION_OUT, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataFactStreamText, RESOURCE_TAG_STREAMTYPE_TEXT, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtFactCacheRoot, hDataFactStreamText, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtCsvFactWriterText = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_CSVSAX, "CSV Text writer");
		Dust.access(MindAccess.Set, hDataFactStreamText, hAgtCsvFactWriterText, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataFactRowText, hAgtCsvFactWriterText, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtCsvFactWriterText, hDataFactRowText, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtXmlLoader = DustDevUtils.registerAgent(XBRLTEST_UNIT, XBRLDOCK_NAR_XMLLOADER, "XML loader");
		Dust.access(MindAccess.Set, hAgtCsvFactWriterData, hAgtXmlLoader, XBRLDOCK_ATT_XMLLOADER_ROWDATA);
		Dust.access(MindAccess.Set, hAgtCsvFactWriterText, hAgtXmlLoader, XBRLDOCK_ATT_XMLLOADER_ROWTEXT);
		Dust.access(MindAccess.Insert, hAgtXmlLoader, hDataReportContent, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		// Data csv loading

		MindHandle hAgtDataLoaderExpr = DustDevUtils.registerAgent(XBRLTEST_UNIT, EXPR_NAR_POPULATE, "Init cache item");
		Dust.access(MindAccess.Set, DustUtils.class, hAgtDataLoaderExpr, EXPR_ATT_EXPRESSION_STATIC, "DustUtils");
		Dust.access(MindAccess.Set, hDataFactStreamData, hAgtDataLoaderExpr, MISC_ATT_CONN_TARGET);
		Dust.access(MindAccess.Set, MISC_ATT_CONN_MEMBERMAP, hAgtDataLoaderExpr, MISC_ATT_GEN_TARGET_ATT);
		Dust.access(MindAccess.Set, "DustUtils.getPostfix(get('ReportURL'), '/').replace('.xml', '_Data.csv')", hAgtDataLoaderExpr, MISC_ATT_CONN_MEMBERMAP, TEXT_ATT_TOKEN);

		// Report pool

		MindHandle hDataReportPool = DustDevUtils.newHandle(XBRLTEST_UNIT, XBRLDOCK_ASP_POOL, "Report pool");
		MindHandle hDataPoolCalendar = DustDevUtils.newHandle(XBRLTEST_UNIT, EVENT_ASP_CALENDAR, "Pool calendar");
		Dust.access(MindAccess.Set, hDataPoolCalendar, hDataReportPool, XBRLDOCK_ATT_POOL_CALENDAR);

		MindHandle hAgtPoolLoader = DustDevUtils.registerAgent(XBRLTEST_UNIT, XBRLDOCK_NAR_POOLLOADER, "XML loader");
		Dust.access(MindAccess.Set, hDataReportPool, hAgtPoolLoader, MISC_ATT_CONN_TARGET);

		Dust.access(MindAccess.Set, XBRLDOCK_ATT_FACT_DECIMALS, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_FACTMAP, "Dec");
		Dust.access(MindAccess.Set, XBRLDOCK_ATT_FACT_SCALE, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_FACTMAP, "Scale");
		Dust.access(MindAccess.Set, XBRLDOCK_TAG_FACT_FORMAT, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_FACTMAP, "Format");

		Dust.access(MindAccess.Set, XBRLDOCK_ASP_REPORT, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:DocumentType");
		Dust.access(MindAccess.Set, XBRLDOCK_ASP_REPORT, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:AmendmentFlag");
		Dust.access(MindAccess.Set, XBRLDOCK_ASP_REPORT, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:DocumentPeriodEndDate");
		Dust.access(MindAccess.Set, XBRLDOCK_ASP_REPORT, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:DocumentFiscalYearFocus");
		Dust.access(MindAccess.Set, XBRLDOCK_ASP_REPORT, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:DocumentFiscalPeriodFocus");
		Dust.access(MindAccess.Set, XBRLDOCK_ASP_REPORT, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:CurrentFiscalYearEndDate");

		Dust.access(MindAccess.Set, XBRLDOCK_ASP_ENTITY, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:TradingSymbol");
		Dust.access(MindAccess.Set, XBRLDOCK_ASP_ENTITY, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:EntityRegistrantName");
		Dust.access(MindAccess.Set, XBRLDOCK_ASP_ENTITY, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:EntityCentralIndexKey");
		Dust.access(MindAccess.Set, XBRLDOCK_ASP_ENTITY, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:EntityFilerCategory");
		Dust.access(MindAccess.Set, XBRLDOCK_ASP_ENTITY, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:EntitySmallBusiness");
		Dust.access(MindAccess.Set, XBRLDOCK_ASP_ENTITY, hAgtPoolLoader, XBRLDOCK_ATT_POOLLOADER_CONCEPTMAP, "dei:EntityEmergingGrowthCompany");

		Dust.access(MindAccess.Insert, new MindCommitFilter(hAgtPoolLoader, MIND_TAG_ACTION_PROCESS), hDataFactRowData, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		Dust.access(MindAccess.Insert, new MindCommitFilter(hAgtPoolLoader, MIND_TAG_ACTION_END), hDataReportRow, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtGuiFrame = DustDevUtils.registerAgent(XBRLTEST_UNIT, XBRLDOCK_NAR_GUI_FRAME, "GUI frame");
		Dust.access(MindAccess.Set, hDataReportPool, hAgtGuiFrame, MISC_ATT_CONN_SOURCE);
		Dust.access(MindAccess.Insert, hAgtGuiFrame, hDataReportPool, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

//		DustDevUtils.setTag(hAgtXmlLoader, DEV_TAG_TEST);

		boolean reportsCached = DustUtilsFile.exists(Dust.access(MindAccess.Peek, "", hAgtT01Root, RESOURCE_ATT_URL_PATH), Dust.access(MindAccess.Peek, "", hAgtReportCacheRoot, TEXT_ATT_TOKEN));

//		reportsCached = false;

		if ( reportsCached ) {
			Dust.access(MindAccess.Insert, hAgtDataLoaderExpr, hDataReportRow, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

			DustDevUtils.setTag(hDataFactStreamData, MISC_TAG_DIRECTION_IN, MISC_TAG_DIRECTION);
			Dust.access(MindAccess.Insert, hAgtCsvFactWriterData, hDataFactStreamData, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		} else {
			Dust.access(MindAccess.Insert, hAgtReportCacheInfoExpr, hDataReportRow, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
			Dust.access(MindAccess.Insert, hAgtCsvFactWriterData, hDataFactRowData, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		}

		DustDevUtils.setTag(hDataReportStream, MISC_TAG_TRANSACTION);
		Dust.access(MindAccess.Set, hDataReportStream, APP_ASSEMBLY_MAIN, MIND_ATT_ASSEMBLY_STARTCOMMITS, KEY_ADD);

	}

}
