package hu.sze.uni.xbrl.edgar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.parser.JSONParser;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsData;
import hu.sze.milab.dust.utils.DustUtilsData.TableReader;
import hu.sze.milab.dust.utils.DustUtilsFile;
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
//		ec.checkJsonIndex();

	}

	void checkJsonIndex() throws Exception {
		File dir = edgarSource.fReportRoot;
		int cut = dir.getCanonicalPath().length();
		JSONParser parser = new JSONParser();

		Pattern ptRFiles = Pattern.compile("R(\\d+)\\.(xml|htm)");
		Pattern ptXbrlExtFiles = Pattern.compile(".*_(cal|def|lab|pre)\\.xml");

		DustUtils.ProcessMonitor pm = new DustUtils.ProcessMonitor("Car manufacturing file index", 0);

		try (PrintStream ps = new PrintStream("work/carsJson.csv")) {

			ps.println("Index\tFile\tType\tSize\tLastModified\tDownload");

			FileFilter ff = new FileFilter() {
				@Override
				public boolean accept(File f) {
					if ( f.isFile() && f.getName().endsWith(EXT_JSON) ) {
						pm.step();
						try (Reader fr = new FileReader(f)) {
							String shortName = f.getCanonicalPath().substring(cut + 1);

							Object root = parser.parse(fr);
							Collection<Map<String, Object>> items = Dust.access(root, MindAccess.Peek, Collections.EMPTY_LIST, "directory", "item");
							ArrayList<Map<String, Object>> sel = new ArrayList<>();
							long maxLen = 0;
							Object si = null;

							for (Map<String, Object> item : items) {
								String name = (String) item.get("name");
								Matcher m = ptRFiles.matcher(name);
								if ( !m.matches() ) {
									sel.add(item);
									if ( name.endsWith(".xml") ) {
										Matcher m2 = ptXbrlExtFiles.matcher(name);
										if ( !m2.matches() ) {
											int l = Integer.parseInt((String) item.get("size"));
											if ( l > maxLen ) {
												maxLen = l;
												si = item;
											}
										}
									}
								}
							}

							String selName = edgarSource.selectFileFromJsonIndex(f);

							for (Map<String, Object> item : sel) {
								String name = (String) item.get("name");
								ps.println(DustUtils.sbAppend(null, "\t", true, shortName, name, item.get("type"), item.get("size"), item.get("last-modified"), si == item, DustUtils.isEqual(name, selName)));
							}

							ps.flush();
						} catch (Exception e) {
							DustException.swallow(e);
						}
					}
					return false;
				}
			};

			DustUtilsFile.searchRecursive(dir, ff);
			System.out.println(pm);
		}
	}

	void procCarsDirect() throws Exception {
		DustUtils.ProcessMonitor pm = new DustUtils.ProcessMonitor("Car manufacturing collection", 10000);
		XbrlUtilsCounter dc = new XbrlUtilsCounter(true);

		DustUtilsData.TableReader trSubIdx = null;

		XbrlReportLoaderDomBase.DELETE_ON_ERROR = false;
		
		boolean unitAware = false;
		String selAccn = null;
		selAccn = "0000950123-11-073440";
		
		try (PrintStream ps = new PrintStream("work/cars.csv"); BufferedReader br = new BufferedReader(new FileReader(edgarSource.fSubmissionIndex))) {

			ps.println(
					"CIK\tCompany Name\tForm Type\tReport Date\tFiling Date\tAccession Number\tFile Size\tDocument name\tFactRefs\tDirectory\tData file\tData lines\tData valid\tMatch\tFactMiss\tDataMiss");

			for (String line; (line = br.readLine()) != null;) {

				pm.step();
				dc.add("Company <ALL>");

				String[] row = line.split("\t");
				if ( null == trSubIdx ) {
					trSubIdx = new DustUtilsData.TableReader(row);
				} else {
					if ( "3711".equals(trSubIdx.get(row, "sic")) ) {
						dc.add("Company <SIC 3711>");
//						++cntCompanies;

						Map<String, ArrayList<String>> reports = new TreeMap<>();

						File fFacts = new File(edgarSource.fFactRoot, trSubIdx.get(row, EdgarHeadFields.__PathPrefix.name()) + ".csv");
						if ( fFacts.isFile() ) {
							try (BufferedReader brf = new BufferedReader(new FileReader(fFacts))) {
								DustUtilsData.TableReader trf = null;

								for (String linef; (linef = brf.readLine()) != null;) {
									String[] rowf = linef.split("\t");
									if ( null == trf ) {
										trf = new DustUtilsData.TableReader(rowf);
									} else {
										dc.add("Fact <ALL>");

										String accn = trf.get(rowf, EdgarFactField.accn.name());
										ArrayList<String> l = reports.get(accn);
										if ( null == l ) {
											dc.add("Doc <Referred in fact>");

											l = new ArrayList<>();
											reports.put(accn, l);
										}
										l.add(linef);
									}
								}
							}
						}

						String[] rowcf = "cik,form,accn,filed,fp,fy,frame,Taxonomy,Concept,Unit,Instant,StartDate,EndDate,Value".split(",");
						DustUtilsData.TableReader trcf = new DustUtilsData.TableReader(rowcf);

						File fSubs = new File(edgarSource.fSubmissionRoot, trSubIdx.get(row, EdgarHeadFields.__PathPrefix.name()) + ".csv");
						if ( fSubs.isFile() ) {
							try (BufferedReader brf = new BufferedReader(new FileReader(fSubs))) {

								dc.add("Company <Submissions found>");

								DustUtilsData.TableReader trf = null;

								for (String linef; (linef = brf.readLine()) != null;) {
									String[] rowf = linef.split("\t");
									if ( null == trf ) {
										trf = new DustUtilsData.TableReader(rowf);
									} else {
										dc.add("Doc <ALL>");
//										++cntDocLine;

										String accn = trf.get(rowf, EdgarSubmissionAtt.accessionNumber.name());

										if ( !DustUtils.isEmpty(selAccn) && !selAccn.equals(accn)) {
											continue;
										}

										String docName = (String) trf.get(rowf, EdgarSubmissionAtt.primaryDocument.name());
										docName = DustUtils.csvUnEscape(docName, true);

										if ( DustUtils.isEmpty(docName) ) {
											DustUtils.breakpoint();
										} else {
											dc.add("Doc <Primary doc OK>");
//											++cntDocOK;
										}

										StringBuilder sbLog = DustUtils.sbAppend(null, "\t", true, trSubIdx.get(row, "cik"), trSubIdx.get(row, "name"), trf.get(rowf, "form"), trf.get(rowf, "reportDate"),
												trf.get(rowf, "filingDate"), accn, trf.get(rowf, "size"), docName);

										ArrayList<String> compFacts = reports.remove(accn);
										if ( null != compFacts ) {
											dc.add("Data doc <ALL>");
											String formType = trf.get(rowf, EdgarSubmissionAtt.form.name());

											String cik = trSubIdx.get(row, EdgarHeadFields.cik.name());
											String pathPrefix = trSubIdx.get(row, EdgarHeadFields.__PathPrefix.name());
											docName = docName.substring(1, docName.length() - 1);

											String dirName = pathPrefix + "/" + formType + "/" + accn;

											DustUtils.sbAppend(sbLog, "\t", true, compFacts.size(), dirName);

											try {
												edgarSource.getFiling(cik, pathPrefix, formType, accn, null);
//												edgarSource.getFiling(cik, pathPrefix, formType, accn, docName);

											} catch (Throwable e) {
												String msg = e.toString();
												msg = DustUtils.getPostfix(msg, ";").trim();
												dc.add("Read error " + msg);
//												DustException.swallow(e);
											}

											String dataFileName = accn + POSTFIX_VAL;
//											File dir = new File(edgarSource.fReportRoot, pathPrefix + "/" + formType + "/" + accn);
//											File fVal = new File(dir, accn + POSTFIX_VAL);
											File fVal = new File(edgarSource.fReportRoot, dirName + "/" + dataFileName);
											if ( fVal.isFile() ) {
												dc.add("Data doc <Available>");
												DustUtils.sbAppend(sbLog, "\t", true, dataFileName);

												dc.add("Fact TO MATCH", compFacts.size());

												int cntLine = 0;
												int cntData = 0;
												int cntMatch = 0;
												int cntMiss = 0;

												try (BufferedReader brv = new BufferedReader(new FileReader(fVal))) {

													DustUtilsData.TableReader trv = null;
													Map<String, Object> val = new TreeMap<>();
													String[] find = new String[] { "Concept", "Taxonomy" };

													for (String linev; (linev = brv.readLine()) != null;) {
														String[] rowv = linev.split("\t");
														if ( null == trv ) {
															trv = new DustUtilsData.TableReader(rowv);
														} else {
															dc.add("Data facts <ALL>");
															++cntLine;

															String matchLine = null;

															val.clear();
															trv.get(rowv, val, "Taxonomy", "Concept", "Unit", "Value", "StartDate", "EndDate", "Instant");

															if ( val.isEmpty() ) {
																dc.add("Data facts INVALID");
																continue;
															}

															++cntData;

															String unit = ((String) val.getOrDefault("Unit", "-")).replace("iso4217:", "").replace("xbrli:", "");

															for (Iterator<String> cfIt = compFacts.iterator(); cfIt.hasNext();) {
																String cfl = cfIt.next();
																rowcf = cfl.split("\t");
																boolean match = true;
																for (String k : find) {
																	if ( !DustUtils.isEqual(val.get(k), trcf.get(rowcf, k)) ) {
																		match = false;
																		break;
																	}
																}
																if ( match ) {
																	match = matchPeriod(val, trcf, rowcf);
																}
																if ( unitAware && match && !"-".equals(unit) && !DustUtils.isEqual(unit, trcf.get(rowcf, "Unit")) ) {
																	match = false;
																	break;
																}

																if ( match ) {
																	String vVal = (String) val.get("Value");
																	String cfVal = trcf.get(rowcf, "Value");

																	String type = trv.get(rowv, "Type");

																	if ( (null != vVal) && vVal.contains(".") && vVal.endsWith("0") ) {
																		vVal = vVal.replaceAll("(0+)$", "");
																		if ( vVal.endsWith(".") ) {
																			vVal = vVal.substring(0, vVal.length() - 1);
																		}
																	}
																	if ( (null != vVal) && cfVal.contains(".") && cfVal.endsWith("0") ) {
																		cfVal = cfVal.replaceAll("(0+)$", "");
																	}

																	if ( DustUtils.isEqual(vVal, cfVal) ) {
																		dc.add("Fact match " + type);
																		++cntMatch;
																		if ( null == matchLine ) {
																			matchLine = cfl;
																		} else {
																			dc.add("Fact MULTIPLE MATCH");
																			DustUtils.breakpoint();
																		}
																		cfIt.remove();
																		break;
//																	} else {
//																		dc.add("Fact DIFF " + accn);
																	}
																}
															}

															if ( null == matchLine ) {
																dc.add("Fact not found in CF");
																++cntMiss;
//																dc.add("Data Fact not found in CF " + trv.get(rowv, "Type"));
															}
														}
													}
												}

												int dmiss = compFacts.size();
												if ( 0 < dmiss ) {
													dc.add("Fact not found in Data", dmiss);
													dc.add("Fact not found in Data " + accn, dmiss);
												}
												DustUtils.sbAppend(sbLog, "\t", true, cntLine, cntData, cntMatch, compFacts.size(), cntMiss);
											}
										}

										ps.println(sbLog);

									}
								}

//								if ( !reports.isEmpty() ) {
//									DustUtils.breakpoint();
//								}
							}
						}
					}
				}
				ps.flush();
			}
		}

//		Dust.dump(" ", true, "Companies:", cntCompanies, "Referred Docs:", cntDocRefs, "All Submission:", cntDocLine, "Primary document known", cntDocOK, "Successfully loaded", cntDocLoaded);
		dc.dump("Car stats");
		edgarSource.dc.dump("Load stats");

		System.out.println(pm);
	}

	private final String[] DATEFLDS = new String[] { "Instant", "StartDate", "EndDate" };
	private final SimpleDateFormat DFISO = new SimpleDateFormat("yyyy-MM-dd");
	private static final long PERIODRANGE = 1000 * 60 * 60 * 24 * 4;
	
	private boolean matchPeriod(Map<String, Object> val, TableReader trcf, String[] rowcf) throws Exception {
		
		String vsd = (String) val.get("StartDate");
		if ( !DustUtils.isEmpty(vsd) && DustUtils.isEqual(vsd, val.get("EndDate")) && !DustUtils.isEmpty(trcf.get(rowcf, "Instant"))) {
			val.remove("StartDate");
			val.remove("EndDate");
			val.put("Instant", vsd);
		}
		
		for ( String k : DATEFLDS ) {
			String d1 = (String) val.get(k);
			String d2 = trcf.get(rowcf, k);
			
			if ( DustUtils.isEmpty(d1) ) {
				if ( !DustUtils.isEmpty(d2) ) {
					return false;
				}
			} else {
				if ( DustUtils.isEmpty(d2) ) {
					return false;
				} else {
					long diff = DFISO.parse(d1).getTime() - DFISO.parse(d2).getTime();
					
					if ( (diff < -PERIODRANGE) || ( PERIODRANGE < diff )) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
}
