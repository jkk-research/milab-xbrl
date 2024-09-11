package com.xbrldock.dev;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.xbrldock.utils.XbrlDockUtils;

public class XbrlDockDevCounter implements XbrlDockDevConsts, Iterable<Map.Entry<Object, Long>> {
	String head;
	Map<Object, Long> counts;

	public XbrlDockDevCounter(String head, boolean sorted) {
		this.head = head;
		counts = sorted ? new TreeMap<>() : new HashMap<>();
	}

	public void reset() {
		if ( null != counts ) {
			counts.clear();
		}
	}

	public Long add(Object ob) {
		return add(ob, 1L);
	}

	public Long add(Object ob, long count) {
		Long l = counts.getOrDefault(ob, 0L);
		l += count;
		counts.put(ob, l);
		
		return l;
	}

	@Override
	public Iterator<Entry<Object, Long>> iterator() {
		return counts.entrySet().iterator();
	}

	@Override
	public String toString() {
		StringBuilder sb = XbrlDockUtils.sbAppend(null, "", true, head, " {");
		for (Map.Entry<Object, Long> e : counts.entrySet()) {
			XbrlDockUtils.sbAppend(sb, "\t", true, "\n", e.getKey(), e.getValue());
		}
		
		sb.append("\n }");
		
		return sb.toString();
	}

	public boolean contains(Object ob) {
		return counts.containsKey(ob);
	}
	public Long peek(Object ob) {
		return counts.getOrDefault(ob, 0L);
	}
}
