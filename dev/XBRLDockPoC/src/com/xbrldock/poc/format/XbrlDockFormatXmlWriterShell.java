package com.xbrldock.poc.format;

import java.io.File;
import java.util.Map;

import com.xbrldock.XbrlDockConsts.GenAgent;
import com.xbrldock.format.XbrlDockFormatAgentXmlWriter;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;

public class XbrlDockFormatXmlWriterShell implements XbrlDockPocConsts.ReportDataHandler {
	GenAgent writer;

	public XbrlDockFormatXmlWriterShell(File targetDir) {
		writer = new XbrlDockFormatAgentXmlWriter();
		XbrlDockUtils.optCallNoEx(writer, XbrlDockFormatAgentXmlWriter.XDC_CMD_GEN_Init, targetDir);
	}

	@Override
	public void beginReport(String repId) {
		XbrlDockUtils.optCallNoEx(writer, XbrlDockFormatAgentXmlWriter.XDC_CMD_GEN_Begin, repId);
	}

	@Override
	public void addNamespace(String ref, String id) {
		XbrlDockUtils.optCallNoEx(writer, XbrlDockFormatAgentXmlWriter.XDC_CMD_REP_ADD_NAMESPACE, ref, id);
	}

	@Override
	public void addTaxonomy(String tx, String type) {
		XbrlDockUtils.optCallNoEx(writer, XbrlDockFormatAgentXmlWriter.XDC_CMD_REP_ADD_SCHEMA, tx, type);
	}

	@Override
	public String processSegment(String segment, Map<String, Object> data) {
		return XbrlDockUtils.optCallNoEx(writer, segment, data);
	}

	@Override
	public void endReport() {
		XbrlDockUtils.optCallNoEx(writer, XbrlDockFormatAgentXmlWriter.XDC_CMD_GEN_End);
	}
}