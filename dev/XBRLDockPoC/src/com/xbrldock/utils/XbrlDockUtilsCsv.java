package com.xbrldock.utils;

import java.util.Collection;
import java.util.regex.Pattern;

import com.xbrldock.XbrlDockException;

public class XbrlDockUtilsCsv implements XbrlDockUtilsConsts {

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

}
