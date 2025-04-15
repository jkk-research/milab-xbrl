package com.xbrldock.poc.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.conn.XbrlDockConnUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.stream.XbrlDockStreamCsv;
import com.xbrldock.utils.stream.XbrlDockStreamCsvWriterAgent;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockReportLoader implements XbrlDockReportConsts, XbrlDockConsts.GenAgent {

	final File dataRoot;
	public boolean flat = false;

	String repId;

	Map reportData;

	int factCount;
	String repStart = null;
	String repEnd = null;

	Map<String, String> nsRefs = new TreeMap<>();
	Map<String, Map<String, Object>> unitDef = new TreeMap<>();
	Map<String, Map<String, Object>> ctxDef = new TreeMap<>();

	ArrayList<String> errors = new ArrayList<>();

	XbrlDockStreamCsvWriterAgent cwData = new XbrlDockStreamCsvWriterAgent();
	XbrlDockStreamCsvWriterAgent cwText = new XbrlDockStreamCsvWriterAgent();

	public XbrlDockReportLoader(File dataRoot) {
		this.dataRoot = dataRoot;
	}

	private void setReportData(Map reportData) throws Exception {
		this.reportData = reportData;

//		File dir = flat ? new File(dataRoot, XbrlDockUtils.strTime()) : XbrlDockConnUtils.getFilingDir(dataRoot, reportData, true, true);
		File dir = flat ? dataRoot : XbrlDockConnUtils.getFilingDir(dataRoot, reportData, true, true);
		String prefix = flat ? ((String) reportData.get(XDC_EXT_TOKEN_id) + "_") : "";

		cwData.process(XDC_CMD_GEN_Init, XbrlDockUtils.setParams(XDC_GEN_TOKEN_members, FACT_DATA_FIELDS));
		cwData.process(XDC_CMD_GEN_Begin, XbrlDockUtils.setParams(XDC_GEN_TOKEN_target, new File(dir, prefix + XDC_FNAME_REPDATA)));
		
		cwText.process(XDC_CMD_GEN_Init, XbrlDockUtils.setParams(XDC_GEN_TOKEN_members, FACT_TEXT_FIELDS));
		cwText.process(XDC_CMD_GEN_Begin, XbrlDockUtils.setParams(XDC_GEN_TOKEN_target, new File(dir, prefix + XDC_FNAME_REPTEXT)));
	}

	@Override
	public Object process(String cmd, Map<String, Object> params) throws Exception {
		switch (cmd) {
		case XDC_CMD_GEN_Init:
			setReportData((Map) params.get(XDC_GEN_TOKEN_source));
			break;
		case XDC_CMD_GEN_Begin:
			beginReport((String) params.get(XDC_EXT_TOKEN_id));
			break;
		case XDC_CMD_REP_ADD_NAMESPACE:
			addNamespace((String) params.get(XDC_EXT_TOKEN_id), (String) params.get(XDC_EXT_TOKEN_value));
			break;
		case XDC_CMD_REP_ADD_SCHEMA:
			addTaxonomy((String) params.get(XDC_EXT_TOKEN_id), (String) params.get(XDC_EXT_TOKEN_value));
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

		unitDef.clear();
		ctxDef.clear();
		nsRefs.clear();
		errors.clear();

		factCount = 0;
		repStart = repEnd = null;
	}

	public void addNamespace(String ref, String id) {
		nsRefs.put(ref, id);
		XbrlDockUtils.safeGet(reportData, XDC_REPORT_TOKEN_namespaces, MAP_CREATOR).put(ref, id);
	}

	public void addTaxonomy(String tx, String type) {
		XbrlDockUtils.safeGet(reportData, XDC_REPORT_TOKEN_schemas, ARRAY_CREATOR).add(tx);
	}

	public String processSegment(String segment, Map<String, Object> data) {
		String ret = "";

		switch (segment) {
		case XDC_REP_SEG_Unit:
			ret = storeSeg(XDC_FACT_TOKEN_unit, "unit-", unitDef, data);
			break;
		case XDC_REP_SEG_Context:
			ret = storeSeg(XDC_FACT_TOKEN_context, "ctx-", ctxDef, data);
			break;
		case XDC_REP_SEG_Fact:
			++factCount;

			ret = (String) data.get(XDC_EXT_TOKEN_id);
			if (XbrlDockUtils.isEmpty(ret)) {
				ret = "fact-" + factCount;
			}

			String str = XbrlDockUtils.simpleGet(data, XDC_FACT_TOKEN_concept);
			int sep = str.indexOf(":");
			if (-1 != sep) {
				String ns = str.substring(0, sep);
				if (!nsRefs.containsKey(ns)) {
					errors.add("unresolved ns\t" + ns);
				}
			} else {
				errors.add("no ns\t" + str);
			}

			str = XbrlDockUtils.simpleGet(data, XDC_FACT_TOKEN_context);
			Map<String, Object> ctx = ctxDef.get(str);
			if (null == ctx) {
				errors.add("unresolved ctx\t" + str);
			} else {
				data.putAll(ctx);
			}

			str = XbrlDockUtils.simpleGet(data, XDC_FACT_TOKEN_unit);

			if (!XbrlDockUtils.isEmpty(str)) {
				Map<String, Object> unit = unitDef.getOrDefault(str, Collections.EMPTY_MAP);
				data.putAll(unit);
			}

			try {
				if (XbrlDockUtils.isEqual(XDC_FACT_VALTYPE_text, data.get(XDC_FACT_TOKEN_xbrldockFactType))) {
					cwText.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParams(XDC_EXT_TOKEN_value, data));
				} else {
					cwData.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParams(XDC_EXT_TOKEN_value, data));
				}
			} catch (Throwable e) {
				XbrlDockException.swallow(e, "Storing report data", repId, data);
			}

			break;
		}

		return ret;
	}

	private static String storeSeg(String idId, String prefix, Map<String, Map<String, Object>> defMap, Map<String, Object> source) {
		String segId = (String) source.get(idId);
		if (XbrlDockUtils.isEmpty(segId)) {
			segId = prefix + (defMap.size() + 1);
		}

		TreeMap<String, Object> clone = new TreeMap<String, Object>(source);
		
		Object dim = source.get(XDC_FACT_TOKEN_dimensions);
		
		if ( null != dim ) {
			clone.put(XDC_FACT_TOKEN_dimensions, dim.toString());
		}
		defMap.put(segId, clone);

		return segId;
	}

	public void endReport() {
		try {
			cwData.close();
			cwText.close();
		} catch (Throwable e) {
			XbrlDockException.swallow(e, "End storing report data", repId);
		}
	}

	@Override
	public String toString() {
		return repId + "\n  Namespaces: " + nsRefs + "\n  Contexts:" + ctxDef + "\n  Units:" + unitDef;
	}

	public static void readCsv(File f, Map filingInfo, GenAgent dataHandler) throws Exception {

		int colCount = 0;
		ArrayList<String> colNames = null;
		ArrayList<String> values = new ArrayList<>();

		Set<Object> unit = new HashSet<>();
		Set<Object> ctx = new HashSet<>();

		Map<String, Object> segment = new HashMap<>();
		Map<String, Object> fact = new HashMap<>();

		XbrlDockStreamCsv.CsvLineReader lineReader = new XbrlDockStreamCsv.CsvLineReader("\t", values);

		try (FileReader fr = new FileReader(f); BufferedReader br = new BufferedReader(fr)) {
			for (String line = br.readLine(); null != line; line = br.readLine()) {
				values.clear();

				if (!lineReader.csvReadLine(line)) {
					continue;
				}

				if (null == colNames) {
					colNames = new ArrayList<>(values);
					colCount = colNames.size();
					
					dataHandler.process(XDC_CMD_GEN_Begin, XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, filingInfo.get(XDC_EXT_TOKEN_id)));
					
					Map<String, String> ns = XbrlDockUtils.simpleGet(filingInfo, XDC_REPORT_TOKEN_namespaces);
					if ( null != ns ) {
						for ( Map.Entry<String, String> ne : ns.entrySet() ) {
							dataHandler.process(XDC_CMD_REP_ADD_NAMESPACE, XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, ne.getKey(), XDC_EXT_TOKEN_value , ne.getValue()));
						}
					}
					
					Collection<String> tx = XbrlDockUtils.simpleGet(filingInfo, XDC_REPORT_TOKEN_schemas);
					if ( null != tx ) {
						for (String t : tx ) {
							dataHandler.process(XDC_CMD_REP_ADD_SCHEMA, XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, t));
						}
					}
				} else {
					for (int i = 0; i < colCount; ++i) {
						fact.put(colNames.get(i), values.get(i));
					}

					Object key = fact.get(XDC_FACT_TOKEN_context);
					if (ctx.add(key)) {
						XbrlDockUtils.optCopyFields(fact, segment, CONTEXT_FIELDS);
						dataHandler.process(XDC_REP_SEG_Context, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, segment));

					}

					key = fact.get(XDC_FACT_TOKEN_unit);
					if (!XbrlDockUtils.isEmpty((String) key) && unit.add(key)) {
						XbrlDockUtils.optCopyFields(fact, segment, UNIT_FIELDS);
						dataHandler.process(XDC_REP_SEG_Unit, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, segment));
					}
					
					switch ((String) fact.get(XDC_FACT_TOKEN_xbrldockFactType) ) {
					case XDC_FACT_VALTYPE_number:
						String sv = (String) fact.get(XDC_EXT_TOKEN_value);
						BigDecimal bd = XbrlDockUtils.isEmpty(sv) ? BigDecimal.ZERO : new BigDecimal(sv);
						fact.put(XDC_EXT_TOKEN_value, bd);
						break;
					}

					String ret = (String) dataHandler.process(XDC_REP_SEG_Fact, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, fact));
					
					if ( XDC_RETVAL_STOP.equals(ret)) {
						break;
					}
				}
			}
			
			if ( null != colNames) {
				dataHandler.process(XDC_CMD_GEN_End, null);
			}
		} finally {
		}

	}

}
