package hu.sze.uni.xbrl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import hu.sze.milab.dust.Dust;

public class XbrlUtilsCounter implements Iterable<Map.Entry<Object, Long>> {
	Map<Object, Long> counts;

	public XbrlUtilsCounter(boolean sorted) {
		counts = sorted ? new TreeMap<>() : new HashMap<>();
	}

	public void reset() {
		if ( null != counts ) {
			counts.clear();
		}
	}

	public void add(Object ob) {
		Long l = counts.getOrDefault(ob, 0L);
		counts.put(ob, l + 1);
	}

	@Override
	public Iterator<Entry<Object, Long>> iterator() {
		return counts.entrySet().iterator();
	}

	public void dump(String head) {
		Dust.dumpObs(head);
		Dust.dumpObs("{");
		for (Map.Entry<Object, Long> e : counts.entrySet()) {
			Dust.dumpObs("   " + e.getKey(), e.getValue());
		}
		Dust.dumpObs("}\n");
	}
}
