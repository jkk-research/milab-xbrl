package hu.sze.uni.xbrl;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import hu.sze.milab.dust.stream.xml.DustXmlUtils;

public class XbrlUtils implements XbrlConsts {
	private static Map<FactFldCommon, String> CONTEXT_FIELDS = new HashMap<>();
	
	static {
		CONTEXT_FIELDS.put(FactFldCommon.EntityId, "xbrli:entity");
		CONTEXT_FIELDS.put(FactFldCommon.StartDate, "xbrli:startDate");
		CONTEXT_FIELDS.put(FactFldCommon.EndDate, "xbrli:endDate");
		CONTEXT_FIELDS.put(FactFldCommon.Instant, "xbrli:instant");
	}
	
	public static void loadCtxFields(Element e, Map<String, String> cd) {
		for (  Map.Entry<FactFldCommon, String> ce : CONTEXT_FIELDS.entrySet() ) {
			DustXmlUtils.optLoadInfo(cd, e, ce.getValue(), ce.getKey().name());
		}
	}
}
