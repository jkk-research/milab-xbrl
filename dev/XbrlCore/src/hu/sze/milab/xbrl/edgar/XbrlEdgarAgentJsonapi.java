package hu.sze.milab.xbrl.edgar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mvel2.MVEL;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts;
import hu.sze.milab.dust.net.DustNetConsts;
import hu.sze.milab.dust.stream.json.DustStreamJsonConsts;
import hu.sze.milab.dust.utils.DustUtilsData;

public class XbrlEdgarAgentJsonapi implements DustStreamJsonConsts, DustNetConsts, DustConsts.MindAgent {

	@Override
	public MindStatus agentExecAction(MindAction action) throws Exception {
		switch ( action ) {
		case Begin:
			break;
		case End:
			break;
		case Init:
			break;
		case Process:
			String entityFilter = Dust.access(MindContext.LocalCtx, MindAccess.Peek, null, JsonApiMember.jsonapi, JsonApiParam.filter, "xbrl:entity");
			long count = 999L;

			if ( null != entityFilter ) {

				Pattern pt = Pattern.compile("(?<cmd>\\w*)(\\((?<par>.*)\\))");

				Matcher m = pt.matcher(entityFilter);
				if ( m.matches() ) {
					String cmd = m.group("cmd");
					String[] par = m.group("par").split(",");
					String expr = null;

					switch ( cmd ) {
					case "equals":
						expr = par[1].replace("'", "\"") + ".equals(" + par[0] + ")";
						break;
					}

					if ( null != expr ) {
						Object x = MVEL.compileExpression(expr);

						File fIdx = new File(System.getProperty("user.home") + "/work/xbrl/data/sources/edgar/SubmissionIndex.csv");
						if ( fIdx.isFile() ) {
							DustUtilsData.TableReader trSubIdx = null;
							Map<String, Object> target = new TreeMap<>();
							count = 0;
							try (BufferedReader br = new BufferedReader(new FileReader(fIdx))) {
								for (String line; (line = br.readLine()) != null;) {
									String[] row = line.split("\t");
									if ( null == trSubIdx ) {
										trSubIdx = new DustUtilsData.TableReader(row);
									} else {
										trSubIdx.get(row, target);
										
										if ( (boolean) MVEL.executeExpression(x, target) ) {
											++count;
										}
									}
								}
							}
						} else {
							Dust.dumpObs("File not found", fIdx.getCanonicalPath());
						}
					}
				}

			}
			Dust.access(MindContext.LocalCtx, MindAccess.Set, count, JsonApiMember.jsonapi, MISC_ATT_COUNT);
			break;
		case Release:
			break;
		}

		return MindStatus.Accept;
	}

}
