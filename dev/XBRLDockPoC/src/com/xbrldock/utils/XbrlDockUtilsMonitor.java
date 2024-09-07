package com.xbrldock.utils;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts.EventLevel;

public class XbrlDockUtilsMonitor {
	private String name;
	private long segment;

	private long tsStart;
	private long tsLast;
	
	private long count;
	
	public XbrlDockUtilsMonitor(String name, long segment) {
		reset(name, segment);
	}
	
	public void reset(String name, long segment) {
		this.name = name;
		this.segment = segment;
		tsStart = tsLast = System.currentTimeMillis();
	}
	
	public synchronized boolean step() {
		boolean ret = false;
		++count;
		
		if ( (0 != segment) && (0 == (count % segment)) ) {
			long ts = System.currentTimeMillis();
			XbrlDock.log(EventLevel.Trace, name, "current count", count, " segment time", (ts - tsLast));
			tsLast = ts;
			ret = true;
		}
		
		return ret;
	}
	
	public long getCount() {
		return count;
	}
	
	@Override
	public String toString() {
		return name + " total count: " + count + " time: " + (System.currentTimeMillis() - tsStart);
	}
}
