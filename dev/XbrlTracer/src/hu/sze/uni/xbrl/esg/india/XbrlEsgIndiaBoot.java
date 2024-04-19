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
		Dust.access(MindAccess.Insert, hAgtDownload, hDataWebRequest, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

	  Dust.access(MindAccess.Insert, "accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "accept-language: en-US,en;q=0.9,hu-HU;q=0.8,hu;q=0.7", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "cache-control: no-cache", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "cookie: _abck=F582EA0B2C10F21D09513B6A46F1AFD6~-1~YAAQzB0SAsaWxPSOAQAAURAa9wtNj1P0QtMMtYI5brcSrhziY7eGx8uMh8UCL0/ZxicKdJR2TOmR/AsDVZlJ6p2QcdAHqVAKBNqgzgX+nW9OgKKTHetyVbArqAkFnZBmAGjQ8YChEAn3IQW/SrTPPVdLr2gCEkNwQW0DnOD17aALKgxCcl9ngWRJ9HgwjaSgSA53RGVMLP53jx05fyiDUDdhkO8+ZCgkWhkSaerQT1CUNhkX+SmDrssR42wzvruUAFI3oHD+hJfAPlQV3IqmV9CbOirAelpvDzbBsMj7NbnhonXU8YkF0KyIVnMb6dmstFVvKhm22ZsknCYHGhMsdY40Sjph/Cv/YhvmrI/B164vOgZ9uBqrg22+eHnHDQ==~-1~-1~-1; ak_bmsc=2EA695D38482FB35A4F3B63D70596CF6~000000000000000000000000000000~YAAQzB0SAseWxPSOAQAAURAa9xesBUVx8Eh1mPzAUQR1O9fb/aVYn62Dzd3CXZvmlDQMM6N++CeVSg6SDR7FGhS8YbPNDHRzsvgeX3zSTAfWl/srQmtiP36egfTMGL7oxI7msOLkMPypyX6gBURr2sR0mQ4nOGP+mCWKI9LdmWFjRhIVXVaMY2RNj3zADfZqck3x16K8wNCiP7Tk0+6aCUDh3nLRF4w5utz7v8wbBpXncWYBAvSRnU8skt/cUe0vYzASMDlqL9nO0HDPrbxamHx7d5lIjiILh1Unn5qdSwrpOmGeSzxkzzOrJLik9f5GlOE5jaha/s2R6iU8tTcmXGZ4CNY7DtQxn3gMHfxipgIK6HQNVpJq/KD0EmbKD7lzZ2asuuruAbL3S+Wq; bm_sz=5BC387B8BEF4BCC5C71AA4648DFFB0DA~YAAQzB0SAsiWxPSOAQAAURAa9xf9thcAMwMeBwO3jGlFL9bwSkZZ6T/NlI05i58AQX1GSHa0dgNv2skvM/PUcVLMdRsTmguU+gWwIIIOQXBqb5b7nMDSzYQDUplbYc0nzE91KmhK31apnENYbaQnGlR7KxYMhX89o+k2fbck/ZdHs/7PxB6GWP3EFyMfU+XLroYLUu1yuCcIYoFbIe/mCvAzM5dAwioNyc9wGRI7byNXmZn06tbxjpx4lXVvTvzimCwQh9kjsfaiYMeMruuVHlmQMBWsF1Zl/9fP8oH9mAv4Aqg4TwkUS12XIa3YjJzjKfn2Sq6B5ormicEoi39dVyL3qlQCP+RyA8pfTN4r1/D3IUJ+JiN04ElUwypmEw/2HlmWsy9VU+lOZNkx4W8=~3421240~3682866", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "pragma: no-cache", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "sec-ch-ua: \"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "sec-ch-ua-mobile: ?0", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "sec-ch-ua-platform: \"macOS\"", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "sec-fetch-dest: document", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "sec-fetch-mode: navigate", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "sec-fetch-site: none", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "sec-fetch-user: ?1", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "upgrade-insecure-requests: 1", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
	  Dust.access(MindAccess.Insert, "user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36", hAgtDownload, NET_ATT_SRVCALL_HEADERS, KEY_ADD);
		
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
		MindHandle hDataReportRowText = DustDevUtils.newHandle(XBRLTEST_UNIT, MISC_ASP_VARIANT, "Report CSV row text");

		MindHandle hAgtXmlLoader = DustDevUtils.registerAgent(XBRLTEST_UNIT, XBRLDOCK_NAR_XMLLOADER, "XML loader");
		Dust.access(MindAccess.Set, hDataReportRowData, hAgtXmlLoader, XBRLDOCK_ATT_XMLLOADER_ROWDATA);
		Dust.access(MindAccess.Set, hDataReportRowText, hAgtXmlLoader, XBRLDOCK_ATT_XMLLOADER_ROWTEXT);
		Dust.access(MindAccess.Insert, hAgtXmlLoader, hDataReportContent, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataReportStreamData = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report Data CSV stream");
		DustDevUtils.setTag(hDataReportStreamData, MISC_TAG_DIRECTION_OUT, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataReportStreamData, RESOURCE_TAG_STREAMTYPE_TEXT, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtCacheRoot, hDataReportStreamData, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtCsvReportWriterData = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_CSVSAX, "CSV Data writer");
		Dust.access(MindAccess.Set, hDataReportStreamData, hAgtCsvReportWriterData, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataReportRowData, hAgtCsvReportWriterData, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtCsvReportWriterData, hDataReportRowData, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hDataReportStreamText = DustDevUtils.newHandle(XBRLTEST_UNIT, RESOURCE_ASP_STREAM, "Report Text CSV stream");
		DustDevUtils.setTag(hDataReportStreamText, MISC_TAG_DIRECTION_OUT, MISC_TAG_DIRECTION);
		DustDevUtils.setTag(hDataReportStreamText, RESOURCE_TAG_STREAMTYPE_TEXT, RESOURCE_TAG_STREAMTYPE);
		Dust.access(MindAccess.Insert, hAgtCacheRoot, hDataReportStreamText, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		MindHandle hAgtCsvReportWriterText = DustDevUtils.registerAgent(XBRLTEST_UNIT, RESOURCE_NAR_CSVSAX, "CSV Text writer");
		Dust.access(MindAccess.Set, hDataReportStreamText, hAgtCsvReportWriterText, RESOURCE_ATT_PROCESSOR_STREAM);
		Dust.access(MindAccess.Set, hDataReportRowText, hAgtCsvReportWriterText, RESOURCE_ATT_PROCESSOR_DATA);
		Dust.access(MindAccess.Insert, hAgtCsvReportWriterText, hDataReportRowText, MIND_ATT_KNOWLEDGE_LISTENERS, KEY_ADD);

		DustDevUtils.setTag(hDataCsvStream, MISC_TAG_TRANSACTION);
		Dust.access(MindAccess.Set, hDataCsvStream, APP_ASSEMBLY_MAIN, MIND_ATT_ASSEMBLY_STARTCOMMITS, KEY_ADD);
	}

}
