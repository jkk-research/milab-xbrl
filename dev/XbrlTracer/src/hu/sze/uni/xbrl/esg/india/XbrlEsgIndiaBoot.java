package hu.sze.uni.xbrl.esg.india;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.dev.DustDevUtils;
import hu.sze.uni.xbrl.parser.XbrlParserXmlAgent;

public class XbrlEsgIndiaBoot implements XbrlEsgIndiaConsts {

	public static void boot(String[] launchParams) throws Exception {
//		DustMachineTempUtils.test();

		zipRead();
	}

	public static void zipRead() throws Exception {
//		DustTestBootSimple.boot(null);

		DustDevUtils.registerNative(XBRLDOCK_NAR_XMLLOADER, XBRLDOCK_UNIT, APP_MODULE_MAIN, XbrlParserXmlAgent.class.getCanonicalName());

		MindHandle hAgtIndiaRoot = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_FILESYSTEM, "Filesystem India root");
		Dust.access(MindAccess.Set, "/Users/lkedves/work/xbrl/20240416_ESG_India", hAgtIndiaRoot, RESOURCE_ATT_URL_PATH);

		MindHandle hDataCsvStream = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report list CSV stream");

		Dust.access(MindAccess.Set, "CF-BRSR-equities-16-Apr-2024.csv", hDataCsvStream, TEXT_ATT_TOKEN);
		DustDevUtils.setTag(hDataCsvStream, MISC_TAG_DIRECTION_IN, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataCsvStream, RESOURCE_TAG_STREAMTYPE_TEXT, RESOURCE_TAG_STREAMTYPE);

		Dust.access(MindAccess.Insert, hAgtIndiaRoot, hDataCsvStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataCsvRow = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Report list row");

		MindHandle hAgtCsvSax = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_CSVSAX, "Report list CSV reader");
		Dust.access(MindAccess.Set, ",", hAgtCsvSax, MISC_ATT_GEN_SEP_ITEM);
		Dust.access(MindAccess.Set, hDataCsvStream, hAgtCsvSax, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataCsvRow, hAgtCsvSax, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtCsvSax, hDataCsvStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtCacheRoot = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_FILESYSTEM, "Filesystem cache root");
		Dust.access(MindAccess.Set, "/Users/lkedves/work/xbrl/20240416_ESG_India/store", hAgtCacheRoot, RESOURCE_ATT_URL_PATH);

		MindHandle hDataReportContentStream = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report content stream");
		DustDevUtils.setTag(hDataReportContentStream, MISC_TAG_DIRECTION_IN, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataReportContentStream, RESOURCE_TAG_STREAMTYPE_RAW, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtCacheRoot, hDataReportContentStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataCacheItem = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Cache item");
		Dust.access(MindAccess.Set, hDataReportContentStream, hDataCacheItem, MISC_ATT_CONN_TARGET);

		MindHandle hAgtPopulateCacheInfo = DustDevUtils.registerAgent(XBRLTEST_UNIT, MIND_NAR_POPULATE, "Init cache item");
		Dust.access(MindAccess.Set, hDataCacheItem, hAgtPopulateCacheInfo, MISC_ATT_CONN_TARGET);
		Dust.access(MindAccess.Set, MISC_ATT_CONN_MEMBERMAP, hAgtPopulateCacheInfo, MIND_ATT_POPULATE_ROOTATT);
		Dust.access(MindAccess.Set, "!get('**XBRL')", hAgtPopulateCacheInfo, MISC_ATT_CONN_MEMBERMAP, RESOURCE_ATT_URL_PATH);
		Dust.access(MindAccess.Set, "!DustUtils.getPostfix(get('**XBRL'), '/')", hAgtPopulateCacheInfo, MISC_ATT_CONN_MEMBERMAP, TEXT_ATT_TOKEN);
		Dust.access(MindAccess.Insert, hAgtPopulateCacheInfo, hDataCsvRow, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataReportDownloadStream = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report download stream");
		DustDevUtils.setTag(hDataReportDownloadStream, MISC_TAG_DIRECTION_OUT, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataReportDownloadStream, RESOURCE_TAG_STREAMTYPE_RAW, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtCacheRoot, hDataReportDownloadStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataWebRequest = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_URL, "Download request");
		Dust.access(MindAccess.Set, hDataReportContentStream, hDataWebRequest, MISC_ATT_CONN_SOURCE);
		Dust.access(MindAccess.Set, hDataReportDownloadStream, hDataWebRequest, MISC_ATT_CONN_TARGET);

		MindHandle hAgtThrottle = DustDevUtils.registerAgent(XBRLTEST_UNIT, EVENT_NAR_THROTTLE, "Cache throttle");
		Dust.access(MindAccess.Set, 2000L, hAgtThrottle, EVENT_ATT_TIME_MILLI);
		Dust.access(MindAccess.Insert, hAgtThrottle, hDataWebRequest, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtDownload = DustDevUtils.registerAgent(XBRLTEST_UNIT, NET_NAR_DOWNLOAD, "Cache downloader");
		Dust.access(MindAccess.Set, 10000L, hAgtDownload, EVENT_ATT_TIME_MILLI);
		Dust.access(MindAccess.Insert, hAgtDownload, hDataWebRequest, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);
		DustDevUtils.setTag(hAgtDownload, DEV_TAG_TEST);

	  Dust.access(MindAccess.Insert, "accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "accept-language: en-US,en;q=0.9,hu-HU;q=0.8,hu;q=0.7", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "cache-control: max-age=0", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "referer: https://www.nseindia.com/", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "cookie: ak_bmsc=3E075BE883D37300D9F9B6CAB07A4C27~000000000000000000000000000000~YAAQlHkmF0VcNvqOAQAAAdfg+heH7y0jBS78sPmivBJlRwfwJNp2/q8V8D+0ooWR/LVzLdha155NXnigDVyOmPpKsNvWOX/FiS4wwkjDhwS4pkb34Gu/bnI6R39b8JgSVV+urglmbp+OuX8NL3qY7fkgKY0KEMbg2msY1+mcqkCKd5pGr9D1ldKS/c6Am9oQTsvrLMszZN+FkiimB/6jMgpki5TvCXXY9ds7GFwG/SU+7k/rZ4e7EPD+G3m3myuUCJrFmk3REzFPKegjFHB5ePagp38tBi479SEH7O+9QKJ8g79X58/ilsfbMBRuOopS1Rj4cDhcInP6+dnp8HKZxzemcpb407E4spLLNO7UMk+X5yT+n/RSovx6AghURs+XLMgEsXKcNskhVQ==; AKA_A2=A; bm_mi=0595116DEA68C6EF2F0F03DA5CBFCE93~YAAQk3nKF7UNhcWOAQAAjbH2+hfjuuCFAfqHvK3Z1H++2Vc6AvrVy8Ne+kXij9Q1D9+S0OOWOoJPV6zyorrqhgFbAWBGIQa4ADt8i1YZfsNO1EC/cnRy2YhNO0/bZ18+d367M5H+UUMmpWt3ajusHpLwHerlibJLiiu6Qbctyf8xt+rB6bKzlp2tCzrLFEP5BsSqsnLr2UkxjeYk/oPH4++DpGU7doMxUNoIEOLDYAH1OXBTZgrt2GgKxtbRfCzLB2j2VXlDpJsNVXwh2rpZ2t9knq+dRlygVw2/4uZk3V2kCCQ039XdQQqFD75iYOkrobaf7w/wjO6Ne9edyo0shEue0Ks880rQ0ORXhZGAlIMnV9u23j2bxZPMbevvOI/MD/FycKkm+5fsJSxkFeLvdYU=~1; _abck=F582EA0B2C10F21D09513B6A46F1AFD6~0~YAAQk3nKFxAXhcWOAQAAqh34+gtN7EgYwHRb/0+O3YlvTpAkavqP/Tz+bXKXxpo9JVEi3qA6P9uaQzMrf5zTqy6KX4kWNOU6FjjqP6epi68Co3nCF4hCc/Mv2brEHvS8ffSJ+U2jN8U04a5ZGicCbqQfFnB0Vd+LEvk1yB7x366I40Vlj8/LcQGH2qbDShgVbZry0HuZmfo/VzU6cfqHtggJUlOwjmr8SzZW53P3SmDsLtjpsn/FO46fFb7ZISntVahe2PEiDIxqEQzIw1DGRBfDbweQtrE9sDfgUe1Igqe228Le5xwKYx29Lh+7K3qZWNBSd04UnLtwhEK6XE8nY2b00XfmVGMS2Zkl88fcLrlLocUnHI3oYFeagT6k2Hd+qavDL6VpedmjriBaxqqlvJ7qTrHP/SOoGOk=~-1~-1~-1; bm_sz=DFF6BBFDE6B174C273071A5F5BF9DE81~YAAQg3kmF5fUbvaOAQAAwyT4+hdZVLtIl1D0a9U2y3zouHL5aP3KzdQAKLC4A1PvtKMVQXLl6gM8N6ClnvJWUsQnQg5nMfzB5R7/DWBYGfOZlTexKp8WjSumTkx2Mb777r6zP6VADF6tfuANuV0VRAexrOoGZIo56oJEjNIWF4NjExZofeAo9gqssiJbJAb8vZhPT3BnEuXihzIgtP44NNfpkcXYRSnxCPQb4ZWbRltLQoKmf0us+Au9SOOJex1BL79n7rLRup1R5NOTnhVkf8TKeRBEFZpC8j0GCndFlMlPaGocH9B9de8gDUXxUniH+yj5hmU9gNlYgyzGILUxlh1nKaNl9WvEyAE7NOejkpKnDYmoUGc12R/vpV7WYq1XNX0UAlnPKpJrSix48gh/vhXcvNbF~3621185~4277558; bm_sv=98E6DAA975224DCED61825262F59194A~YAAQk3nKF7AXhcWOAQAAoCz4+he7oUA8W4JKoz+rubEnE3/0doYNG54eYzzA5MeXLgvLz5IsAE6f/1tpLSImPpAetGKzayowWnl0SS+c63Ky/2DzBZ8jKq8DmIeop/2qyfImuHwvonCsjhMLAzJS4MQVPYM6CIgvT0JW/aJ1yY0oCiAgaMRZ4TVvQR23nXOaQFxWGlvJVLiZJ1l7mxmpbPLFM2eRve6ddJTx8wi1Qu7yDj6JIpmaLBF5GGod+6tJdI3D~1", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
		Dust.access(MindAccess.Insert, "Upgrade-Insecure-Requests: 1", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
		Dust.access(MindAccess.Insert, "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
		Dust.access(MindAccess.Insert, "sec-ch-ua: \"Google Chrome\";v=\"124\", \"Not:A-Brand\";v=\"99\", \"Chromium\";v=\"124\"", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
		Dust.access(MindAccess.Insert, "sec-ch-ua-mobile: ?0", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
		Dust.access(MindAccess.Insert, "sec-ch-ua-platform: \"macOS\"", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
		Dust.access(MindAccess.Insert, "sec-fetch-dest: document", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
		Dust.access(MindAccess.Insert, "sec-fetch-mode: navigate", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
		Dust.access(MindAccess.Insert, "sec-fetch-site: same-site", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
		Dust.access(MindAccess.Insert, "sec-fetch-user: ?1", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);

		MindHandle hAgtReportCache = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_CACHE, "Report Cache");
		DustDevUtils.setTag(hAgtReportCache, MISC_TAG_DBLHASH);
		Dust.access(MindAccess.Set, hDataWebRequest, hAgtReportCache, RESOURCE_ATT_CACHE_REQUEST);
		Dust.access(MindAccess.Insert, hAgtReportCache, hDataCacheItem, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataReportContent = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Parsed report");

		MindHandle hAgtXmlDom = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_XMLDOM, "XML reader");
		Dust.access(MindAccess.Set, hDataReportContentStream, hAgtXmlDom, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataReportContent, hAgtXmlDom, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtXmlDom, hDataReportContentStream, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataReportRowData = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Report CSV row data");
		MindHandle hDataReportStreamData = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report Data CSV stream");
		DustDevUtils.setTag(hDataReportStreamData, MISC_TAG_DIRECTION_OUT, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataReportStreamData, RESOURCE_TAG_STREAMTYPE_TEXT, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtCacheRoot, hDataReportStreamData, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtCsvReportWriterData = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_CSVSAX, "CSV Data writer");
		Dust.access(MindAccess.Set, hDataReportStreamData, hAgtCsvReportWriterData, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataReportRowData, hAgtCsvReportWriterData, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtCsvReportWriterData, hDataReportRowData, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataReportRowText = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Report CSV row text");
		MindHandle hDataReportStreamText = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report Text CSV stream");
		DustDevUtils.setTag(hDataReportStreamText, MISC_TAG_DIRECTION_OUT, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataReportStreamText, RESOURCE_TAG_STREAMTYPE_TEXT, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtCacheRoot, hDataReportStreamText, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtCsvReportWriterText = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_CSVSAX, "CSV Text writer");
		Dust.access(MindAccess.Set, hDataReportStreamText, hAgtCsvReportWriterText, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataReportRowText, hAgtCsvReportWriterText, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtCsvReportWriterText, hDataReportRowText, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtXmlLoader = DustDevUtils.registerAgent(XBRLTEST_UNIT, XBRLDOCK_NAR_XMLLOADER, "XML loader");
		Dust.access(MindAccess.Set, hAgtCsvReportWriterData, hAgtXmlLoader, XBRLDOCK_ATT_XMLLOADER_ROWDATA);
		Dust.access(MindAccess.Set, hAgtCsvReportWriterText, hAgtXmlLoader, XBRLDOCK_ATT_XMLLOADER_ROWTEXT);
		Dust.access(MindAccess.Insert, hAgtXmlLoader, hDataReportContent, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		DustDevUtils.setTag(hDataCsvStream, MISC_TAG_TRANSACTION);
		Dust.access(MindAccess.Set, hDataCsvStream, APP_ASSEMBLY_MAIN, MIND_ATT_ASSEMBLY_STARTCOMMITS, KEY_ADD);
	}

}
