package com.xbrldock.utils.stream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.xbrldock.XbrlDockException;
import com.xbrldock.utils.XbrlDockUtils;

public class XbrlDockStreamCsv implements XbrlDockStreamConsts {

	public static String csvOptEscape(String valStr, String sepChar) {
		if (null == valStr) {
			return "";
		}

		String ret = valStr.trim();

		if (valStr.startsWith("\"") && valStr.endsWith("\"")) {
			return ret;
		}

		if (valStr.contains(sepChar) || valStr.contains("\"") || valStr.contains("\n")) {
			ret = csvEscape(valStr, true);
		}
		return ret;
	}

	static Pattern PT_ESC = Pattern.compile("\\s+", Pattern.MULTILINE);

	public static String csvEscape(String valStr, boolean addQuotes) {
		String ret = "";

		if (null != valStr) {

			ret = valStr.replace("\"", "\"\"");
			ret = PT_ESC.matcher(ret).replaceAll(" ");
		}

		if (addQuotes) {
			ret = "\"" + ret + "\"";
		}

		return ret;
	}

	public static String csvOptUnEscape(String valStr, boolean removeQuotes) {
		if (XbrlDockUtils.isEmpty(valStr) || !valStr.contains("\"")) {
			return valStr;
		}

		String ret = valStr;
		if (removeQuotes) {
			if (valStr.startsWith("\"")) {
				ret = valStr.substring(1, valStr.length() - 1);
			}
		}

		ret = ret.replace("\"\"", "\"");

		return ret;
	}

	public static class CsvLineReader {
		final char sep;
		final Collection<String> target;

		private StringBuilder sb;
		private boolean inQuote;
		private boolean prevQuote;
		private int pos;

		public CsvLineReader(String sep, Collection<String> target) {
			this.sep = sep.charAt(0);
			this.target = target;
		}

		void throwError(String line, String msg) {
			XbrlDockException.wrap(null, "CSV - " + msg + " in line", line, "at pos", pos);
		}

		public boolean csvReadLine(String line) {
			pos = 0;

			for (char c : line.toCharArray()) {
				++pos;

				switch (c) {
				case 65279:
					// BOM?
					break;
				case '\"':
					if (null == sb) {
						inQuote = true;
						sb = new StringBuilder();
						prevQuote = false;
					} else if (inQuote) {
						if (prevQuote) {
							sb.append(c);
							prevQuote = false;
						} else {
							prevQuote = true;
						}
					} else {
						throwError("Quotation mark in unquoted field!", line);
					}
					break;
				default:
					if (c == sep) {
						if (null != sb) {
							if (inQuote && !prevQuote) {
								sb.append(c);
							} else {
								target.add(sb.toString());
								sb = null;
							}
						} else if (!prevQuote) {
							target.add("");
						}
						prevQuote = false;
					} else {
						prevQuote = false;
						if (null == sb) {
							if (Character.isWhitespace(c)) {
								break;
							}
							sb = new StringBuilder();
							inQuote = false;
						}
						sb.append(c);
					}
					break;
				}
			}

			if (null != sb) {
				if (inQuote && !prevQuote) {
					sb.append("\n");
					return false;
				} else {
					target.add(sb.toString());
					sb = null;
				}
			} else if (0 < pos) {
				target.add("");
			}

			return true;
		}
	}

	public static int readCsv(File f, GenAgent dataHandler) throws Exception {
		return readCsv(f, dataHandler, "\t");
	}

	public static int readCsv(File f, GenAgent dataHandler, String sep) throws Exception {

		int colCount = 0;
		int rowCount = 0;
		ArrayList<String> colNames = null;
		ArrayList<String> values = new ArrayList<>();
		Map<String, Object> data = new HashMap<>();


		XbrlDockStreamCsv.CsvLineReader lineReader = new XbrlDockStreamCsv.CsvLineReader(sep, values);

		try (FileReader fr = new FileReader(f); BufferedReader br = new BufferedReader(fr)) {
			for (String line = br.readLine(); null != line; line = br.readLine()) {
				values.clear();

				if (!lineReader.csvReadLine(line)) {
					continue;
				}

				if (null == colNames) {
					colNames = new ArrayList<>(values);
					colCount = colNames.size();					
				} else {
					for (int i = 0; i < colCount; ++i) {
						data.put(colNames.get(i), values.get(i));
					}

					String ret = (String) dataHandler.process(XDC_CMD_GEN_Process, data);
					
					++rowCount;
					
					if ( XDC_CMD_GEN_STOP.equals(ret)) {
						break;
					}
				}
			}
			
			if ( null != colNames) {
				dataHandler.process(XDC_CMD_GEN_End, null);
			}
		}

		return rowCount;
	}
}
