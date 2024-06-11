package hu.sze.uni.xbrl.charles.t01blockgui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;

import org.json.simple.JSONValue;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.stream.DustStreamUtils;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.uni.xbrl.edgar.XbrlEdgarConsts;

public class T02ReportFinderNarrative extends DustAgent implements XbrlEdgarConsts {

	@Override
	protected MindHandle agentProcess() throws Exception {

		String[] listHead = null;
		File fDataRoot = new File("/Users/lkedves/work/xbrl/data/sources/edgar/submissions");

		try (FileReader fr = new FileReader("temp/SEC EDGAR_SIC3711_20240610.csv");
				BufferedReader br = new BufferedReader(fr)) {
			for (String line = br.readLine(); null != line; line = br.readLine()) {
				String[] ss = line.split("\\t");

				if (null == listHead) {
					listHead = ss;
				} else {

					String cik = ss[0];

					String id = "CIK" + cik;
					String ii = Integer.toString(Integer.parseInt(cik));

					String idHash = DustUtils.getHash2(id, File.separator);
					File fData = new File(fDataRoot, idHash + File.separator + id + DUST_EXT_CSV);

					if (!fData.isFile()) {
						Dust.log(EVENT_TAG_TYPE_INFO, "MISSING", fData.getCanonicalFile());
					}

					File fCompanies = new File(fDataRoot, idHash + File.separator + EDGAR_COMPANY_INDEX);

					try (FileReader frc = new FileReader(fCompanies)) {
						Object jsonRoot = JSONValue.parse(frc);
						Map ci = DustUtils.simpleGet(jsonRoot, ii);
						if ( null == ci ) {
							ci = DustUtils.simpleGet(jsonRoot, cik);
						}
						StringBuilder sb = DustUtils.sbAppend(null, "\t", true, ii);

						if (null == ci) {
							sb = DustUtils.sbAppend(sb, "\t", true, "MISSING");
						} else {
							sb = DustUtils.sbAppend(sb, "\t", true, "OK", ci.get("name"), ci.get("stateOfIncorporation"));
						}
						Dust.log(EVENT_TAG_TYPE_INFO, sb);
					}

					try (FileReader frd = new FileReader(fData); BufferedReader brd = new BufferedReader(frd)) {
						String[] dataHead = null;
						
						boolean found = false;
						DustDevCounter cnt = new DustDevCounter("Form types", true);

						for (String ld = brd.readLine(); null != ld; ld = brd.readLine()) {
							String[] sd = ld.split("\\t");
							if (null == dataHead) {
								dataHead = sd;
							} else {
								cnt.add(sd[2]);
								if (DustUtils.isEqual(sd[2], "10-K")) {
									Dust.log(EVENT_TAG_TYPE_INFO, ld);
									found = true;
								}
							}
						}
						
						if ( !found ) {
							Dust.log(EVENT_TAG_TYPE_INFO, DustUtils.sbAppend(null, "\t", true, ii, "NO 10-K", DustStreamUtils.csvEscape(cnt.toString(), true)));
						}
					}
				}
			}
		}

		return MIND_TAG_RESULT_ACCEPT;
	}
}
