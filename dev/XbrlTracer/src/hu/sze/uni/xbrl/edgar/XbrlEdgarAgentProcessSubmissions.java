package hu.sze.uni.xbrl.edgar;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.dev.DustDevProcMon;
import hu.sze.milab.dust.utils.DustUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlEdgarAgentProcessSubmissions extends DustAgent implements XbrlEdgarConsts {
	
	DustDevProcMon pm = new DustDevProcMon("Company process", 10000);

	@Override
	protected MindHandle agentProcess() throws Exception {
		MindHandle ret = MIND_TAG_RESULT_PASS;

		Collection updatedDirs = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_SOURCE,
				MISC_ATT_CONN_MEMBERSET);

		if (null == updatedDirs) {
			Dust.access(MindAccess.Commit, MindAction.Process, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_SOURCE);
		}

		String path = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_SOURCE,
				RESOURCE_ATT_URL_PATH);
		File fDataRoot = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_SOURCE,
				DUST_ATT_IMPL_DATA);

		Dust.log(EVENT_TAG_TYPE_TRACE, "... and now rebuild company index", fDataRoot.getCanonicalPath());

		for (EdgarSubmissionAtt sa : EdgarSubmissionAtt.values()) {
			Dust.access(MindAccess.Set, sa.name(), MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX, RESOURCE_ATT_PROCESSOR_DATA,
					MISC_ATT_CONN_MEMBERARR, KEY_ADD);
		}

		if (null != updatedDirs) {
			for (Object idHash : updatedDirs) {
				File fDir = new File(fDataRoot, (String) idHash);
				String subPath = path + File.separator + idHash + File.separator;

				ret = processDir(fDir, subPath);
			}
		} else {
			FileFilter dirFilter = new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory();
				}
			};

			for (File h1 : fDataRoot.listFiles(dirFilter)) {
				for (File h2 : h1.listFiles(dirFilter)) {
					String subPath = path + File.separator + h1.getName() + File.separator + h2.getName() + File.separator;
					ret = processDir(h2, subPath);
				}
			}

			ret = MIND_TAG_RESULT_ACCEPT;
		}

		Dust.log(EVENT_TAG_TYPE_INFO, "process complete", pm.toString());

		return ret;
	}

	private MindHandle processDir(File fDir, String subPath) throws Exception {
		MindHandle ret = MIND_TAG_RESULT_PASS;

//		Dust.log(EVENT_TAG_TYPE_TRACE, "Processing directory", fDir.getCanonicalPath());
		if (fDir.isDirectory()) {
			ret = MIND_TAG_RESULT_READACCEPT;
			File fc = new File(fDir, EDGAR_COMPANY_INDEX);

//			Map compIdx = fromJson(fc);
//			if (null == compIdx) {
//				compIdx = new HashMap<>();
//			}
			Map compIdx = new HashMap<>();

			for (File f : fDir.listFiles()) {
				if (f.isFile()) {
					String fName = f.getName();
					if (fName.endsWith(DUST_EXT_JSON) && !(fName.contains("submissions"))
							&& !(fName.equals(EDGAR_COMPANY_INDEX))) {
						String fId = DustUtils.cutPostfix(fName, ".");

						Dust.access(MindAccess.Set, subPath + fId + DUST_EXT_CSV, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX,
								RESOURCE_ATT_PROCESSOR_STREAM, RESOURCE_ATT_URL_PATH);
						Dust.access(MindAccess.Commit, MIND_TAG_ACTION_BEGIN, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX,
								RESOURCE_ATT_PROCESSOR_STREAM);

						try {
							Map subData = fromJson(f);
							Map filings = (Map) subData.remove("filings");

							String cik = (String) subData.get("cik");
							compIdx.put(cik, subData);

							writeFilings(cik, filings.get("recent"));

							Collection subFiles = (Collection) filings.getOrDefault("files", Collections.EMPTY_LIST);
							for (Object sf : subFiles) {
								String subFileName = (String) ((sf instanceof String) ? sf : ((Map) sf).get("name"));

								File fSubFile = new File(fDir, subFileName);
								Map subData2 = fromJson(fSubFile);
								writeFilings(cik, subData2);
							}

						} finally {
							Dust.access(MindAccess.Commit, MIND_TAG_ACTION_END, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX,
									RESOURCE_ATT_PROCESSOR_STREAM);
						}

						pm.step();
//						Dust.log(EVENT_TAG_TYPE_TRACE, "Processed", f.getCanonicalPath());
					}
				}
			}

			toJson(fc, compIdx);

		} else {
			Dust.log(EVENT_TAG_TYPE_WARNING, "Target is not directory???", fDir.getCanonicalPath());
		}
		return ret;
	}

	private <RetType> RetType fromJson(File f) throws Exception {
		Dust.access(MindAccess.Set, f, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM, RESOURCE_ATT_PROCESSOR_STREAM,
				DUST_ATT_IMPL_DATA);
		Dust.access(MindAccess.Set, null, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM, RESOURCE_ATT_PROCESSOR_DATA,
				MISC_ATT_VARIANT_VALUE);
		Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM,
				RESOURCE_ATT_PROCESSOR_STREAM);
		return Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM, RESOURCE_ATT_PROCESSOR_DATA,
				MISC_ATT_VARIANT_VALUE);
	}

	private void toJson(File f, Object data) throws Exception {
		Dust.access(MindAccess.Set, f, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM, RESOURCE_ATT_PROCESSOR_STREAM,
				DUST_ATT_IMPL_DATA);
		Dust.access(MindAccess.Set, data, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM, RESOURCE_ATT_PROCESSOR_DATA,
				MISC_ATT_VARIANT_VALUE);
		Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM,
				RESOURCE_ATT_PROCESSOR_DATA);
	}

	private void writeFilings(String cik, Object data) throws Exception {
		Map<String, ArrayList> filingData = (Map<String, ArrayList>) data;
		int l = filingData.get(EdgarSubmissionAtt.accessionNumber.name()).size();

		for (int i = 0; i < l; ++i) {
			for (EdgarSubmissionAtt sa : EdgarSubmissionAtt.values()) {
				String key = sa.name();
				String val = EdgarSubmissionAtt.CIK.equals(sa) ? cik : DustUtils.toString(filingData.get(key).get(i));

				Dust.access(MindAccess.Set, val, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX, RESOURCE_ATT_PROCESSOR_DATA,
						MISC_ATT_CONN_MEMBERMAP, key);
			}
			Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX,
					RESOURCE_ATT_PROCESSOR_DATA);
		}
	}
}
