package com.xbrldock;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsConsts;
import com.xbrldock.utils.XbrlDockUtilsJson;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class XbrlDock implements XbrlDockConsts, XbrlDockUtilsConsts, XbrlDockConsts.GenApp {

	private static XbrlDock XBRLDOCK;
	private static PrintStream PS_LOG = System.out;

//	private static String APP_PREFIX;
	protected final static Map<String, Object> APP_CONFIG = new TreeMap<>();
	protected final static ArrayList<String> APP_ARGS = new ArrayList<>();

	private final Map<String, GenModule> modules = new TreeMap<>();

	public static void main(String[] args) {
		try {
			Map cfgData = XbrlDockUtilsJson.readJson(XDC_FNAME_CONFIG);
			
			if ( null != cfgData ) {
				APP_CONFIG.putAll(cfgData);
			}

			for (Map.Entry<String, String> e : System.getenv().entrySet()) {
				addConfig(e.getKey(), e.getValue());
			}

			Properties props = System.getProperties();
			Set<Object> pks = new TreeSet<>(Collections.reverseOrder());
			pks.addAll( props.keySet());
			for (Object k : pks) {
				String pk = XbrlDockUtils.toString(k);
				addConfig(pk, props.getProperty(pk));
			}
			
			for (String a : args) {
				if (a.startsWith("-")) {
					String name = a.substring(1);
					Object val = true;

					int sep = name.indexOf("=");
					if (-1 != sep) {
						val = name.substring(sep + 1);
						name = name.substring(0, sep);
					}

					addConfig(name, val);
				} else {
					APP_ARGS.add(a);
				}
			}

			XBRLDOCK = XbrlDockUtils.createObject(null, XbrlDockUtils.safeGet(APP_CONFIG, XDC_CFGTOKEN_app, MAP_CREATOR));
			
			XBRLDOCK.run();
			
		} catch (Throwable t) {
			XbrlDock.log(EventLevel.Exception, t);
		}
	}
	
	protected abstract void run() throws Exception;

	static void addConfig(String key, Object val) {
		Object[] path = key.split("\\.");
		
		Map root = XbrlDockUtils.isEqual(XDC_CFGPREFIX_xbrldock, path[0]) ? APP_CONFIG : XbrlDockUtils.safeGet(APP_CONFIG, XDC_CFGTOKEN_env, MAP_CREATOR);		
			
		XbrlDockUtils.simpleSet(root, val, path);
	}

//	public void initEnv(String appPrefix, String[] args, Map<String, Object> loadedConfig) {
//		APP_PREFIX = appPrefix;
//		
//		envData.putAll(System.getenv());
//
//		Properties props = System.getProperties();
//		for (Object k : props.keySet()) {
//			String pk = XbrlDockUtils.toString(k);
//			envData.put(pk, props.getProperty(pk));
//		}
//
//		for (String a : args) {
//			if (a.startsWith("-")) {
//				String name = a.substring(1);
//				String val = null;
//
//				int sep = name.indexOf("=");
//				if (-1 != sep) {
//					val = name.substring(sep + 1);
//					name = name.substring(0, sep);
//				}
//
//				envData.put(name, val);
//			} else {
//				APP_ARGS.add(a);
//			}
//		}
//
//		envData.putAll(loadedConfig);
//	}

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

	@Override
	public <RetType> RetType getModule(String moduleKey) {
		Map cfg = XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_app, XDC_CFGTOKEN_modules, moduleKey);
		
		return (RetType) XbrlDockUtils.safeGet(XBRLDOCK.modules, moduleKey, new ItemCreator<Object>() {
			@Override
			public Object create(Object key, Object... hints) {
				try {
					return XbrlDockUtils.createObject(XBRLDOCK, cfg);
				} catch (Exception e) {
					return XbrlDockException.wrap(e, "getModule", moduleKey, cfg);
				}
			}
		});
	}

//	public static <RetType> RetType getConfig(Map<String, Object> source, RetType defVal, Object... path) {
//		Object ret;
//		
//		if ( null == source ) {
//			source = XBRLDOCK.envData;
//		}
//		String p = XbrlDockUtils.sbAppend(null, XDC_SEP_PATH, true, path).toString();
//		
//		ret = source.getOrDefault(p, defVal);
//		
//		if ((null != ret) && !(defVal instanceof String) ) {
//			if ( defVal instanceof Integer ) {
//				ret = ((Number) ret).intValue();
//			} else if ( defVal instanceof Long ) {
//				ret = ((Number) ret).longValue();
//			} else if ( defVal instanceof Float ) {
//				ret = ((Number) ret).floatValue();
//			} else if ( defVal instanceof Double ) {
//				ret = ((Number) ret).doubleValue();
//			} 
//		}
//		
//		return (RetType) ret;
//	}

//	public static Map<String, Object> getSubConfig(Map<String, Object> target, Object... prefix) {
//		if (null == target) {
//			target = new TreeMap<String, Object>();
//		} else {
//			target.clear();
//		}
//
//		StringBuilder sb = new StringBuilder(APP_PREFIX);
//		String p = XbrlDockUtils.sbAppend(sb, XDC_SEP_PATH, true, prefix).append(XDC_SEP_PATH).toString();
//		int pl = p.length();
//
//		for (Map.Entry<String, Object> e : XBRLDOCK.envData.entrySet()) {
//			String k = e.getKey();
//			if (k.startsWith(p)) {
//				target.put(k.substring(pl), e.getValue());
//			}
//		}
//
//		return target;
//	}

}
