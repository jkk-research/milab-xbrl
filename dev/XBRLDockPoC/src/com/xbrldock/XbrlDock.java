package com.xbrldock;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.xbrldock.utils.XbrlDockUtils;

@SuppressWarnings("unchecked")
public abstract class XbrlDock implements XbrlDockConsts {

	private static XbrlDock XBRLDOCK;
	private static PrintStream PS_LOG = System.out;

	private static String APP_PREFIX;
	private final Map<String, Object> envData = new TreeMap<>();
	private final ArrayList<String> argList = new ArrayList<>();

	protected XbrlDock() {
		XBRLDOCK = this;
	}

	public void initEnv(String appPrefix, String[] args, Map<String, Object> loadedConfig) {
		APP_PREFIX = appPrefix;
		
		envData.putAll(System.getenv());

		Properties props = System.getProperties();
		for (Object k : props.keySet()) {
			String pk = XbrlDockUtils.toString(k);
			envData.put(pk, props.getProperty(pk));
		}

		for (String a : args) {
			if (a.startsWith("-")) {
				String name = a.substring(1);
				String val = null;

				int sep = name.indexOf("=");
				if (-1 != sep) {
					val = name.substring(sep + 1);
					name = name.substring(0, sep);
				}

				envData.put(name, val);
			} else {
				argList.add(a);
			}
		}

		envData.putAll(loadedConfig);
	}

	protected abstract void handleLog(EventLevel level, Object... params);

	protected static void handleLogDefault(EventLevel level, Object... params) {
		handleLogDefault(PS_LOG, level, params);
	}

	public static void handleLogDefault(PrintStream target, EventLevel level, Object... params) {
		StringBuilder sb = XbrlDockUtils.sbAppend(null, ", ", false, params);
		target.println(XbrlDockUtils.strTime() + " " + level + " " + XbrlDockUtils.toString(sb));
		target.flush();
	}

	public static void setLogStream(PrintStream ps) {
		PS_LOG = (null == ps) ? System.out : ps;
	}

	public static void log(EventLevel level, Object... params) {
		if (null == XBRLDOCK) {
			handleLogDefault(level, params);
		} else {
			XBRLDOCK.handleLog(level, params);
		}
	}

	public static <RetType> RetType getConfig(Map<String, Object> source, RetType defVal, Object... path) {
		Object ret;
		
		if ( null == source ) {
			source = XBRLDOCK.envData;
		}
		String p = XbrlDockUtils.sbAppend(null, XDC_SEP_PATH, true, path).toString();
		
		ret = source.getOrDefault(p, defVal);
		
		if ((null != ret) && !(defVal instanceof String) ) {
			if ( defVal instanceof Integer ) {
				ret = ((Number) ret).intValue();
			} else if ( defVal instanceof Long ) {
				ret = ((Number) ret).longValue();
			} else if ( defVal instanceof Float ) {
				ret = ((Number) ret).floatValue();
			} else if ( defVal instanceof Double ) {
				ret = ((Number) ret).doubleValue();
			} 
		}
		
		return (RetType) ret;
	}

	public static Map<String, Object> getSubConfig(Map<String, Object> target, Object... prefix) {
		if (null == target) {
			target = new TreeMap<String, Object>();
		} else {
			target.clear();
		}

		StringBuilder sb = new StringBuilder(APP_PREFIX);
		String p = XbrlDockUtils.sbAppend(sb, XDC_SEP_PATH, true, prefix).append(XDC_SEP_PATH).toString();
		int pl = p.length();

		for (Map.Entry<String, Object> e : XBRLDOCK.envData.entrySet()) {
			String k = e.getKey();
			if (k.startsWith(p)) {
				target.put(k.substring(pl), e.getValue());
			}
		}

		return target;
	}

}
