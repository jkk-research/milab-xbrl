package hu.sze.uni.xbrl.edgar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.utils.DustUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlEdgarAgentProcessSubmissions extends DustAgent implements XbrlEdgarConsts {

	@Override
	protected MindHandle agentProcess() throws Exception {
		MindHandle ret = MIND_TAG_RESULT_PASS;

		Collection updatedDirs = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, MISC_ATT_CONN_MEMBERSET);

		if ( null != updatedDirs ) {
			String path = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, RESOURCE_ATT_URL_PATH);
			File fDataRoot = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, DUST_ATT_IMPL_DATA);

			for (Object idHash : updatedDirs) {
//				String subPath = path + File.pathSeparator + idHash;
				File fDir = new File(fDataRoot, (String) idHash);
				Dust.log(EVENT_TAG_TYPE_TRACE, "Processing directory", fDir.getCanonicalPath());
				if ( fDir.isDirectory() ) {
					ret = MIND_TAG_RESULT_READACCEPT;
					File fc = new File(fDir, EDGAR_COMPANY_INDEX);

					Map compIdx = fromJson(fc);
					if ( null == compIdx ) {
						compIdx = new HashMap<>();
					}

					for (File f : fDir.listFiles()) {
						if ( f.isFile() ) {
							String fName = f.getName();
							if ( fName.endsWith(DUST_EXT_JSON) && !(fName.contains("submissions")) && !(fName.equals(EDGAR_COMPANY_INDEX)) ) {
								String fId = DustUtils.cutPostfix(fName, ".");
								String subPath = path + File.separator + idHash + File.separator;

								Dust.access(MindAccess.Set, subPath + fId + DUST_EXT_CSV, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX, RESOURCE_ATT_PROCESSOR_STREAM, RESOURCE_ATT_URL_PATH);
								for (EdgarSubmissionAtt sa : EdgarSubmissionAtt.values()) {
									Dust.access(MindAccess.Set, sa.name(), MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX, RESOURCE_ATT_PROCESSOR_DATA, MISC_ATT_CONN_MEMBERARR, KEY_ADD);
								}
								Dust.access(MindAccess.Commit, MIND_TAG_ACTION_BEGIN, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX, RESOURCE_ATT_PROCESSOR_STREAM);

								try {
									Map subData = fromJson(f);
									Map filings = (Map) subData.remove("filings");

									String cik = (String) subData.get("cik");
									compIdx.put(cik, subData);

									writeFilings(cik, filings.get("recent"));

									Collection subFiles = (Collection) filings.getOrDefault("files", Collections.EMPTY_LIST);
									for (Object sfName : subFiles) {
										if ( sfName instanceof String ) {
											File fSubFile = new File(fDir, (String) sfName);
											Map subData2 = fromJson(fSubFile);
											writeFilings(cik, subData2);
										} else {
											Dust.log(EVENT_TAG_TYPE_WARNING, "bad filings list", filings.toString());
										}
									}

								} finally {
									Dust.access(MindAccess.Commit, MIND_TAG_ACTION_END, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX, RESOURCE_ATT_PROCESSOR_STREAM);
								}

								Dust.log(EVENT_TAG_TYPE_TRACE, "Processed", f.getCanonicalPath());
							}
						}
					}

					toJson(fc, compIdx);

				} else {
					Dust.log(EVENT_TAG_TYPE_WARNING, "Target is not directory???", fDir.getCanonicalPath());
				}
			}

			Dust.log(EVENT_TAG_TYPE_TRACE, "... and now rebuild company index", fDataRoot.getCanonicalPath());
		}

		return ret;
	}

	private <RetType> RetType fromJson(File f) throws Exception {
		Dust.access(MindAccess.Set, f, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM, RESOURCE_ATT_PROCESSOR_STREAM, DUST_ATT_IMPL_DATA);
		Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM, RESOURCE_ATT_PROCESSOR_STREAM);
		return Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM, RESOURCE_ATT_PROCESSOR_DATA, MISC_ATT_VARIANT_VALUE);
	}

	private void toJson(File f, Object data) throws Exception {
		Dust.access(MindAccess.Set, f, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM, RESOURCE_ATT_PROCESSOR_STREAM, DUST_ATT_IMPL_DATA);
		Dust.access(MindAccess.Set, data, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM, RESOURCE_ATT_PROCESSOR_DATA, MISC_ATT_VARIANT_VALUE);
		Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_JSONDOM, RESOURCE_ATT_PROCESSOR_DATA);
	}

	private void writeFilings(String cik, Object data) throws Exception {
		Map<String, ArrayList> filingData = (Map<String, ArrayList>) data;
		int l = filingData.get(EdgarSubmissionAtt.accessionNumber.name()).size();

		for (int i = 0; i < l; ++i) {
			for (EdgarSubmissionAtt sa : EdgarSubmissionAtt.values()) {
				String key = sa.name();
				String val = EdgarSubmissionAtt.CIK.equals(sa) ? cik : filingData.get(key).get(i).toString();

				Dust.access(MindAccess.Set, val, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX, RESOURCE_ATT_PROCESSOR_DATA, MISC_ATT_CONN_MEMBERMAP, key);
			}
			Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, MIND_TAG_CONTEXT_SELF, EDGARMETA_ATT_CSVSAX, RESOURCE_ATT_PROCESSOR_DATA);
		}
	}
}
