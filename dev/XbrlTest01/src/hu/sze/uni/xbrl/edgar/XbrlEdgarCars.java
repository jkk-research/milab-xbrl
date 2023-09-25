package hu.sze.uni.xbrl.edgar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Set;
import java.util.TreeSet;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsData;
import hu.sze.uni.xbrl.XbrlReportLoaderDomBase;

public class XbrlEdgarCars implements XbrlEdgarConsts {

	XbrlEdgarSource edgarSource;

	public XbrlEdgarCars() {

		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");

		edgarSource = new XbrlEdgarSource(dataRoot);
	}

	public static void main(String[] args) throws Exception {
		Dust.main(args);

		XbrlEdgarCars ec = new XbrlEdgarCars();

		ec.procCarsDirect();

		ec.edgarSource.dc.dump("Summary");
	}

	private void procCarsDirect() throws Exception {
		DustUtils.ProcessMonitor pm = new DustUtils.ProcessMonitor("Car manufacturing collection", 10000);

		long cntCompanies = 0;
		long cntDocRefs = 0;
		long cntDocLine = 0;
		long cntDocOK = 0;
		long cntDocLoaded = 0;

		DustUtilsData.TableReader trSubIdx = null;

		XbrlReportLoaderDomBase.DELETE_ON_ERROR = false;

		try (PrintStream ps = new PrintStream("work/subProcTest.txt"); BufferedReader br = new BufferedReader(new FileReader(edgarSource.fSubmissionIndex))) {
			for (String line; (line = br.readLine()) != null;) {

				pm.step();

				String[] row = line.split("\t");
				if ( null == trSubIdx ) {
					trSubIdx = new DustUtilsData.TableReader(row);
					ps.println(line);
				} else {
					if ( "3711".equals(trSubIdx.get(row, "sic")) ) {
						++cntCompanies;

						Set<String> reports = new TreeSet<>();

						File fFacts = new File(edgarSource.fFactRoot, trSubIdx.get(row, EdgarHeadFields.__PathPrefix.name()) + ".csv");
						if ( fFacts.isFile() ) {
							try (BufferedReader brf = new BufferedReader(new FileReader(fFacts))) {
								DustUtilsData.TableReader trf = null;

								for (String linef; (linef = brf.readLine()) != null;) {
									String[] rowf = linef.split("\t");
									if ( null == trf ) {
										trf = new DustUtilsData.TableReader(rowf);
									} else {
										String accn = trf.get(rowf, EdgarFactField.accn.name());
										reports.add(accn);
									}
								}
							}
							cntDocRefs += reports.size();
						}

						File fSubs = new File(edgarSource.fSubmissionRoot, trSubIdx.get(row, EdgarHeadFields.__PathPrefix.name()) + ".csv");
						if ( fSubs.isFile() ) {
							try (BufferedReader brf = new BufferedReader(new FileReader(fSubs))) {

								DustUtilsData.TableReader trf = null;

								for (String linef; (linef = brf.readLine()) != null;) {
									String[] rowf = linef.split("\t");
									if ( null == trf ) {
										trf = new DustUtilsData.TableReader(rowf);
									} else {
										++cntDocLine;

										String docName = (String) trf.get(rowf, EdgarSubmissionAtt.primaryDocument.name());
										docName = DustUtils.csvUnEscape(docName, true);

										if ( DustUtils.isEmpty(docName) ) {
											DustUtils.breakpoint();
										} else {
											++cntDocOK;
										}

										String accn = trf.get(rowf, EdgarSubmissionAtt.accessionNumber.name());
										if ( reports.remove(accn) ) {
											String formType = trf.get(rowf, EdgarSubmissionAtt.form.name());

											String cik = trSubIdx.get(row, EdgarHeadFields.cik.name());
											String pathPrefix = trSubIdx.get(row, EdgarHeadFields.__PathPrefix.name());
											docName = docName.substring(1, docName.length() - 1);

											try {
												edgarSource.getFiling(cik, pathPrefix, formType, accn, docName);

											} catch (Throwable e) {
												DustException.swallow(e);
											}
											
											File dir = new File(edgarSource.fReportRoot, pathPrefix + "/" + formType + "/" + accn);
											File fVal = new File(dir, accn + POSTFIX_VAL);
											if ( fVal.isFile() ) {
												++cntDocLoaded;
											}
										}
									}
								}

								if ( !reports.isEmpty() ) {
									DustUtils.breakpoint();
								}
							}
						}
					}
				}
			}
		}

		Dust.dump(" ", true, "Companies:", cntCompanies, "Referred Docs:", cntDocRefs, "All Submission:", cntDocLine, "Primary document known", cntDocOK, "Successfully loaded", cntDocLoaded);

		pm.toString();
	}
}
