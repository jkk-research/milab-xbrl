package hu.sze.milab.xbrl;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.utils.DustUtils;

public class XbrlCoreUtils implements XbrlConsts {
	
	public static Double convertToDouble(String val, String fmt, String scale, String sign) {
		char decSep = '.';
		Double dVal = 0.0;
		boolean unitSep = false;

		if ( fmt.contains("zero") ) {
			dVal = 0.0;
		} else {
			if ( !DustUtils.isEmpty(val) ) {
				if ( fmt.contains("comma") ) {
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
					} else if ( unitSep && Character.isLetter(c)) {
						sbVal.append('.');
						unitSep = false;
					}
				}

				try {
					dVal = Double.valueOf(sbVal.toString());
				} catch (Throwable ttt) {
					Dust.dumpObs("ERROR parsing number from", val, "format", fmt, "converted to", sbVal);
				}

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
