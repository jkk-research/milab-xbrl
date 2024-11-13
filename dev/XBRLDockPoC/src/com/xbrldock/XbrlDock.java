package com.xbrldock;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsConsts;
import com.xbrldock.utils.XbrlDockUtilsJson;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class XbrlDock implements XbrlDockConsts, XbrlDockUtilsConsts, XbrlDockConsts.GenAgent {

	private static XbrlDock XBRLDOCK;
	
	private static PrintStream LOG_STREAM = System.out;
	private static EventLevel LOG_LIMIT_ABOVE = EventLevel.Context;
	private static Object[] LOG_CONTEXT_PENDING = null;

	protected final static Map<String, Object> APP_CONFIG = new TreeMap<>();
	protected final static ArrayList<String> APP_ARGS = new ArrayList<>();

	private final Map<String, GenAgent> agents = new TreeMap<>();

	public static void main(String[] args) {
		try {
			Map cfgData = XbrlDockUtilsJson.readJson(XDC_FNAME_CONFIG);

			if (null != cfgData) {
				APP_CONFIG.putAll(cfgData);
			}

			for (Map.Entry<String, String> e : System.getenv().entrySet()) {
				addConfig(e.getKey(), e.getValue());
			}

			Properties props = System.getProperties();
			Set<Object> pks = new TreeSet<>(Collections.reverseOrder());
			pks.addAll(props.keySet());
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

			String userName = XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_env, "user", "name");
			Map<String, Collection> ufm = XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_app, XDC_CFGTOKEN_userFlags);
			Map<String, Boolean> fm = XbrlDockUtils.safeGet(APP_CONFIG, XDC_CFGTOKEN_userFlags, SORTEDMAP_CREATOR);

			if (null != ufm) {
				for (Map.Entry<String, Collection> ufe : ufm.entrySet()) {
					fm.put(ufe.getKey(), ufe.getValue().contains(userName));
				}
			}

			XBRLDOCK = XbrlDockUtils.createObject(XbrlDockUtils.safeGet(APP_CONFIG, XDC_CFGTOKEN_app, MAP_CREATOR));

			XBRLDOCK.process(XDC_CMD_GEN_Begin);

		} catch (Throwable t) {
			XbrlDock.log(EventLevel.Exception, t);
		}

		LOG_STREAM.println("**********************");
		LOG_STREAM.println("*                    *");
		LOG_STREAM.println("*   Main Complete    *");
		LOG_STREAM.println("*                    *");
		LOG_STREAM.println("**********************");
	}

//	protected abstract void run() throws Exception;

	static void addConfig(String key, Object val) {
		Object[] path = key.split("\\.");

		Map root = XbrlDockUtils.isEqual(XDC_CFGPREFIX_xbrldock, path[0]) ? APP_CONFIG : XbrlDockUtils.safeGet(APP_CONFIG, XDC_CFGTOKEN_env, MAP_CREATOR);

		XbrlDockUtils.simpleSet(root, val, path);
	}

	public static void setLogStream(PrintStream ps) {
		LOG_STREAM = (null == ps) ? System.out : ps;
	}

	public static void log(EventLevel level, Object... params) {
		if (EventLevel.Context == level) {
			LOG_CONTEXT_PENDING = params;
		} else {
			if (0 > level.compareTo(LOG_LIMIT_ABOVE)) {
				if (null != LOG_CONTEXT_PENDING) {
					System.out.println();
					handleLogDefault(EventLevel.Context, LOG_CONTEXT_PENDING);
					LOG_CONTEXT_PENDING = null;
				}
				handleLogDefault(level, params);
			}
		}
	}

	protected static void handleLogDefault(EventLevel level, Object... params) {
		handleLogDefault(LOG_STREAM, level, params);
	}

	public static void handleLogDefault(PrintStream target, EventLevel level, Object... params) {
		StringBuilder sb = XbrlDockUtils.sbAppend(null, ", ", false, params);
		target.println(XbrlDockUtils.strTime() + " " + level + " " + XbrlDockUtils.toString(sb));
		target.flush();
	}
	
	public static boolean checkFlag(String flag) {
		return Boolean.TRUE.equals(XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_userFlags, flag));
	}

	public static GenAgent getAgent(String agentId) {
		Map cfg = XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_app, XDC_CFGTOKEN_agents, agentId);

		GenAgent agent = XbrlDockUtils.safeGet(XBRLDOCK.agents, agentId, new ItemCreator<GenAgent>() {
			@Override
			public GenAgent create(Object key, Object... hints) {
				try {
					return XbrlDockUtils.createObject(cfg);
				} catch (Exception e) {
					return XbrlDockException.wrap(e, "getAgent", agentId, cfg);
				}
			}
		});

		return agent;
	}

	public static <RetType> RetType callAgentNoEx(String agentId, String command, Object... params) {
		try {
			return callAgent(agentId, command, params);
		} catch (Exception e) {
			return XbrlDockException.wrap(e, "callAgent", agentId, command, params);
		}
	}

	public static <RetType> RetType callAgent(String agentId, String command, Object... params) throws Exception {
		Object ret = null;

		GenAgent agent = getAgent(agentId);

		ret = agent.process(command, params);

		return (RetType) ret;
	}

}
