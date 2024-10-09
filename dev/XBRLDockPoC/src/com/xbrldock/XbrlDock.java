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
public abstract class XbrlDock implements XbrlDockConsts, XbrlDockUtilsConsts {

	private static XbrlDock XBRLDOCK;
	private static PrintStream PS_LOG = System.out;

	protected final static Map<String, Object> APP_CONFIG = new TreeMap<>();
	protected final static ArrayList<String> APP_ARGS = new ArrayList<>();

	private final Map<String, GenAgent> agents = new TreeMap<>();

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
		
		PS_LOG.println("**********************");
		PS_LOG.println("*                    *");
		PS_LOG.println("*      Complete      *");
		PS_LOG.println("*                    *");
		PS_LOG.println("**********************");
	}
	
	protected abstract void run() throws Exception;

	static void addConfig(String key, Object val) {
		Object[] path = key.split("\\.");
		
		Map root = XbrlDockUtils.isEqual(XDC_CFGPREFIX_xbrldock, path[0]) ? APP_CONFIG : XbrlDockUtils.safeGet(APP_CONFIG, XDC_CFGTOKEN_env, MAP_CREATOR);		
			
		XbrlDockUtils.simpleSet(root, val, path);
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
	
	public static GenAgent getAgent(String agentId) throws Exception {
		Map cfg = XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_app, XDC_CFGTOKEN_agents, agentId);
		
		GenAgent agent = XbrlDockUtils.safeGet(XBRLDOCK.agents, agentId, new ItemCreator<GenAgent>() {
			@Override
			public GenAgent create(Object key, Object... hints) {
				try {
					return XbrlDockUtils.createObject(XBRLDOCK, cfg);
				} catch (Exception e) {
					return XbrlDockException.wrap(e, "getModule", agentId, cfg);
				}
			}
		});
		
		return agent;
	}

	public static <RetType> RetType callAgent(String agentId, String command, Object... params) throws Exception {
		Object ret = null;
		GenAgent agent = getAgent(agentId);
		
		if ( !XbrlDockUtils.isEmpty(command) ) {
			ret = agent.process(command, params);
		}
		
		return (RetType) ret;
	}

}
