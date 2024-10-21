package com.xbrldock.poc.report;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsMvel;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockReportExprEval implements XbrlDockReportConsts, XbrlDockPocConsts.ReportDataHandler {
	private boolean byContext;
	private GenProcessor<Map> resultProc;

	private String exprStr;
	private Object exprComp;

	private String repId;
	private Map<String, Map> segData = new TreeMap<>();
	private Map evalCtx = new TreeMap<>();
	private boolean inited;

	public void setExpression(String expr, boolean byContext, GenProcessor<Map> resultProc) {
		this.byContext = byContext;
		this.exprStr = expr;
		exprComp = XbrlDockUtilsMvel.compile(expr);
	}

	@Override
	public void beginReport(String repId) {
		this.repId = repId;

		for (Map sc : segData.values()) {
			sc.clear();
		}

		inited = false;
	}

	@Override
	public void addNamespace(String ref, String id) {
	}

	@Override
	public void addTaxonomy(String tx) {
	}

	@Override
	public String processSegment(String segment, Map<String, Object> data) {
		Map segContent = XbrlDockUtils.safeGet(segData, segment, SORTEDMAP_CREATOR);

		String segIdKey = XbrlDockReportUtils.getSegmentIdKey(segment);
		String segId = (String) data.get(segIdKey);
		if ( XbrlDockUtils.isEmpty(segId) ) {
			segId = segment + "-" + (segContent.size() + 1);
		}

		Object ret = segId;

		switch ( segment ) {
		case XDC_REP_SEG_Unit:
		case XDC_REP_SEG_Context:
			segContent.put(segId, new TreeMap<String, Object>(data));
			ret = segId;
			break;
		case XDC_REP_SEG_Fact:
			String ctxId = (String) data.get(XDC_FACT_TOKEN_context);

			try {
				if ( !inited ) {
					inited = true;
					if ( null != resultProc ) {
						data.put(XDC_EXPR_result, ret);
						if ( !resultProc.process(ProcessorAction.Init, segData) ) {
							return XDC_RETVAL_STOP;
						}
					}
				}

				if ( byContext ) {
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

	@Override
	public void endReport() {
		try {
			if ( inited ) {
				if ( byContext ) {
					Map<String, Collection<Map>> ctxFacts = XbrlDockUtils.simpleGet(segData, XDC_REP_SEG_Fact);
					for (Map.Entry<String, Collection<Map>> cfe : ctxFacts.entrySet()) {
						String ctxId = cfe.getKey();
						Map ctx = XbrlDockUtils.simpleGet(segData, XDC_REP_SEG_Context, ctxId);
						resultProc.process(ProcessorAction.Begin, ctx);
						for (Map f : cfe.getValue()) {
							evaluate(f);
						}
						resultProc.process(ProcessorAction.End, ctx);
					}
				}
				resultProc.process(ProcessorAction.Release, segData.get(XDC_REP_SEG_Context));
			}
		} catch (Throwable e) {
			XbrlDockException.wrap(e, "evaluating expression", repId, exprStr);
		}
	}

	private Object evaluate(Map data) throws Exception {
		Object ret = XbrlDockUtilsMvel.evalCompiled(exprComp, evalCtx);
		
		evalCtx.clear();
		evalCtx.putAll(data);
		
		if ( null != resultProc ) {
			evalCtx.put(XDC_EXPR_result, ret);
			if ( !resultProc.process(ProcessorAction.Process, evalCtx) ) {
				ret = XDC_RETVAL_STOP;
			}
		}
		
		return ret;
	}
}
