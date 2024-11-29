package com.xbrldock.poc.conn.concordance;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.XbrlDockPocRefactorUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsMvel;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockConnConcordance implements XbrlDockConnConcordanceConsts, XbrlDockPocRefactorUtils, XbrlDockConsts.GenAgent {

	ArrayList<Map<String, Object>> ruleArr = new ArrayList<>();

	public XbrlDockConnConcordance() {
		// TODO Auto-generated constructor stub
	}

	public void initModule(Map config) throws Exception {
		String ruleFileName = XbrlDockUtils.simpleGet(config, XDC_CONN_CONCORDANCE_TOKEN_loadRules);

		if (!XbrlDockUtils.isEmpty(ruleFileName)) {
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
									line = line.replaceAll("End If", "; ruleExecuted = true; }");

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
			};
			
			val.put("CurrentAssetsExcludingHeldForSale", 10L);
			val.put("AssetsHeldForSale", 10L);

			for (Map<String, Object> r : ruleArr) {
				String expr = (String) r.get(XDC_EXT_TOKEN_value);
				Object mvelObj = XbrlDockUtilsMvel.compile(expr);
				r.put(XDC_UTILS_MVEL_mvelObj, mvelObj);
				
				XbrlDockUtilsMvel.evalCompiled(mvelObj, val);
				
				XbrlDock.log(EventLevel.Info, r.get(XDC_EXT_TOKEN_name), expr);
				if ( Boolean.TRUE.equals(val.get(XDC_CONN_CONCORDANCE_TOKEN_ruleExecuted))) {
					XbrlDock.log(EventLevel.Info, "Executed");
					val.remove(XDC_CONN_CONCORDANCE_TOKEN_ruleExecuted);
				}
			}
		}

//		Collection<String> taxIds = XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_requires);

	}

	@Override
	public Object process(String command, Object... params) throws Exception {
		Object ret = null;

		switch (command) {
		case XDC_CMD_GEN_Init:
			initModule((Map) params[0]);
			break;
		case XDC_CMD_GEN_REFRESH:
			ret = refresh((Collection<String>) params[0]);
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
