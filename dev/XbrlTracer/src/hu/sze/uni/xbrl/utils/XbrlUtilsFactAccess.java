package hu.sze.uni.xbrl.utils;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.mvel.DustMvelConsts.MvelDataWrapper;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.uni.xbrl.XbrlPoolLoaderAgent;

public class XbrlUtilsFactAccess implements MvelDataWrapper, XbrlUtilsConsts {
	DecimalFormat df = new DecimalFormat("#");

	String tax;
	Map<Object, String> conceptMap = new HashMap<>();

	Map<String, Object> factVals = new TreeMap<>();
	Map<String, Object> asked = new TreeMap<>();
	Boolean valid;

	public XbrlUtilsFactAccess(MindHandle pool, String tax) {
		this.tax = tax;

		Map<String, Object> taxUsGaap = Dust.access(MindAccess.Peek, null, pool, XbrlPoolLoaderAgent.XBRLDOCK_ATT_POOL_TAXONOMIES, tax, XbrlPoolLoaderAgent.XBRLDOCK_ATT_TAXONOMY_CONCEPTS);
		for (Map.Entry<String, Object> eCon : taxUsGaap.entrySet()) {
			conceptMap.put(eCon.getValue(), tax + ":" + eCon.getKey());
		}

		df.setMaximumFractionDigits(8);
	}

	public void reset(Set<Object> facts) {
		factVals.clear();
		getAsked().clear();

		for (Object f : facts) {
			Object con = Dust.access(MindAccess.Peek, "?", f, XbrlPoolLoaderAgent.XBRLDOCK_ATT_FACT_CONCEPT);
			String key = conceptMap.get(con);
			if ( !DustUtils.isEmpty(key) ) {
				factVals.put(key, f);
			}
		}

		valid = null;
	}

	public boolean isValid() {
		return Boolean.TRUE.equals(valid);
	}

	@Override
	public Number getNum(String conceptId) {
		Object f = factVals.get(conceptId);
		Number ret = Dust.access(MindAccess.Peek, null, f, XbrlPoolLoaderAgent.MISC_ATT_VARIANT_VALUE);

		if ( null == ret ) {
			valid = false;
			ret = 0;
		} else if ( null == valid ) {
			valid = true;
		}

		getAsked().put(conceptId, df.format(ret));

		return ret;
	}

	public Map<String, Object> getAsked() {
		return asked;
	}

}