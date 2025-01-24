package com.xbrldock.poc;

public interface XbrlDockPocRefactorUtils {
//	public abstract class AgentShell {
//		protected final GenAgent agent;
//
//		public AgentShell(GenAgent agent) {
//			this.agent = agent;
//		}
//		public GenAgent getAgent() {
//			return agent;
//		}
//	}
//	
//	public class ReportDataHandlerShell extends AgentShell implements XbrlDockPocConsts.ReportDataHandler {
//
//		public ReportDataHandlerShell(GenAgent agent) {
//			super(agent);
//		}
//		
//		@Override
//		public void beginReport(String repId) {
//			XbrlDockUtils.optCallNoEx(agent, XbrlDockFormatAgentXmlWriter.XDC_CMD_GEN_Begin, repId);
//		}
//
//		@Override
//		public void addNamespace(String ref, String id) {
//			XbrlDockUtils.optCallNoEx(agent, XbrlDockFormatAgentXmlWriter.XDC_CMD_REP_ADD_NAMESPACE, ref, id);
//		}
//
//		@Override
//		public void addTaxonomy(String tx, String type) {
//			XbrlDockUtils.optCallNoEx(agent, XbrlDockFormatAgentXmlWriter.XDC_CMD_REP_ADD_SCHEMA, tx, type);
//		}
//
//		@Override
//		public String processSegment(String segment, Map<String, Object> data) {
//			return XbrlDockUtils.optCallNoEx(agent, segment, data);
//		}
//
//		@Override
//		public void endReport() {
//			XbrlDockUtils.optCallNoEx(agent, XbrlDockFormatAgentXmlWriter.XDC_CMD_GEN_End);
//		}
//	}
//
//	public class ReportFormatHandlerShell extends AgentShell implements XbrlDockPocConsts.ReportFormatHandler {
//
//		public ReportFormatHandlerShell(GenAgent agent) {
//			super(agent);
//		}
//
//		@Override
//		public void loadReport(InputStream in, ReportDataHandler dataHandler) throws Exception {
//			XbrlDockUtils.optCallNoEx(agent, XbrlDockFormatAgentXmlWriter.XDC_CMD_GEN_Process, in, ((AgentShell)dataHandler).getAgent() );
//		}
//
//	}
}
