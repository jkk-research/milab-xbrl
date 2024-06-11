package hu.sze.uni.xbrl.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsFactory;
import hu.sze.uni.xbrl.XbrlConsts;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlCurrencyConverterAgent extends DustAgent implements XbrlConsts {
	public final Pattern ptUnitCurrency = Pattern.compile("ISO4217:(\\w+).*");
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	@Override
	protected MindHandle agentProcess() throws Exception {
		MindHandle hInput = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET);
		MindHandle hLoadItem = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_SOURCE);

		DustUtilsFactory<String, Map<String, Double>> rates = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, DUST_ATT_IMPL_DATA);

		if ( null == rates ) {
			rates = new DustUtilsFactory(new DustCreator<Map<String, Double>>() {
				@Override
				public Map<String, Double> create(Object key, Object... hints) {
					return new TreeMap<>();
				}
			});
			Dust.access(MindAccess.Set, rates, MIND_TAG_CONTEXT_SELF, DUST_ATT_IMPL_DATA);
		}

		if ( hLoadItem == hInput ) {
			String e = Dust.access(MindAccess.Peek, null, hInput, MISC_ATT_VECTOR_COORDINATES, 0);
			String date = Dust.access(MindAccess.Peek, null, hInput, MISC_ATT_VECTOR_COORDINATES, 1);
			String v = Dust.access(MindAccess.Peek, null, hInput, MISC_ATT_VARIANT_VALUE);

			if ( !DustUtils.isEmpty(v) && !"N/A".equals(v) ) {
				Double bd = new Double(v);

				String fmtIn = Dust.access(MindAccess.Peek, null, hInput, EVENT_ATT_EVENT_TIMEFORMAT);
				String fmtCvt = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, EVENT_ATT_EVENT_TIMEFORMAT);

				if ( !DustUtils.isEqual(fmtIn, fmtCvt) ) {
					SimpleDateFormat sdfIn = new SimpleDateFormat(fmtIn);
					SimpleDateFormat sdfCvt = new SimpleDateFormat(fmtCvt);

					date = sdfCvt.format(sdfIn.parse(date));
				}

				rates.get(e).put(date, bd);
			}
		} else {
			Map m = Dust.access(MindAccess.Peek, null, hInput, MISC_ATT_CONN_MEMBERMAP);
			
			optConvert(rates, "EUR", m);
		}

		return MIND_TAG_RESULT_ACCEPT;
	}

	String optGetUnitCurrency(Map fact) {
		String ret = null;

		String unit = (String) fact.get("Unit");

		unit = unit.toUpperCase().trim();
		Matcher m = ptUnitCurrency.matcher(unit);

		if ( m.matches() ) {
			ret = m.group(1);
		}

		return ret;
	}

	Double optConvert(DustUtilsFactory<String, Map<String, Double>> rates, String mainCurrency, Map fact) throws Exception {
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

			if ( "0".equals(vv) || mainCurrency.equals(ccy) ) {
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
			fact.put("CvtValue", bd.toPlainString());
			fact.put("CvtRate", div);
			fact.put("CvtCount", count);
		} else {
			val = Double.NaN;
		}

		fact.put("CvtStatus", status);

		return val;
	}
}