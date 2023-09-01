package hu.sze.uni.xbrl.portal;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.uni.xbrl.XbrlConsts;
import hu.sze.uni.xbrl.XbrlFilingManager;

@SuppressWarnings({ "rawtypes", "unchecked" })
public interface XbrlTestPortalConsts extends XbrlConsts {
	Pattern PT_DATE_ONLY = Pattern.compile("(\\d+-\\d+-\\d+).*");

	enum ListColumns {
		Country("country"), PeriodEnd("period_end"), Publisher(XbrlFilingManager.ENTITY_NAME), DateAdded("date_added"), Report(XbrlFilingManager.REPORT_ID), CsvVal(XbrlFilingManager.REPORT_ID),
		CsvTxt(XbrlFilingManager.REPORT_ID), Zip("package_url"), Json("json_url"), Orig("report_url"), ChkErr("error_count"), ChkWarn("warning_count"), ChkInc("inconsistency_count");

		public final String colName;

		private ListColumns(String colName) {
			this.colName = colName;
		}

		static Map load(Map from, Map to) {
			if ( null == to ) {
				to = new TreeMap();
			} else {
				to.clear();
			}

			Object repId = from.get(XbrlFilingManager.REPORT_ID);

			for (ListColumns lc : values()) {
				Object val = from.get(lc.colName);

				switch ( lc ) {
				case DateAdded:
					Matcher m = PT_DATE_ONLY.matcher((String) val);
					if ( m.matches() ) {
						val = m.group(1);
					}
					break;
				case CsvVal:
				case CsvTxt:
					val = "<a href=\"/bin?type=csv&ct=" + lc + "&id=" + val + "\">" + lc + "</a>";
					break;
				case Zip:
				case Json:
					val = DustUtils.isEmpty((String) val) ? " - " : "<a href=\"/bin?type=" + lc.colName + "&id=" + repId + "\">" + lc + "</a>";
					break;
				case Orig:
					val = DustUtils.isEmpty((String) val) ? " - " : "<a target=\"_blank\" href=\"https://filings.xbrl.org" + val + "\">" + lc + "</a>";
					break;
				default:
					break;
				}

				if ( null != val ) {
					to.put(lc.name(), val);
				}
			}

			return to;
		}

	}

}
