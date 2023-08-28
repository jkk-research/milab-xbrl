package hu.sze.milab.xbrl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hu.sze.milab.dust.utils.DustUtils;

public class XbrlCoreUtils implements XbrlConsts {

	public static Date convertToDate(String val, String fmtCode) throws Exception {
		Date ret = null;

		String expr = fmtCode.replace("date-", "");

		boolean named = expr.contains("monthname");
		SimpleDateFormat fmt;

		if ( named ) {
			String locale = DustUtils.getPostfix(expr, "-");
			fmt = new SimpleDateFormat("yyyy-MMMM-dd", Locale.forLanguageTag(locale));
			expr = DustUtils.cutPostfix(expr, "-");
		} else {
			fmt = new SimpleDateFormat("yyyy-MM-dd");			
		}

		expr = "(?<" + expr.replace("-", ">\\d+)\\W+(?<") + ">\\d+)";

		if ( named ) {
			expr = expr.replace("monthname>\\d", "month>\\w");
		}
		
		Pattern pt = Pattern.compile(expr);

		Matcher m = pt.matcher(val);

		if ( m.matches() ) {
			String yr = m.group("year");
			if ( 2 == yr.length() ) {
				yr = (( 50 < Integer.parseInt(yr) ) ? "19" : "20" ) + yr;
			}
			String normVal = DustUtils.sbAppend(null, "-", false, yr, m.group("month"), m.group("day")).toString();
			ret = fmt.parse(normVal);
		}

		return ret;
	}

	public static Double convertToDouble(String val, String fmt, String scale, String sign) {
		char decSep = '.';
		Double dVal = 0.0;
		boolean unitSep = false;

		if ( fmt.contains("zero") || fmt.contains("numdash") ) {
			dVal = 0.0;
		} else {
			if ( !DustUtils.isEmpty(val) ) {
				if ( fmt.contains("comma") && !fmt.contains("numcommadot") ) {
					decSep = ',';
				} else if ( fmt.contains("unit") ) {
					unitSep = true;
					decSep = 0;
				}

				StringBuilder sbVal = new StringBuilder();

				for (int i = 0; i < val.length(); ++i) {
					char c = val.charAt(i);

					if ( Character.isDigit(c) || (('-' == c) && (0 == i)) || (c == decSep) ) {
						sbVal.append((c == decSep) ? '.' : c);
					} else if ( unitSep && Character.isLetter(c) ) {
						sbVal.append('.');
						unitSep = false;
					}
				}

				dVal = Double.valueOf(sbVal.toString());

				if ( !DustUtils.isEmpty(scale) ) {
					dVal *= Math.pow(10, Double.valueOf(scale));
				}

				if ( "-".equals(sign) ) {
					dVal = -dVal;
				}
			}
		}
		return dVal;
	}
}
