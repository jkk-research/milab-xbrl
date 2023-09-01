package hu.sze.milab.xbrl;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
				yr = ((50 < Integer.parseInt(yr)) ? "19" : "20") + yr;
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
	
//	static BigDecimal DEF_UNIT_DIV = new BigDecimal(100);

	public static BigDecimal convertToNumber(String val, String fmt, String scale, String decimals, String sign) {
		char decSep = '.';
		BigDecimal dVal = null;
		boolean unitSep = false;

		if ( fmt.contains("zero") || fmt.contains("numdash") ) {
			dVal = BigDecimal.ZERO;
		} else {
			if ( !DustUtils.isEmpty(val) ) {
				if ( fmt.contains("comma") && !fmt.contains("numcommadot") ) {
					decSep = ',';
				} else if ( fmt.contains("unit") ) {
					unitSep = true;
					decSep = 0;
				}

				StringBuilder sbVal = new StringBuilder();
				StringBuilder sbFrac = new StringBuilder();
				StringBuilder sbApp = sbVal;

				for (int i = 0; i < val.length(); ++i) {
					char c = val.charAt(i);

					if ( Character.isDigit(c) || (('-' == c) && (0 == i)) ) {
						sbApp.append(c);
					} else if ( (unitSep && !Character.isDigit(c) && !Character.isWhitespace(c)) || (c == decSep) ) {
						sbApp = sbFrac;
					}
				}

				BigDecimal dUnit = null;

				if ( 0 < sbFrac.length() ) {
					if ( unitSep ) {
						dUnit = new BigDecimal(sbFrac.toString());
						dUnit = dUnit.movePointLeft(2);
					} else {
						sbVal.append(".").append(sbFrac);
					}
				}

				dVal = new BigDecimal(sbVal.toString());
				if ( null != dUnit ) {
					dVal = dVal.add(dUnit);
				}

				if ( !DustUtils.isEmpty(scale) && !"0".equals(scale) ) {
					dVal = dVal.movePointRight(Integer.valueOf(scale));
				}

				if (!DustUtils.isEmpty(decimals) && !"0".equals(decimals) && !"INF".equals(decimals) ) {
					dVal = dVal.setScale(Integer.valueOf(decimals), RoundingMode.FLOOR);
				}

				if ( "-".equals(sign) ) {
					dVal = dVal.negate();
				}
			}
		}
		
		return dVal;
	}
}
