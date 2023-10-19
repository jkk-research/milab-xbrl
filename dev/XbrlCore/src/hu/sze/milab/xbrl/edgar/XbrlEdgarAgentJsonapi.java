package hu.sze.milab.xbrl.edgar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.mvel2.MVEL;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts;
import hu.sze.milab.dust.net.DustNetConsts;
import hu.sze.milab.dust.stream.DustStreamUtils;
import hu.sze.milab.dust.stream.json.DustStreamJsonConsts;
import hu.sze.milab.dust.stream.json.DustStreamJsonapiUtils;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsData;

public class XbrlEdgarAgentJsonapi implements XbrlEdgarConsts, DustStreamJsonConsts, DustNetConsts, DustConsts.MindAgent {

	@Override
	public MindStatus agentExecAction(MindAction action) throws Exception {
		switch ( action ) {
		case Begin:
			break;
		case End:
			break;
		case Init:
			break;
		case Process:
			String entityFilter = Dust.access(MindContext.LocalCtx, MindAccess.Peek, null, JsonApiMember.jsonapi, JsonApiParam.filter, TYPE_XBRL_ENTITIES);
			long count = 999L;

			if ( null != entityFilter ) {
				Object fxEntities = MVEL.compileExpression(entityFilter);

				File edgarRoot = new File(System.getProperty("user.home") + "/work/xbrl/data/sources/edgar");

				File fIdx = new File(edgarRoot, "SubmissionIndex.csv");
				if ( fIdx.isFile() ) {
					DustStreamUtils.PrintWriterProvider pwp = Dust.access(MindContext.LocalCtx, MindAccess.Peek, null, JsonApiMember.jsonapi, STREAM_ATT_STREAM_PROVIDER);
					DustStreamJsonapiUtils.StreamWriter sw = new DustStreamJsonapiUtils.StreamWriter(pwp, TYPE_XBRL_ENTITIES, TYPE_XBRL_REPORTS, TYPE_XBRL_CONTEXTS, TYPE_XBRL_FACTS);

					String factFilter = Dust.access(MindContext.LocalCtx, MindAccess.Peek, null, JsonApiMember.jsonapi, JsonApiParam.filter, TYPE_XBRL_FACTS);
					Object fxFacts = DustUtils.isEmpty(factFilter) ? null : MVEL.compileExpression(factFilter);

					try (BufferedReader brEntity = new BufferedReader(new FileReader(fIdx))) {

						DustUtilsData.TableReader trEntity = null;
						Map<String, Object> mEntity = new TreeMap<>();
						count = 0;

						DustStreamJsonapiUtils.Filter jf = new DustStreamJsonapiUtils.Filter();

						for (String lEntity; (lEntity = brEntity.readLine()) != null;) {
							String[] rEntity = lEntity.split("\t");
							if ( null == trEntity ) {
								trEntity = new DustUtilsData.TableReader(rEntity);
							} else {
								trEntity.get(rEntity, mEntity);

								jf.load(mEntity);
								if ( (boolean) MVEL.executeExpression(fxEntities, (Object) jf) ) {

									String cik = (String) mEntity.get("cik");
									String entityID = "edgar:" + cik;

									Set<String> refAccn = new HashSet<>();
									DustUtilsData.Indexer<String> ctxIdx = new DustUtilsData.Indexer<>();

									String fPref = trEntity.get(rEntity, "__PathPrefix");
									File fFacts = new File(edgarRoot, "companyfacts/" + fPref + EXT_CSV);
									if ( fFacts.isFile() ) {
										try (BufferedReader brFacts = new BufferedReader(new FileReader(fFacts))) {
											DustUtilsData.TableReader trFact = null;
//											Map<String, Object> mFact = new TreeMap<>();

											int factCount = 0;

											for (String lFact; (lFact = brFacts.readLine()) != null;) {
												String[] rFact = lFact.split("\t");
												if ( null == trFact ) {
													trFact = new DustUtilsData.TableReader(rFact);
												} else {
													++factCount;

													String ctxKey = trFact.format(rFact, ":", "instant", "start", "end");
													int s = ctxIdx.getSize();
													int ci = ctxIdx.getIndex(ctxKey);

													String ctxId = "edgar:cfCtx_" + cik + "_" + ci;

													if ( null != fxFacts ) {
														jf.clear();
														trFact.get(rFact, jf);
														if ( !((boolean) MVEL.executeExpression(fxFacts, (Object) jf)) ) {
															continue;
														}
													}

													if ( s == ci ) {
														sw.write(JsonApiMember.included, TYPE_XBRL_CONTEXTS, ctxId, trFact, rFact);
													}

													String accn = trFact.get(rFact, "accn");
													refAccn.add(accn);

													sw.addRelationship("report", TYPE_XBRL_REPORTS, entityID + "_" + accn);
													sw.addRelationship("context", TYPE_XBRL_CONTEXTS, ctxId);

													sw.write(JsonApiMember.data, TYPE_XBRL_FACTS, "edgar:cfFact_" + cik + "_" + factCount, trFact, rFact);

													++count;
												}
											}
										}
									}

									if ( 0 < refAccn.size() ) {

										sw.write(JsonApiMember.included, TYPE_XBRL_ENTITIES, entityID, trEntity, rEntity);

										File fAccn = new File(edgarRoot, "submissions/" + fPref + EXT_CSV);
										if ( fAccn.isFile() ) {
											try (BufferedReader brAccn = new BufferedReader(new FileReader(fAccn))) {
												DustUtilsData.TableReader trAccn = null;

												for (String lAccn; (lAccn = brAccn.readLine()) != null;) {
													String[] rAccn = lAccn.split("\t");
													if ( null == trAccn ) {
														trAccn = new DustUtilsData.TableReader(rAccn);
													} else {
														String accn = (String) trAccn.get(rAccn, "accessionNumber");

														if ( refAccn.contains(accn) ) {
															sw.addRelationship("entity", TYPE_XBRL_ENTITIES, entityID);

															sw.write(JsonApiMember.included, TYPE_XBRL_REPORTS, entityID + "_" + accn, trAccn, rAccn);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					} finally {
						sw.close();
					}
				} else {
					Dust.dumpObs("File not found", fIdx.getCanonicalPath());
				}
			}
			Dust.access(MindContext.LocalCtx, MindAccess.Set, count, JsonApiMember.jsonapi, MISC_ATT_COUNT);
			break;
		case Release:
			break;
		}

		return MindStatus.Accept;
	}

}
