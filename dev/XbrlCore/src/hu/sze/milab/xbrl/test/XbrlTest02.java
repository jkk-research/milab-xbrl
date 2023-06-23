package hu.sze.milab.xbrl.test;

import java.io.File;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.brain.DustImpl;
import hu.sze.milab.dust.stream.DustStreamConsts;
import hu.sze.milab.dust.stream.DustStreamXmlAgent;

public class XbrlTest02 implements DustStreamConsts {

	public static void main(String[] args) throws Exception {
		DustImpl.main(args);

		String data = System.getProperty("user.home") + "/work/xbrl/data";
		File dataDir = new File(data);

		File out = new File("out");
		out.mkdirs();

		DustStreamXmlAgent aXml = new DustStreamXmlAgent();

		aXml.agentExecAction(MindAction.Init);

		MindHandle target = Dust.createHandle();
		Dust.access(MIND_ATT_AGENT_SELF, MindAccess.Set, target, MISC_ATT_CONN_TARGET);

//		DustDevAgentDump dump = new DustDevAgentDump();
//		dump.prefix = "Commit dump";

//		DustDevAgentXmlWriter dump = new DustDevAgentXmlWriter();
//		dump.hTarget = target;
//		dump.ps = new PrintStream(new File(out, "xmlTest.txt"));

		XbrlAgentReportToExcel dump = new XbrlAgentReportToExcel();

		MindHandle listener = Dust.createHandle();
		Dust.access(listener, MindAccess.Set, dump, BRAIN_ATT_ACTOR_INSTANCE);
		Dust.access(target, MindAccess.Set, listener, MIND_ATT_KNOWLEDGE_LISTENERS);

		for (String fn : args) {
			long t = System.currentTimeMillis();
			File fIn = new File(dataDir, fn);

			String outName = fIn.getName();
			int idx = outName.lastIndexOf(".");
			outName = "out/" + outName.substring(0, idx + 1) + "xlsx";

			dump.init(target, outName);

			Dust.access(MIND_ATT_AGENT_SELF, MindAccess.Set, fIn, STREAM_ATT_STREAM_FILE);
			aXml.agentExecAction(MindAction.Process);

			File f = dump.save();

			if ( null == f ) {
				Dust.dump(" ", false, "No XBRL:", fn, "time:", System.currentTimeMillis() - t, "msec.");
			} else {
				Dust.dump(" ", false, "Result:", f.getName(), "input size:", fIn.length(), "bytes, time:", System.currentTimeMillis() - t, "msec.");
			}
		}

		aXml.agentExecAction(MindAction.Release);

//		dump.ps.flush();
//		dump.ps.close();

	}

	/*
	 * 
	 * if(header.length() > 50){ //Length of String for my test
	 * sheet.setColumnWidth(0, 18000); //Set column width, you'll probably want to
	 * tweak the second int CellStyle style = wb.createCellStyle(); //Create new
	 * style style.setWrapText(true); //Set wordwrap cell.setCellStyle(style);
	 * //Apply style to cell cell.setCellValue(header); //Write header }
	 * 
	 * 
	 * 
	 * public void setCellValue(double value)
	 * 
	 * dCell.setCellValue(123);
	 */
}
