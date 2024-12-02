package com.xbrldock.poc.conn.concordance;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.XbrlDockPocRefactorUtils;
import com.xbrldock.poc.meta.XbrlDockMetaContainer;
import com.xbrldock.poc.report.XbrlDockReportExprEval;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsMvel;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockConnConcordance implements XbrlDockConnConcordanceConsts, XbrlDockPocRefactorUtils, XbrlDockConsts.GenAgent {

	Pattern ptVerifyRule = Pattern.compile("[A-Z]+\\d+");

	Map<String, String> conceptMapping = new TreeMap<>();
	ArrayList<Map<String, Object>> ruleArr = new ArrayList<>();
	String store;

	ExprResultProcessor conProc = new ExprResultProcessor() {
		Map segment = new TreeMap();
		Map<String, Object> data = new TreeMap();

		@Override
		public Object process(String cmd, Object... params) throws Exception {

			switch (cmd) {
			case XDC_CMD_GEN_Init:
				break;
			case XDC_CMD_GEN_Begin:
				segment.clear();
				segment.putAll((Map) params[0]);
				data.clear();
				break;
			case XDC_CMD_GEN_Process:
				Map fact = (Map) params[0];
				String concept = (String) fact.get(XDC_FACT_TOKEN_concept);

				concept = concept.replaceFirst(":", "_");

				String m = conceptMapping.get(concept);

				if (null != m) {
					data.put(m, new TreeMap(fact));
				}
				break;
			case XDC_CMD_GEN_End:

				if (!data.isEmpty()) {
					String ruleName = null;
					
					Map<String, Object> val = new HashMap<String, Object>() {
						private static final long serialVersionUID = 1L;

						@Override
						public boolean containsKey(Object key) {
							return true;
						}

						@Override
						public Object get(Object key) {
							Object ret = super.get(key);
							return (null == ret) ? getOrDefault("fac_" + key, 0L) : ret;
						}

						@Override
						public Object put(String key, Object value) {
							if (!key.startsWith("fac_")) {
								if (!ptVerifyRule.matcher(key).matches()) {
									key = "fac_" + key;
								}
							}
							
							
							
							return super.put(key, value);
						}
					};

					for (String k : data.keySet()) {
						val.put(k, XbrlDockUtils.simpleGet(data, k, XDC_EXT_TOKEN_value));
					}

					for (Map<String, Object> r : ruleArr) {
						ruleName = (String) r.get(XDC_EXT_TOKEN_name);
						String expr = (String) r.get(XDC_EXT_TOKEN_value);
						Object mvelObj = XbrlDockUtilsMvel.compile(expr);
						r.put(XDC_UTILS_MVEL_mvelObj, mvelObj);

						XbrlDockUtilsMvel.evalCompiled(mvelObj, val);
						
						
					}
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
	}

	public void dumpConcordanceSettings() {
		for (Map<String, Object> r : ruleArr) {
			System.out.println(r.get(XDC_EXT_TOKEN_name) + "\t" + r.get(XDC_EXT_TOKEN_value));
		}

		System.out.println("-------");

		for (Map.Entry<String, String> me : conceptMapping.entrySet()) {
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
						String from = XbrlDockUtils.getPostfix((String) re.get("xlink:from"), "#");
						String to = XbrlDockUtils.getPostfix((String) re.get("xlink:to"), "#");

						conceptMapping.put(to, from);
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
			Map<String, Object> filings = (Map<String, Object>) catalog.get(XDC_CONN_CAT_TOKEN_filings);
			for (String repId : filings.keySet()) {
				XbrlDock.callAgent(store, XDC_CMD_CONN_VISITREPORT, repId, repEval);
			}
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
