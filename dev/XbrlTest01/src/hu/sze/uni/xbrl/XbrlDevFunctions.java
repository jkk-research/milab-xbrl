package hu.sze.uni.xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.xbrl.XbrlCoreUtils;

public class XbrlDevFunctions implements XbrlConsts {

	public static void testDateConv() throws Exception {
		File f = new File("work/AllDate_Filtered.csv");
		PrintWriter result = new PrintWriter("work/DatePostProc.csv");

		SimpleDateFormat fmtOut = new SimpleDateFormat("yyyy-MM-dd");

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			DustUtils.TableReader tr = null;

			for (String line; (line = br.readLine()) != null;) {
				String[] data = line.split("\t");

				if ( null == tr ) {
					tr = new DustUtils.TableReader(data);
				} else {
					String fmt = tr.get(data, "Format");
					tr.set(data, "Err", "");

					if ( fmt.contains("date") ) {
						fmt = DustUtils.getPostfix(fmt, ":");
						String val = tr.get(data, "OrigValue");
						if ( !DustUtils.isEmpty(val) ) {
							String v = val.substring(1, val.length() - 1);

							try {
								Date d = XbrlCoreUtils.convertToDate(v, fmt);

								if ( null == d ) {
									if ( !DustUtils.isEmpty(v) ) {
										DustException.wrap(null, "Format mismatch", fmt, v);
									}
								} else {
									v = fmtOut.format(d);
									tr.set(data, "Value", "\"" + v + "\"");
								}
							} catch (Exception e) {
								tr.set(data, "Err", e.toString());
							}
						}
					}

					line = DustUtils.sbAppend(null, "\t", true, (Object[]) data).toString();
				}

				result.println(line);
			}
		}

		result.flush();
		result.close();
	}

	public static void main(String[] args) throws Exception {
		testDateConv();
	}
}
