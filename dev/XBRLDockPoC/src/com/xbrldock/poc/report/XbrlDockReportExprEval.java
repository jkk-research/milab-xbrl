package com.xbrldock.poc.report;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.format.XbrlDockFormatUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsMvel;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockReportExprEval implements XbrlDockReportConsts, XbrlDockConsts.GenAgent {
	private ExprResultProcessor resultProc;

	private String exprStr;
	private Object exprComp;

	private String repId;
	protected Map<String, Map> segData = new TreeMap<>();
	private Map evalCtx = new TreeMap<>();
	private boolean inited;

	public void setExpression(String expr, ExprResultProcessor resultProc) {
		this.resultProc = resultProc;
		this.exprStr = expr;

		exprComp = XbrlDockUtils.isEmpty(expr) ? null : XbrlDockUtilsMvel.compile(expr);
	}
	
	@Override
	public Object process(String cmd, Map<String, Object> params) throws Exception {
		switch (cmd) {
		case XDC_CMD_GEN_Init:
			break;
		case XDC_CMD_GEN_Begin:
			beginReport((String) params.get(XDC_EXT_TOKEN_id));
			break;
		case XDC_REP_SEG_Unit:
		case XDC_REP_SEG_Context:
		case XDC_REP_SEG_Fact:
			return processSegment(cmd, (Map<String, Object>) params.get(XDC_GEN_TOKEN_source));
		case XDC_CMD_GEN_End:
			endReport();
			break;
		}
		return null;
	}


	public void beginReport(String repId) {
		this.repId = repId;

		for (Map sc : segData.values()) {
			sc.clear();
		}

		inited = false;
	}

	public String processSegment(String segment, Map<String, Object> data) {
		Map segContent = XbrlDockUtils.safeGet(segData, segment, SORTEDMAP_CREATOR);

		String segIdKey = XbrlDockFormatUtils.getSegmentIdKey(segment);
		String segId = (String) data.get(segIdKey);
		if (XbrlDockUtils.isEmpty(segId)) {
			segId = segment + "-" + (segContent.size() + 1);
		}

		Object ret = segId;

		switch (segment) {
		case XDC_REP_SEG_Unit:
		case XDC_REP_SEG_Context:
			segContent.put(segId, new TreeMap<String, Object>(data));
			ret = segId;
			break;
		case XDC_REP_SEG_Fact:
			String ctxId = (String) data.get(XDC_FACT_TOKEN_context);

			try {
				if (!inited) {
					inited = true;
					data.put(XDC_EXPR_result, ret);
					if (!(boolean) resultProc.process(XDC_CMD_GEN_Init, null)) {
						return XDC_RETVAL_STOP;
					}
				}

				if (resultProc.isByContext()) {
					XbrlDockUtils.safeGet(segContent, ctxId, SET_CREATOR).add(new TreeMap<String, Object>(data));
				} else {
					ret = evaluate(data);
				}
			} catch (Throwable e) {
				XbrlDockException.wrap(e, "evaluating expression", repId, exprStr, data);
			}

			break;
		default:
			XbrlDockException.wrap(null, "Invalid segment name", repId, segment, "in expression", exprStr);
			break;
		}

		return XbrlDockUtils.toString(ret);
	}

	public void endReport() {
		try {
			if (inited) {
				if (resultProc.isByContext()) {
					Map<String, Collection<Map>> ctxFacts = XbrlDockUtils.simpleGet(segData, XDC_REP_SEG_Fact);
					for (Map.Entry<String, Collection<Map>> cfe : ctxFacts.entrySet()) {
						String ctxId = cfe.getKey();
						Map ctx = XbrlDockUtils.simpleGet(segData, XDC_REP_SEG_Context, ctxId);
						resultProc.process(XDC_CMD_GEN_Begin, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, ctx));
						for (Map f : cfe.getValue()) {
							evaluate(f);
						}
						resultProc.process(XDC_CMD_GEN_End, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, ctx));
					}
				}
				resultProc.process(XDC_CMD_GEN_Release, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, segData.get(XDC_REP_SEG_Context)));
			}
		} catch (Throwable e) {
			XbrlDockException.wrap(e, "evaluating expression", repId, exprStr);
		}
	}

	protected Object evaluate(Map data) throws Exception {
		evalCtx.clear();
		evalCtx.putAll(data);

		Object ret = null;
		if (null != exprComp) {
			ret = XbrlDockUtilsMvel.evalCompiled(exprComp, evalCtx);
			evalCtx.put(XDC_EXPR_result, ret);
		}

		if (!(boolean) resultProc.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParams(XDC_EXT_TOKEN_value, evalCtx))) {
			ret = XDC_RETVAL_STOP;
		}

		return ret;
	}
}
