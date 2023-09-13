package hu.sze.uni.xbrl.portal;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts.MindAccess;
import hu.sze.uni.http.DustHttpServlet;
import hu.sze.uni.xbrl.XbrlConsts.XbrlReportType;
import hu.sze.uni.xbrl.XbrlFilingManager;
import hu.sze.uni.xbrl.portal.XbrlTestPortalConsts.ListColumns;

@SuppressWarnings("rawtypes")
class XbrlTestServletReportBinary extends DustHttpServlet {
	private static final long serialVersionUID = 1L;
	
	private XbrlFilingManager filings;

	public XbrlTestServletReportBinary(XbrlFilingManager filings) {
		this.filings = filings;
	}

	@Override
	protected void processRequest(Map data) throws Exception {
		Dust.dumpObs("get binary content", data);

		String id = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "id");
		String type = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "type");

		Map rep = filings.getReportData().get(id);
//		String lDir = Dust.access(rep, MindAccess.Peek, null, XbrlFilingManager.LOCAL_DIR);
//		String url = Dust.access(rep, MindAccess.Peek, null, type);

		String cType = CONTENT_TEXT;
		XbrlReportType repType = null;
		String fn = (String) rep.get("fxo_id");

		HttpServletResponse resp = Dust.access(data, MindAccess.Peek, null, ServletData.Response);

		switch ( type ) {
		case "package_url":
			fn += ".zip";
			resp.setHeader("Content-Disposition", "attachment; filename=" + fn);
			cType = CONTENT_ZIP + "; filename=" + fn;
			repType = XbrlReportType.Zip;
			break;
		case "json_url":
			cType = CONTENT_JSON;
			repType = XbrlReportType.Json;
			break;
		case "csv":
			String ct = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "ct");

			fn += ("_" + ct + ".csv");
			resp.setHeader("Content-Disposition", "attachment; filename=" + fn);
			cType = CONTENT_CSV + "; filename=" + fn;

			repType = ListColumns.CsvTxt.name().equals(ct) ? XbrlReportType.ContentTxt : XbrlReportType.ContentVal;

//			url = Dust.access(rep, MindAccess.Peek, null, "package_url");
			break;
		}

		if ( null != repType ) {
			resp.setContentType(cType);

			File f = filings.getReport(rep, repType, true);
			OutputStream out = getOutStream(data);
			Files.copy(f.toPath(), out);
			out.flush();
		} else {
			resp.setContentType(cType);
			getWriter(data).println("Here comes the binary content " + data);
		}
	}
}