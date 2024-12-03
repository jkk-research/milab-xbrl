package com.xbrldock.poc.conn.concordance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import com.xbrldock.utils.XbrlDockUtilsMvel;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockConnConcordance implements XbrlDockConnConcordanceConsts, XbrlDockPocRefactorUtils, XbrlDockConsts.GenAgent {

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

				concept = concept.replaceFirst(":", "_");

				Map m = conceptMapping.get(concept);

				if (null != m) {
					String targetConcept = XbrlDockUtils.getPostfix((String) m.get("xlink:from"), "#");
					Map<String, Object> target = ctxFacts.get(targetConcept);
					String strOrder = (String) m.get(XDC_EXT_TOKEN_order);
					BigDecimal order = XbrlDockUtils.isEmpty(strOrder) ? null : new BigDecimal(strOrder);

					if (null == target) {
						ctxFacts.put(targetConcept, target = new TreeMap(fact));
						target.put(XDC_EXT_TOKEN_order, order);
						target.put(XDC_FACT_TOKEN_concept, targetConcept);
					} else {
						BigDecimal oo = (BigDecimal) target.get(XDC_EXT_TOKEN_order);
						
						if ( 0 <= oo.compareTo(order) ) {
							break;
						}
						target.put(XDC_EXT_TOKEN_value, fact.get(XDC_EXT_TOKEN_value));
						
						XbrlDock.log(EventLevel.Trace, target.get(XDC_FACT_TOKEN_context), "Override", targetConcept, XbrlDockUtils.getPostfix((String)target.get(XDC_GEN_TOKEN_comment), " "), "with", concept);
					}

					target.put(XDC_GEN_TOKEN_comment, "Direct mapping from " + concept);
				}
				break;
			case XDC_CMD_GEN_End:

				if (!ctxFacts.isEmpty()) {
					final StringBuilder ruleName = new StringBuilder();

					Map<String, Object> val = new AbstractMap<String, Object>() {
						Map<String, Object> lastGet;

						@Override
						public boolean containsKey(Object key) {
							return true;
						}

						@Override
						public Object get(Object key) {
							Map<String, Object> ret = ctxFacts.get(key);
							if (null == ret) {
								ret = ctxFacts.get("fac_" + key);
							}

							if (null != ret) {
								lastGet = ret;
								return ret.getOrDefault(XDC_EXT_TOKEN_value, 0L);
							}

							return 0L;
						}

						@Override
						public Object put(String key, Object value) {
							if (!key.startsWith("fac_")) {
								if (!ptVerifyRule.matcher(key).matches()) {
									key = "fac_" + key;
								}
							}

							Map<String, Object> target = ctxFacts.get(key);
							if (null == target) {
								target = new TreeMap((null == lastGet) ? ctxInfo : lastGet);
								target.put(XDC_FACT_TOKEN_concept, key);
								ctxFacts.put(key, target);
							}

							target.put(XDC_GEN_TOKEN_comment, "Applied rule: " + ruleName.toString());

							return target.put(XDC_EXT_TOKEN_value, value);
						}

						@Override
						public Set<Entry<String, Object>> entrySet() {
							return XbrlDockException.wrap(null, "Should not call this");
						}
					};

					for (Map<String, Object> r : ruleArr) {
						String rn = (String) r.get(XDC_EXT_TOKEN_name);

						if (!XbrlDockUtils.isEmpty(rn) && rn.contains("VERIFICATION")) {
//							continue;
						}

						ruleName.replace(0, ruleName.length(), rn);
						String expr = (String) r.get(XDC_EXT_TOKEN_value);
						Object mvelObj = XbrlDockUtilsMvel.compile(expr);
						r.put(XDC_UTILS_MVEL_mvelObj, mvelObj);

						XbrlDockUtilsMvel.evalCompiled(mvelObj, val);
					}

//					XbrlDock.log(EventLevel.Info, "Context facts", ctxFacts);

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
			testRules();
		}

		Collection<String> taxIds = XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_requires);
		if (null != taxIds) {
			for (String tid : taxIds) {
				readConceptMapping(tid);
			}
		}

		dumpConcordanceSettings();

		dirStore = XbrlDockUtilsFile.ensureDir((String) XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirStore));
		loader = new XbrlDockReportLoader(dirStore);
	}

	public void dumpConcordanceSettings() {
		for (Map<String, Object> r : ruleArr) {
			System.out.println(r.get(XDC_EXT_TOKEN_name) + "\t" + r.get(XDC_EXT_TOKEN_value));
		}

		System.out.println("-------");

		for (Map.Entry<String, Map> me : conceptMapping.entrySet()) {
			System.out.println(me.getKey() + "\t" + me.getValue());
		}

		System.out.println("-------");
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

	public void readRuleFile(String ruleFileName) throws IOException, FileNotFoundException {
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
						if (null != sbExpr) {
							sbExpr = XbrlDockUtils.sbAppend(sbExpr, " ", false, line);
							if (line.endsWith("End If")) {
								line = sbExpr.toString();

								int csep = line.indexOf(" Then ");

								String cond = line.substring(0, csep);

								cond = cond.replaceAll("If ", "if ((");
								cond = cond.replaceAll("=", "==");
								cond = cond.replaceAll("<>", "!=");
								cond = cond.replaceAll(" and ", ") && (");
								cond = cond.replaceAll(" or ", ") || (");

								line = cond + line.substring(csep);

								line = line.replaceAll(" Then", ")) { ");
								line = line.replaceAll("End If", "; }");

								sbExpr = null;
							} else {
								continue;
							}
						} else if (line.startsWith("If ")) {
							sbExpr = new StringBuilder(line);
							continue;
						}

						Map<String, Object> rule = new TreeMap<>();
						rule.put(XDC_EXT_TOKEN_value, line);

						int sep = lastComment.indexOf(':');
						if (-1 == sep) {
							rule.put(XDC_EXT_TOKEN_name, lastComment);
						} else {
							rule.put(XDC_EXT_TOKEN_name, lastComment.substring(0, sep));
							rule.put(XDC_GEN_TOKEN_description, lastComment.substring(sep + 1));
						}

						ruleArr.add(rule);
					}
				}
			}
		}
	}

	public void testRules() {
		Map<String, Object> chg = new HashMap<String, Object>();

		Map<String, Object> val = new HashMap<String, Object>() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean containsKey(Object key) {
				return true;
			}

			@Override
			public Object get(Object key) {
				// TODO Auto-generated method stub
				return getOrDefault(key, 0L);
			}

			@Override
			public Object put(String key, Object value) {
				chg.put(key, value);
				return super.put(key, value);
			}
		};

		val.put("CurrentAssetsExcludingHeldForSale", 10L);
		val.put("AssetsHeldForSale", 10L);

		for (Map<String, Object> r : ruleArr) {
			chg.clear();

			String expr = (String) r.get(XDC_EXT_TOKEN_value);
			Object mvelObj = XbrlDockUtilsMvel.compile(expr);
			r.put(XDC_UTILS_MVEL_mvelObj, mvelObj);

			XbrlDockUtilsMvel.evalCompiled(mvelObj, val);

			XbrlDock.log(EventLevel.Info, r.get(XDC_EXT_TOKEN_name), expr);
			if (!chg.isEmpty()) {
				XbrlDock.log(EventLevel.Info, "Updated", chg);
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

			Object filter = (null == strFilter) ? null : XbrlDockUtilsMvel.compile(strFilter);
			
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
