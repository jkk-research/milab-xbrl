package com.xbrldock.poc.conn.concordance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevMonitor;
import com.xbrldock.poc.XbrlDockPocRefactorUtils;
import com.xbrldock.poc.meta.XbrlDockMetaContainer;
import com.xbrldock.poc.report.XbrlDockReportExprEval;
import com.xbrldock.poc.report.XbrlDockReportLoader;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsJson;
import com.xbrldock.utils.XbrlDockUtilsMvel;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockConnConcordance implements XbrlDockConnConcordanceConsts, XbrlDockPocRefactorUtils, XbrlDockConsts.GenAgent {

	private static final String IMPUTE_END_IF = "End If";

	private static final String IMPUTE_IF = "If ";

	private static final String IMPUTE_THEN = " Then ";

	Pattern ptVerifyRule = Pattern.compile("[A-Z]+\\d+");

	Map<String, Map> conceptMapping = new TreeMap<>();
	ArrayList<Map<String, Object>> ruleArr = new ArrayList<>();
	String store;

	File dirStore;
	XbrlDockReportLoader loader;

	ExprResultProcessor conProc = new ExprResultProcessor() {
		Map ctxInfo = new TreeMap();
		Map<String, Map> ctxFacts = new TreeMap();

		@Override
		public Object process(String cmd, Object... params) throws Exception {

			switch (cmd) {
			case XDC_CMD_GEN_Init:
				break;
			case XDC_CMD_GEN_Begin:
				ctxInfo.clear();
				ctxInfo.putAll((Map) params[0]);
				ctxFacts.clear();
				break;
			case XDC_CMD_GEN_Process:
				Map fact = (Map) params[0];
				String concept = (String) fact.get(XDC_FACT_TOKEN_concept);

//				concept = concept.replaceFirst(":", "_");

				Map m = conceptMapping.get(concept);

				if (null != m) {
					String targetConcept = XbrlDockUtils.getPostfix((String) m.get("xlink:from"), "#");
					targetConcept = targetConcept.replaceFirst("_", ":");
					Map<String, Object> target = ctxFacts.get(targetConcept);
					String strOrder = (String) m.get(XDC_EXT_TOKEN_order);
					BigDecimal order = XbrlDockUtils.isEmpty(strOrder) ? null : new BigDecimal(strOrder);

					if (null == target) {
						ctxFacts.put(targetConcept, target = new TreeMap(fact));
						target.put(XDC_EXT_TOKEN_order, order);
						target.put(XDC_FACT_TOKEN_concept, targetConcept);
					} else {
						BigDecimal oo = (BigDecimal) target.get(XDC_EXT_TOKEN_order);

						if (0 >= oo.compareTo(order)) {
							break;
						}
						target.put(XDC_EXT_TOKEN_value, fact.get(XDC_EXT_TOKEN_value));

//						XbrlDock.log(EventLevel.Trace, target.get(XDC_FACT_TOKEN_context), "Override", targetConcept,
//								XbrlDockUtils.getPostfix((String) target.get(XDC_GEN_TOKEN_comment), " "), "with", concept);
					}

					target.put(XDC_GEN_TOKEN_comment, "Direct mapping from " + concept);
				}
				break;
			case XDC_CMD_GEN_End:

				if (!ctxFacts.isEmpty()) {
					final Map<String, Object> lastGet = new HashMap<>();

					Map<String, Object> val = new AbstractMap<String, Object>() {

						@Override
						public boolean containsKey(Object key) {
							return true;
						}

						@Override
						public Object get(Object key) {
							Map<String, Object> ret = ctxFacts.get(key);
							if (null == ret) {
								ret = ctxFacts.get("fac:" + key);
							}

							if (null != ret) {
								lastGet.clear();
								lastGet.putAll(ret);
								return ret.getOrDefault(XDC_EXT_TOKEN_value, 0L);
							}

							return 0L;
						}

						@Override
						public Object put(String key, Object value) {
							return XbrlDockException.wrap(null, "Expression should not have side effect on data here", key, value);
						}

						@Override
						public Set<Entry<String, Object>> entrySet() {
							return XbrlDockException.wrap(null, "Should not call this");
						}
					};

					for (Map<String, Object> r : ruleArr) {
						String key = (String) r.get(XDC_FACT_TOKEN_concept);
						String rn = (String) r.get(XDC_EXT_TOKEN_name);

						Object mo = r.get(XDC_UTILS_MVEL_mvelCompCond);
						if ((null != mo) && !(boolean) XbrlDockUtilsMvel.evalCompiled(mo, val)) {
							continue;
						}

						mo = r.get(XDC_UTILS_MVEL_mvelCompObj);
						lastGet.clear();
						Object value = XbrlDockUtilsMvel.evalCompiled(mo, val);

						Map<String, Object> target = ctxFacts.get(key);
						if (null == target) {
							target = new TreeMap(lastGet.isEmpty() ? ctxInfo : lastGet);
							target.put(XDC_FACT_TOKEN_concept, key);
							ctxFacts.put(key, target);
						}

						target.put(XDC_EXT_TOKEN_value, value);
						target.put(XDC_GEN_TOKEN_comment, "Applied rule: " + rn);

					}

					loader.processSegment(XDC_REP_SEG_Context, ctxInfo);

					for (Map f : ctxFacts.values()) {
						loader.processSegment(XDC_REP_SEG_Fact, f);
					}

				} else {
//					XbrlDock.log(EventLevel.Info, "No mapped fact in ctx", ctxInfo);
				}

				break;
			case XDC_CMD_GEN_Release:
				break;
			default:
				break;
			}
			return true;
		}

		@Override
		public boolean isByContext() {
			return true;
		}
	};

	XbrlDockReportExprEval repEval = new XbrlDockReportExprEval() {

	};

	public XbrlDockConnConcordance() {
		repEval.setExpression(null, conProc);
	}

	public void initModule(Map config) throws Exception {
		store = XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_store);

		String ruleFileName = XbrlDockUtils.simpleGet(config, XDC_CONN_CONCORDANCE_TOKEN_loadRules);

		if (!XbrlDockUtils.isEmpty(ruleFileName)) {
			readRuleFile(ruleFileName);
			compileRules();
		}

		Collection<String> taxIds = XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_requires);
		if (null != taxIds) {
			for (String tid : taxIds) {
				readConceptMapping(tid);
			}
		}

		dirStore = XbrlDockUtilsFile.ensureDir((String) XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirStore));
		loader = new XbrlDockReportLoader(dirStore);

		dumpConcordanceSettings();
	}

	public void dumpConcordanceSettings() throws Exception {
		for (Map<String, Object> r : ruleArr) {
			System.out.println(r.get(XDC_EXT_TOKEN_name) + "\t" + r.get(XDC_EXT_TOKEN_value));
		}

		System.out.println("-------");

		for (Map.Entry<String, Map> me : conceptMapping.entrySet()) {
			Map m = me.getValue();
			String line = XbrlDockUtils
					.sbAppend(null, "\t", true, me.getKey(), XbrlDockUtils.getPostfix((String) m.get("xlink:from"), "#"), m.get(XDC_EXT_TOKEN_order)).toString();
			System.out.println(line);
		}

		System.out.println("-------");

		saveConcordanceSettings();
	}

	public void saveConcordanceSettings() throws Exception {

		Map conCfg = new TreeMap();

		ArrayList expRules = XbrlDockUtils.safeGet(conCfg, XDC_FORMULA_expressions, ARRAY_CREATOR);
		for (Map<String, Object> r : ruleArr) {
			Map rm = new TreeMap(r);
			rm.remove(XDC_UTILS_MVEL_mvelCompObj);
			rm.remove(XDC_UTILS_MVEL_mvelCompCond);
			expRules.add(rm);
		}

		Map mapExp = XbrlDockUtils.safeGet(conCfg, XDC_CONN_CONCORDANCE_CFG_mappings, SORTEDMAP_CREATOR);
		for (Map.Entry<String, Map> me : conceptMapping.entrySet()) {
			Map m = me.getValue();
			String from = XbrlDockUtils.getPostfix((String) m.get("xlink:from"), "#");
			from = from.replaceFirst("_", ":");
			
			String k = me.getKey();
			int s = k.indexOf(":");
			Map mm = XbrlDockUtils.safeGet(mapExp, k.substring(0, s), SORTEDMAP_CREATOR);
			XbrlDockUtils.safeGet(mm, from, ARRAY_CREATOR).add(k);
		}

		XbrlDockUtilsJson.writeJson(new File(dirStore, "concordance.json"), conCfg);

	}

	public void readConceptMapping(String tid) throws Exception {
		XbrlDockMetaContainer tx2nd = XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_GETMC, tid);
		tx2nd.visit(XDC_METATOKEN_links, new GenAgent() {
			@Override
			public Object process(String cmd, Object... params) throws Exception {
				switch (cmd) {
				case XDC_CMD_GEN_Begin:
					break;
				case XDC_CMD_GEN_Process:
					Map<String, Object> re = (Map<String, Object>) params[0];

					if ("class-equivalentClass".equals(re.get("xlink:arcrole"))) {
						String to = XbrlDockUtils.getPostfix((String) re.get("xlink:to"), "#");
						to = to.replaceFirst("_", ":");
						conceptMapping.put(to, re);
					}

					break;
				case XDC_CMD_GEN_End:
					break;
				default:
					break;
				}
				return true;
			}
		});

		XbrlDock.log(EventLevel.Info, "Concept mapping", conceptMapping);

	}

	public void readRuleFile(String ruleFileName) throws Exception {
		try (FileReader fr = new FileReader(ruleFileName); BufferedReader br = new BufferedReader(fr)) {
			String lastComment = null;
			StringBuilder sbExpr = null;

			for (String line = br.readLine(); null != line; line = br.readLine()) {
				line = line.trim();

				if (!XbrlDockUtils.isEmpty(line)) {
					if (line.startsWith("'")) {
						String c = line.substring(1);
						if (!(c.startsWith("'") || c.startsWith("Reviewed"))) {
							lastComment = c;
						}
					} else {
						String cond = null;

						if (null != sbExpr) {
							sbExpr = XbrlDockUtils.sbAppend(sbExpr, " ", false, line);

							if (line.endsWith(IMPUTE_END_IF)) {
								line = sbExpr.toString();

								int csep = line.indexOf(IMPUTE_THEN);

								cond = line.substring(0, csep);

								cond = cond.replaceAll(IMPUTE_IF, "(");
								cond = cond.replaceAll("=", "==");
								cond = cond.replaceAll("<>", "!=");
								cond = cond.replaceAll(" and ", ") && (");
								cond = cond.replaceAll(" or ", ") || (");
								cond = cond + ")";

								line = line.substring(csep + IMPUTE_THEN.length());

								line = line.replaceAll(IMPUTE_END_IF, "");

								sbExpr = null;
							} else {
								continue;
							}
						} else if (line.startsWith(IMPUTE_IF)) {
							sbExpr = new StringBuilder(line);
							continue;
						}

						Map<String, Object> rule = new TreeMap<>();

						int sep = lastComment.indexOf(':');
						if (-1 == sep) {
							rule.put(XDC_EXT_TOKEN_name, lastComment);
						} else {
							rule.put(XDC_EXT_TOKEN_name, lastComment.substring(0, sep));
							rule.put(XDC_GEN_TOKEN_description, lastComment.substring(sep + 1));
						}

						sep = line.indexOf("=");

						String target = line.substring(0, sep).trim();
						rule.put(XDC_FACT_TOKEN_concept, "fac:" + target);

						line = line.substring(sep + 1).trim();
						rule.put(XDC_UTILS_MVEL_mvelText, line);

						if (null != cond) {
							rule.put(XDC_UTILS_MVEL_mvelCondition, cond);
							cond = null;
						}

						ruleArr.add(rule);
					}
				}
			}
		}
	}

	public void compileRules() {
		String str;

		for (Map<String, Object> r : ruleArr) {
			str = (String) r.get(XDC_UTILS_MVEL_mvelText);
			r.put(XDC_UTILS_MVEL_mvelCompObj, XbrlDockUtilsMvel.compile(str));

			str = (String) r.get(XDC_UTILS_MVEL_mvelCondition);
			if (null != str) {
				r.put(XDC_UTILS_MVEL_mvelCompCond, XbrlDockUtilsMvel.compile(str));
			}
		}

	}

	@Override
	public Object process(String command, Object... params) throws Exception {
		Object ret = null;

		switch (command) {
		case XDC_CMD_GEN_Init:
			initModule((Map) params[0]);
			break;
		case XDC_CMD_GEN_REFRESH:
			Map<String, Object> catalog = XbrlDock.callAgent(store, XDC_CMD_GEN_GETCATALOG);
			Map<String, Map> filings = (Map<String, Map>) catalog.get(XDC_CONN_CAT_TOKEN_filings);

			String strFilter = ((params.length > 0) && (params[0] instanceof String)) ? (String) params[0] : null;

			Object filter = XbrlDockUtils.isEmpty(strFilter) ? null : XbrlDockUtilsMvel.compile(strFilter);

			loader.flat = (null != filter);

			XbrlDockDevMonitor mon = new XbrlDockDevMonitor("Concordance", 100);
			for (Map.Entry<String, Map> ef : filings.entrySet()) {
				Map repInfo = ef.getValue();
				if ((null == filter) || (Boolean) XbrlDockUtilsMvel.evalCompiled(filter, repInfo)) {
					mon.step();
					String repId = ef.getKey();
					XbrlDock.log(EventLevel.Context, repId);
					loader.setReportData(repInfo);
					XbrlDock.callAgent(store, XDC_CMD_CONN_VISITREPORT, repId, repEval);
					loader.endReport();
				}
//				break;
			}
			XbrlDock.log(EventLevel.Info, mon);

			break;

		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}

		return ret;
	}

//	@Override
	public int refresh(Collection<String> updated) throws Exception {
		int newCount = 0;

		if (null != updated) {
			updated.clear();
		}

		return (null == updated) ? newCount : updated.size();
	}
}
