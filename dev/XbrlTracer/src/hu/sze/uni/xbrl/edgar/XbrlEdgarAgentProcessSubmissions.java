package hu.sze.uni.xbrl.edgar;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.stream.DustUtilsSream;
import hu.sze.milab.dust.utils.DustUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlEdgarAgentProcessSubmissions extends DustAgent implements XbrlEdgarConsts {

	@Override
	public MindHandle agentProcess() throws Exception {
		MindHandle ret = MIND_TAG_RESULT_PASS;

		Collection updatedDirs = Dust.access(MIND_TAG_CONTEXT_TARGET, MIND_TAG_ACCESS_PEEK, null, MISC_ATT_CONN_MEMBERSET);

		if ( null != updatedDirs ) {
			File fDataRoot = Dust.access(MIND_TAG_CONTEXT_TARGET, MIND_TAG_ACCESS_PEEK, null, MISC_ATT_VARIANT_VALUE);
			JSONParser parser = new JSONParser();

			for (Object idHash : updatedDirs) {
				File fDir = new File(fDataRoot, (String) idHash);
				Dust.log(EVENT_TAG_TYPE_TRACE, "Processing directory", fDir.getCanonicalPath());
				if ( fDir.isDirectory() ) {
					ret = MIND_TAG_RESULT_READACCEPT;
					Map compIdx = null;
					File fc = new File(fDir, EDGAR_COMPANY_INDEX);
					if ( fc.isFile() ) {
						try (FileReader fr = new FileReader(fDir)) {
							compIdx = (Map<String, Object>) parser.parse(fr);
						}
					}
					if ( null == compIdx ) {
						compIdx = new HashMap<>();
					}

					for (File f : fDir.listFiles()) {
						if ( f.isFile() ) {
							String fName = f.getName();
							if ( fName.endsWith(DUST_EXT_JSON) && !(fName.contains("submissions")) && !(fName.equals(EDGAR_COMPANY_INDEX)) ) {
								String fId = DustUtils.cutPostfix(fName, ".");
								File fFilings = new File(fDir, fId + DUST_EXT_CSV);

								try (FileReader fr = new FileReader(f); FileWriter fw = new FileWriter(fFilings)) {
									Map subData = (Map) parser.parse(fr);
									Map filings = (Map) subData.remove("filings");

									String cik = (String) subData.get("cik");
									compIdx.put(cik, subData);

									for (EdgarSubmissionAtt sa : EdgarSubmissionAtt.values()) {
										fw.write(sa.name());
										fw.write("\t");
									}
									fw.write("\n");
									writeFilings(cik, fw, filings.get("recent"));

									Collection subFiles = (Collection) filings.getOrDefault("files", Collections.EMPTY_LIST);
									for (Object sfName : subFiles) {
										File fSubFile = new File(fDir, (String) sfName);
										try (FileReader frs = new FileReader(fSubFile)) {
											Object subData2 = parser.parse(frs);
											writeFilings(cik, fw, subData2);
										}
									}
								}

								Dust.log(EVENT_TAG_TYPE_TRACE, "Would process", f.getCanonicalPath());
							}
						}
					}

					try (FileWriter fw = new FileWriter(fc)) {
						JSONObject.writeJSONString(compIdx, fw);
						fw.flush();
						fw.close();
					}
				} else {
					Dust.log(EVENT_TAG_TYPE_WARNING, "Target is not directory???", fDir.getCanonicalPath());
				}
			}

			Dust.log(EVENT_TAG_TYPE_TRACE, "... and now rebuild company index", fDataRoot.getCanonicalPath());
		}

		return ret;
	}

	private void writeFilings(String cik, FileWriter fw, Object data) throws Exception {
		Map<String, ArrayList> filingData = (Map<String, ArrayList>) data;

		int l = filingData.get(EdgarSubmissionAtt.accessionNumber.name()).size();

		for (int i = 0; i < l; ++i) {
			for (EdgarSubmissionAtt sa : EdgarSubmissionAtt.values()) {
				String val = EdgarSubmissionAtt.CIK.equals(sa) ? cik : filingData.get(sa.name()).get(i).toString();
				if ( sa.str ) {
					val = DustUtilsSream.csvEscape(val, true);
				}
				fw.write(val);
				fw.write("\t");
			}
			fw.write("\n");
		}
		
		fw.flush();
	}
}
