package hu.sze.uni.xbrl.edgar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsData;
import hu.sze.uni.xbrl.XbrlReportLoaderDomBase;
import hu.sze.uni.xbrl.XbrlUtilsCounter;

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
		XbrlUtilsCounter dc = new XbrlUtilsCounter(true);

		DustUtilsData.TableReader trSubIdx = null;
		
		XbrlReportLoaderDomBase.DELETE_ON_ERROR = false;

		try (PrintStream ps = new PrintStream("work/subProcTest.txt"); BufferedReader br = new BufferedReader(new FileReader(edgarSource.fSubmissionIndex))) {
			for (String line; (line = br.readLine()) != null;) {

				pm.step();
				dc.add("Company <ALL>");

				String[] row = line.split("\t");
				if ( null == trSubIdx ) {
					trSubIdx = new DustUtilsData.TableReader(row);
					ps.println(line);
				} else {
					if ( "3711".equals(trSubIdx.get(row, "sic")) ) {
						dc.add("Company <SIC 3711>");
//						++cntCompanies;

						Map<String, ArrayList<String>> reports = new TreeMap<>();

						File fFacts = new File(edgarSource.fFactRoot, trSubIdx.get(row, EdgarHeadFields.__PathPrefix.name()) + ".csv");
						if ( fFacts.isFile() ) {
//							String headLine = null;
							try (BufferedReader brf = new BufferedReader(new FileReader(fFacts))) {
								DustUtilsData.TableReader trf = null;

								for (String linef; (linef = brf.readLine()) != null;) {
									String[] rowf = linef.split("\t");
									if ( null == trf ) {
										trf = new DustUtilsData.TableReader(rowf);
//										headLine = linef;
									} else {
										dc.add("Fact <ALL>");

										String accn = trf.get(rowf, EdgarFactField.accn.name());
										ArrayList<String> l = reports.get(accn);
										if ( null == l ) {
											dc.add("Doc <Referred in fact>");

											l = new ArrayList<>();
//											l.add(headLine);
											reports.put(accn, l);
										}
										l.add(linef);										
									}
								}
							}
//							cntDocRefs += reports.size();
						}

						String[] rowcf = "cik,form,accn,filed,fp,fy,frame,Taxonomy,Concept,Unit,Instant,StartDate,EndDate,Value".split(",");
						DustUtilsData.TableReader trcf = new DustUtilsData.TableReader(rowcf);

						File fSubs = new File(edgarSource.fSubmissionRoot, trSubIdx.get(row, EdgarHeadFields.__PathPrefix.name()) + ".csv");
						if ( fSubs.isFile() ) {
							try (BufferedReader brf = new BufferedReader(new FileReader(fSubs))) {

								DustUtilsData.TableReader trf = null;

								for (String linef; (linef = brf.readLine()) != null;) {
									String[] rowf = linef.split("\t");
									if ( null == trf ) {
										trf = new DustUtilsData.TableReader(rowf);
									} else {
										dc.add("Doc <ALL>");
//										++cntDocLine;

										String docName = (String) trf.get(rowf, EdgarSubmissionAtt.primaryDocument.name());
										docName = DustUtils.csvUnEscape(docName, true);

										if ( DustUtils.isEmpty(docName) ) {
											DustUtils.breakpoint();
										} else {
											dc.add("Doc <Primary doc OK>");
//											++cntDocOK;
										}

										String accn = trf.get(rowf, EdgarSubmissionAtt.accessionNumber.name());
										ArrayList<String> compFacts = reports.remove(accn);
										if ( null != compFacts ) {
											dc.add("Data doc <ALL>");
											String formType = trf.get(rowf, EdgarSubmissionAtt.form.name());

//											String cik = trSubIdx.get(row, EdgarHeadFields.cik.name());
											String pathPrefix = trSubIdx.get(row, EdgarHeadFields.__PathPrefix.name());
											docName = docName.substring(1, docName.length() - 1);

											try {
//												edgarSource.getFiling(cik, pathPrefix, formType, accn, docName);

											} catch (Throwable e) {
												String msg = e.toString();
												msg = DustUtils.getPostfix(msg, ";").trim();
												dc.add("Read error " + msg);
//												DustException.swallow(e);
											}

											File dir = new File(edgarSource.fReportRoot, pathPrefix + "/" + formType + "/" + accn);
											File fVal = new File(dir, accn + POSTFIX_VAL);
											if ( fVal.isFile() ) {
												dc.add("Data doc <Available>");
												
												for ( @SuppressWarnings("unused") String cfl : compFacts ) {
													dc.add("Fact TO MATCH");
												}
																								
												try (BufferedReader brv = new BufferedReader(new FileReader(fVal))) {

													DustUtilsData.TableReader trv = null;
													Map<String, Object> val = new TreeMap<>();
													String[] find = new String[] {"Concept", "Taxonomy", "Instant", "StartDate", "EndDate"};

													for (String linev; (linev = brv.readLine()) != null;) {
														String[] rowv = linev.split("\t");
														if ( null == trv ) {
															trv = new DustUtilsData.TableReader(rowv);
														} else {
															dc.add("Data facts <ALL>");
															
															String matchLine = null;

															val.clear();
															trv.get(rowv, val, "Taxonomy", "Concept", "Unit", "Value", "StartDate", "EndDate", "Instant");
															
															if ( val.isEmpty() ) {
																continue;
															}
															
															String unit = ((String)val.getOrDefault("Unit", "-")).replace("iso4217:", "").replace("xbrli:", "");
															
															for ( Iterator<String> cfIt = compFacts.iterator(); cfIt.hasNext(); ) {
																String cfl = cfIt.next();
																rowcf = cfl.split("\t");
																boolean match = true;
																for ( String k : find ) {
																	if ( !DustUtils.isEqual(val.get(k), trcf.get(rowcf, k))) {
																		match = false;
																		break;
																	}
																}
																if ( match && !"-".equals(unit) && !DustUtils.isEqual(unit, trcf.get(rowcf, "Unit"))) {
																	match = false;
																	break;
																}
																
																if ( match ) {
																	if ( DustUtils.isEqual(val.get("Value"), trcf.get(rowcf, "Value"))) {
																		dc.add("Fact match " + trv.get(rowv, "Type"));
																		
																		if ( null == matchLine ) {
																			matchLine = cfl;																			
																		} else {
																			dc.add("Fact MULTIPLE MATCH");
																			DustUtils.breakpoint();
																		}
//																		cfIt.remove();
//																		break;
																	}
																}
															}
															
															if ( null == matchLine ) {
																dc.add("Fact not found in CF");
//																dc.add("Data Fact not found in CF " + trv.get(rowv, "Type"));
															}
														}
													}
												}
												
												for ( @SuppressWarnings("unused") String cfl : compFacts ) {
//													dc.add("Fact not found in Data");
												}
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

//		Dust.dump(" ", true, "Companies:", cntCompanies, "Referred Docs:", cntDocRefs, "All Submission:", cntDocLine, "Primary document known", cntDocOK, "Successfully loaded", cntDocLoaded);
		dc.dump("Car stats");

		System.out.println(pm);
	}
}
