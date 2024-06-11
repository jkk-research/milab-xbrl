package hu.sze.uni.xbrl.charles.t01blockgui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.Map;

import org.json.simple.JSONValue;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.dev.DustDevProcMon;
import hu.sze.milab.dust.stream.DustStreamUtils;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsFile;
import hu.sze.uni.xbrl.edgar.XbrlEdgarConsts;

@SuppressWarnings("unused")
public class T02ReportFinderNarrative extends DustAgent implements XbrlEdgarConsts {

	@Override
	protected MindHandle agentProcess() throws Exception {

		countTypes();

		return MIND_TAG_RESULT_ACCEPT;
	}

	private void countTypes() throws Exception {
		String path = "/Users/lkedves/work/xbrl/data/sources/edgar/submissions";
		File fDataRoot = new File(path);
		
		FileFilter ff = new DustUtilsFile.ExtFilter(DUST_EXT_CSV);
		DustDevCounter cnt = new DustDevCounter("Form types", true);
		DustDevCounter cntBlock = new DustDevCounter("Report count blocks", true);
		
		DustDevProcMon pm = new DustDevProcMon("File process", 10000);

		for (File h1 : fDataRoot.listFiles(DustUtilsFile.FF_DIR)) {
			for (File h2 : h1.listFiles(DustUtilsFile.FF_DIR)) {
				for (File f : h2.listFiles(ff)) {
					
					pm.step();
					
					try (FileReader frd = new FileReader(f); BufferedReader brd = new BufferedReader(frd)) {
						String[] dataHead = null;
						
						boolean found = false;
						int rc = 0;

						for (String ld = brd.readLine(); null != ld; ld = brd.readLine()) {
							String[] sd = ld.split("\\t");
							if (null == dataHead) {
								dataHead = sd;
							} else {
								String type = sd[2];
								cnt.add("<< ALL >>\t");
								
								++rc;
								
								cnt.add(type + "\t");
								
								String x = "\t" + (DustUtils.isEqual("1", sd[7]) ? "1" : "0") + (DustUtils.isEqual("1", sd[8]) ? "1" : "0");
								cnt.add(type + x);
//								if (DustUtils.isEqual(type, "10-K")) {
//									Dust.log(EVENT_TAG_TYPE_INFO, ld);
//									found = true;
//								}
							}
						}
						
						int block = 100 * ((rc / 100) + 1);
						
						cntBlock.add(block);
					}
				}				
			}
		}

		Dust.log(EVENT_TAG_TYPE_INFO, cnt);
		Dust.log(EVENT_TAG_TYPE_INFO, cntBlock);
		Dust.log(EVENT_TAG_TYPE_INFO, pm);
	}

	private void collect10K() throws Exception {
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
	}
}
