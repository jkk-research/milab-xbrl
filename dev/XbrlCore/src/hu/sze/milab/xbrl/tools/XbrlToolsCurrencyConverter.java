package hu.sze.milab.xbrl.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hu.sze.milab.dust.utils.DustUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlToolsCurrencyConverter {
	public final Pattern ptUnitCurrency = Pattern.compile("ISO4217:(\\w+).*");

	String mainCurrency;
	Map<String, Map<String, Double>> rates = null;

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	public XbrlToolsCurrencyConverter(String mainCurrency, String rateFilePath, String sep) throws Exception {
		this.mainCurrency = mainCurrency;
		String[] header = null;

		try (BufferedReader br = new BufferedReader(new FileReader(rateFilePath))) {
			for (String line; (line = br.readLine()) != null;) {
				String[] row = line.split(sep);

				if ( null == rates ) {
					rates = new TreeMap<>();
					for (int i = 1; i < row.length; ++i) {
						rates.put(row[i], new TreeMap<>());
					}
					rates.put(mainCurrency, new TreeMap<>());
					header = row;
				} else {
					String date = row[0].replace(".", "-");

					for (int i = 1; i < row.length; ++i) {
						String val = row[i];
						if ( !DustUtils.isEmpty(val) && !"N/A".equals(val) ) {
							Double bd = new Double(val);
							rates.get(header[i]).put(date, bd);
						}
					}
				}
			}
		}
	}

	public String optGetUnitCurrency(Map fact) {
		String ret = null;

		String unit = (String) fact.get("Unit");

		unit = unit.toUpperCase().trim();
		Matcher m = ptUnitCurrency.matcher(unit);

		if ( m.matches() ) {
			ret = m.group(1);
		}

		return ret;
	}

	public Double optConvert(Map fact) throws Exception {
		String vv = (String) fact.get("Value");
		if ( DustUtils.isEmpty(vv) ) {
			return null;
		}

		String ccy = optGetUnitCurrency(fact);

		if ( DustUtils.isEmpty(ccy) ) {
			return null;
		}

		Double val = null;
		Double div = null;
		String status = null;
		int count = 0;

		Map<String, Double> dateRate = rates.get(ccy);
		if ( null == dateRate ) {
			status = "Unknown currency";
		} else {
			val = new Double(vv);

			if ( mainCurrency.equals(ccy) ) {
				status = "No conversion";
				div = 1.0;
			} else {
				String tInstant = (String) fact.get("Instant");
				String tStart = (String) fact.get("StartDate");
				String tEnd = (String) fact.get("EndDate");

				if ( !DustUtils.isEmpty(tStart) && DustUtils.isEqual(tStart, tEnd) ) {
					tInstant = tStart;
				}

				if ( !DustUtils.isEmpty(tInstant) ) {
					div = dateRate.get(tInstant);
					if ( null != div ) {
						count = 1;
						val = val / div;
						status = DustUtils.isEmpty(tStart) ? "SUCCESS" : "SUCCESS *";
					} else {
						String lastDate = null;
						for (String rd : dateRate.keySet()) {
							if ( 0 < rd.compareTo(tInstant) ) {
								break;
							}
							lastDate = rd;
						}

						if ( null != lastDate ) {
							Date firstDate = sdf.parse(tInstant);
							Date secondDate = sdf.parse(lastDate);

							long diffInMillies = Math.abs(secondDate.getTime() - firstDate.getTime());
							long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

							if ( diff <= 7 ) {
								div = dateRate.get(lastDate);
								count = 1;
								val = val / div;
								status = "SUCCESS *";
							}
						}

						if ( null == div ) {
							val = null;
							status = "No rate for Instant";
						}
					}
				} else {
					for (Map.Entry<String, Double> er : dateRate.entrySet()) {
						String rd = er.getKey();
						if ( 0 <= rd.compareTo(tStart) ) {
							if ( 0 < rd.compareTo(tEnd) ) {
								break;
							}
							++count;
							Double rv = er.getValue();
							div = (null == div) ? rv : div + rv;
						}
					}

					if ( 0 < count ) {
						div = div / count;
						val = val / div;
						status = "SUCCESS";
					} else {
						val = null;
						status = "No rate for Period";
					}
				}
			}
		}

		if ( null != val ) {
			String dec = (String) fact.get("Dec");
			int decInt = 0;
			if ( !DustUtils.isEmpty(dec) && !"INF".equals(dec) ) {
				decInt = Integer.parseInt(dec);
			}

			BigDecimal bd = new BigDecimal(val);

			if ( 0 != decInt ) {
				bd = bd.setScale(decInt, RoundingMode.FLOOR);
			}
			fact.put("cvtValue", bd.toPlainString());
			fact.put("cvtRate", div);
			fact.put("cvtCount", count);
		} else {
			val = Double.NaN;
		}

		fact.put("cvtStatus", status);
		fact.put("cvtCurrency", ccy);

		return val;
	}
}