package hu.sze.milab.dustsimple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class Dust implements DustConsts {
	
	static Map<Object, Object> ALL_OBJECTS = new HashMap<>();

	public static <RetType> RetType access(Object root, MindAccess cmd, Object val, Object... path) {
		Object ret = null;

		Object curr = root;
		Object prev = null;
		Object lastKey = null;

		for (Object p : path) {
			prev = curr;
			lastKey = p;

			if ( curr instanceof ArrayList ) {
				curr = ((ArrayList) curr).get((Integer) p);
			} else if ( curr instanceof Map ) {
				curr = ((Map) curr).get(p);
			} else {
				curr = null;
			}

			if ( null == curr ) {
				break;
			}
		}

		ret = (null == curr) ? val : curr;

		if ( (cmd == MindAccess.Set) && (null != lastKey) ) {
			if ( prev instanceof Map ) {
				((Map) prev).put(lastKey, val);
			}
		}

		return (RetType) ret;
	}

	public static void dump(Object sep, boolean strict, Object... objects) {
		StringBuilder sb = DustUtils.sbAppend(null, sep, strict, objects);
		
		if ( null != sb ) {
			System.out.println(sb);
		}
	}

	public static void dump(Object... obs) {
		log(null, obs);
	}

	public static void log(Object event, Object... params) {
		dump(", ", false, params);
	}
}
