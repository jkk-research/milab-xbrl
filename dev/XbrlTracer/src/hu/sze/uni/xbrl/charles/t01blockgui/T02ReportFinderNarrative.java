package hu.sze.uni.xbrl.charles.t01blockgui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONValue;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.dev.DustDevProcMon;
import hu.sze.milab.dust.dev.DustDevUtils;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsFile;
import hu.sze.uni.xbrl.edgar.XbrlEdgarConsts;
import hu.sze.uni.xbrl.edgar.XbrlEdgarUtils;

@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
public class T02ReportFinderNarrative extends DustAgent implements XbrlEdgarConsts {

	@Override
	protected MindHandle agentProcess() throws Exception {
		
		int mode = 0;
		
		switch ( mode ) {
		case 0:
			collectListed10K();
			break;
		case 1:
			countTypes();
			break;
		}

		return MIND_TAG_RESULT_ACCEPT;
	}

	private void countTypes() throws Exception {
		String path = "/Users/lkedves/work/xbrl/data/sources/edgar/submissions";
		File fDataRoot = new File(path);
		
		FileFilter ff = new DustUtilsFile.ExtFilter(DUST_EXT_CSV);
		DustDevCounter cnt = new DustDevCounter("Form types", true);
		DustDevCounter cntBlock = new DustDevCounter("Report count blocks", true);
		DustDevCounter cntSic = new DustDevCounter("Sic", true);
		
		DustDevProcMon pm = new DustDevProcMon("File process", 10000);

		for (File h1 : fDataRoot.listFiles(DustUtilsFile.FF_DIR)) {
			for (File h2 : h1.listFiles(DustUtilsFile.FF_DIR)) {
				
				File fCompanies = new File(h2, EDGAR_COMPANY_INDEX);
				Map<String, Map> jsonRoot = null;

				try (FileReader frc = new FileReader(fCompanies)) {
					jsonRoot = (Map<String, Map>) JSONValue.parse(frc);
					for ( Map m : jsonRoot.values() ) {
						Object sic = m.getOrDefault("sic", "MISSING");
						cntSic.add(sic);
					}
				}
				
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
								String cik = sd[0];
								
								String sic1 = DustUtils.simpleGet(jsonRoot, cik, "sic");
								
								if ( DustUtils.isEmpty(sic1) ) {
									sic1 = "<< NO SIC >>";
								}
								
								String repDate = sd[4];
								
								if ( DustUtils.isEmpty(repDate) ) {
									cnt.add("<< Missing report date >>\t");
									repDate = "<< NO DATE >>";
								} else {
									repDate = repDate.split("-")[0];
								}
								
								String type = sd[2];
								cnt.add("<< ALL >>\t" + sic1 + "\t" + repDate + "\t");
								
								++rc;
								
								cnt.add(type + "\t" + sic1 + "\t << ALL YEARS >>\t");
								cnt.add(type + "\t" + sic1 + "\t" + repDate + "\t");
								
								String x = "\t" + sic1 + "\t" + repDate + "\t" + (DustUtils.isEqual("1", sd[7]) ? "1" : "0") + (DustUtils.isEqual("1", sd[8]) ? "1" : "0");
								cnt.add(type + x);
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
		Dust.log(EVENT_TAG_TYPE_INFO, cntSic);
		Dust.log(EVENT_TAG_TYPE_INFO, pm);
	}

	private void collectListed10K() throws Exception {
		String[] listHead = null;
		File fDataRoot = new File("/Users/lkedves/work/xbrl/data/sources/edgar/submissions");
		File fReportRoot = new File("/Users/lkedves/work/xbrl/data/sources/edgar/reports");
		
		Set<String> pfHtml = new HashSet<>();
		pfHtml.add("htm");
		pfHtml.add("html");
		pfHtml.add("xhtml");

		Set<String> pfXml = new HashSet<>();
		pfXml.add("xml");
		pfXml.add("xbrl");

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
//						Dust.log(EVENT_TAG_TYPE_INFO, sb);
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
								String doc = (13 < sd.length) ? sd[13] : null;
								
								if (DustUtils.isEqual(sd[2], "10-K")) {
									boolean fmtXml = DustUtils.isEqual("1", sd[7]);
									boolean fmtXHtml = DustUtils.isEqual("1", sd[8]);

									Collection<String> reqPf = fmtXml ? fmtXHtml ? pfHtml : pfXml : null;

									File f = XbrlEdgarUtils.getFiling(fReportRoot, sd[0], sd[1], doc, reqPf);
									
									if (fmtXml) {
										String fn = f.getName();
										String ext = DustUtils.getPostfix(fn, ".").toLowerCase();
										String msg = fmtXHtml ? "inlineXbrl" : "Xbrl";
										boolean ok = reqPf.contains(ext);
										
										if ( !ok ) {
											DustDevUtils.breakpoint();
											XbrlEdgarUtils.getFiling(fReportRoot, sd[0], sd[1], doc, reqPf);
										}
										Dust.log(EVENT_TAG_TYPE_INFO, msg, ok, f.getCanonicalFile());
									}
									
									found = true;
								}
							}
						}
						
						if ( !found ) {
//							Dust.log(EVENT_TAG_TYPE_INFO, DustUtils.sbAppend(null, "\t", true, ii, "NO 10-K", DustStreamUtils.csvEscape(cnt.toString(), true)));
						}
					}
				}
			}
		}
	}
}
