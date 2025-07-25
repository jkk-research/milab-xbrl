package com.xbrldock.poc.conn.manual;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevMonitor;
import com.xbrldock.poc.XbrlDockPocRefactorUtils;
import com.xbrldock.poc.format.XbrlDockFormatAgentXhtmlReader;
import com.xbrldock.poc.report.XbrlDockReportLoader;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.stream.XbrlDockStreamJson;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockConnManual implements XbrlDockConnManualConsts, XbrlDockPocRefactorUtils, XbrlDockConsts.GenAgent {

	File dirStore;
	File dirInput;
	
	FileFilter xhtmlFilter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			String name = pathname.getName();
			return name.endsWith(".xhtml");
		}
	};

	Map catalog = new TreeMap<String, Object>();

	public XbrlDockConnManual() {
		// TODO Auto-generated constructor stub
	}

	public void initModule(Map config) throws Exception {

		dirInput = new File((String) XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirInput));
		XbrlDockUtilsFile.ensureDir(dirInput);

		dirStore = new File((String) XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirStore));
		XbrlDockUtilsFile.ensureDir(dirStore);

		File fc = new File(dirStore, XDC_FNAME_CONNCATALOG);
		if (fc.isFile()) {
			catalog = XbrlDockStreamJson.readJson(fc);
		}

	}

	@Override
	public Object process(String command, Map<String, Object> params) throws Exception {
		Object ret = null;
//		Map<String, Map> filings = XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings);

		switch (command) {
		case XDC_CMD_GEN_Init:
			initModule(params);
			break;
		case XDC_CMD_GEN_GETCATALOG:
			ret = catalog;
			break;
		case XDC_CMD_GEN_TEST01:
			
			XbrlDockDevMonitor mon = new XbrlDockDevMonitor("Report count", 100);
			int dl = dirInput.getAbsolutePath().length();

			GenAgent xhtmlReader = new GenAgent() {
				@Override
				public Object process(String cmd, Map<String, Object> params) throws Exception {
					switch ( cmd  ) {
					case XDC_CMD_GEN_Process:
						File f = (File) params.get(XDC_GEN_TOKEN_target);
						XbrlDock.log(EventLevel.Trace, "Would process", f.getName());

						String id = f.getAbsolutePath().substring(dl+1).replaceAll("\\s+", "_");
						
						mon.step();

						try (FileInputStream fr = new FileInputStream(f)) {
							XbrlDockReportLoader dh = new XbrlDockReportLoader(dirStore);
							dh.flat = true;
							GenAgent fh = new XbrlDockFormatAgentXhtmlReader();
							
							Map reportData = new TreeMap<String, Object>();

							reportData.put(XDC_EXT_TOKEN_id, id);
							dh.setReportData(reportData);

							dh.process(XDC_CMD_GEN_Begin, XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, id));
							fh.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, fr, XDC_GEN_TOKEN_target, dh));
							dh.process(XDC_CMD_GEN_End, null);
						} catch ( Throwable e ) {
							XbrlDockException.swallow(e, id);
						}

						break;
					}
					return true;
				}
			};
			

			XbrlDockUtilsFile.processFiles(dirInput, xhtmlReader, xhtmlFilter);

			break;

		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}

		return ret;
	}

}
