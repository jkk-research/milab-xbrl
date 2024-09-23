package com.xbrldock.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlDockUtils implements XbrlDockUtilsConsts {

	public static boolean isEmpty(String str) {
		return (null == str) || str.isEmpty();
	}

	public static boolean isEqual(Object o1, Object o2) {
		return (null == o1) ? (null == o2) : (null != o2) && o1.equals(o2);
	}

	public static int safeCompare(Object v1, Object v2) {
		return (null == v1) ? (null == v2) ? 0 : 1 : (null == v2) ? 1 : ((Comparable) v1).compareTo(v2);
	};

	public static void ensureArrSize(ArrayList arr, int idx) {
		for (int i = arr.size(); i <= idx; ++i) {
			arr.add(null);
		}
	}

	public static int safeArrPut(ArrayList arr, int index, Object value, boolean overwrite) {
		int idx;
		int s = arr.size();

		if (KEY_ADD == index) {
			idx = s;
			arr.add(value);
		} else {
			if (index < s) {
				if (overwrite) {
					arr.set(index, value);
				} else {
					arr.add(index, value);
				}
			} else {
				ensureArrSize(arr, index);
				arr.set(index, value);
			}
			idx = index;
		}

		return idx;
	}

	public static Map<String, Object> toFlatMap(String prefix, String sep, Object src) {
		return toFlatMap(new TreeMap(), new StringBuilder(prefix), sep, src);
	}

	private static Map<String, Object> toFlatMap(Map<String, Object> target, StringBuilder prefix, String sep, Object src) {
		int l = prefix.length() + 1;

		if (src instanceof Map) {
			prefix.append(sep);
			for (Map.Entry<String, Object> e : ((Map<String, Object>) src).entrySet()) {
				prefix.setLength(l);
				prefix.append(e.getKey());
				toFlatMap(target, prefix, sep, e.getValue());
			}
		} else if (src instanceof Iterable) {
			int idx = 0;
			prefix.append(sep);
			for (Object o : (Iterable) src) {
				prefix.setLength(l);
				prefix.append(idx++);
				toFlatMap(target, prefix, sep, o);
			}
		} else {
			target.put(prefix.toString(), src);
		}

		return target;
	};

	public static String toString(Object ob) {
		return toString(ob, ", ");
	}

	public static String toString(Object ob, String sep) {
		if (null == ob) {
			return "";
		} else if (ob.getClass().isArray()) {
			StringBuilder sb = null;
			for (Object oo : (Object[]) ob) {
				sb = sbAppend(sb, sep, false, oo);
			}
			return (null == sb) ? "" : sb.toString();
		} else {
			return ob.toString();
		}
	}

	public static StringBuilder sbAppend(StringBuilder sb, Object sep, boolean strict, Object... objects) {
		for (Object ob : objects) {
			String str = toString(ob);

			if (strict || (0 < str.length())) {
				if (null == sb) {
					sb = new StringBuilder(str);
				} else {
					sb.append(sep);
					sb.append(str);
				}
			}
		}

		return sb;
	}

	public static <RetType> RetType simpleGet(Object root, Object... path) {
		Object curr = root;

		for (Object p : path) {
			if (null == curr) {
				break;
			}
			if (p instanceof Integer) {
				int idx = (Integer) p;
				ArrayList l = (ArrayList) curr;
				curr = ((0 < idx) && (idx < l.size())) ? l.get(idx) : null;
			} else {
				curr = ((Map) curr).get(p);
			}
		}

		return (RetType) curr;
	}

	public static void simpleSet(Object root, Object val, Object... path) {
		Object curr = root;
		Object lastKey = null;
		Object lastParent = null;

		for (Object p : path) {
			if (null == curr) {
				curr = new HashMap();
				((Map) lastParent).put(lastKey, curr);
			}

			lastKey = p;
			lastParent = curr;

			if (p instanceof Integer) {
				int idx = (Integer) p;
				ArrayList l = (ArrayList) curr;
				curr = ((0 < idx) && (idx < l.size())) ? l.get(idx) : null;
			} else {
				curr = ((Map) curr).get(p);
			}
		}

		((Map) lastParent).put(lastKey, val);
	}

	public static <RetType> RetType safeGet(Object map, Object key, ItemCreator<RetType> creator, Object... hints) {
		synchronized (map) {

			RetType ret = ((Map<Object, RetType>) map).get(key);
			if ((null == ret) && (null != creator)) {
				ret = creator.create(key, hints);
				((Map<Object, RetType>) map).put(key, ret);
			}
			return ret;
		}
	}

	public static String getPostfix(String strSrc, String pfSep) {
		int sep = strSrc.lastIndexOf(pfSep);
		return strSrc.substring(sep + pfSep.length());
	}

	public static String cutPostfix(String strSrc, String pfSep) {
		int sep = strSrc.lastIndexOf(pfSep);
		return (-1 == sep) ? strSrc : strSrc.substring(0, sep);
	}

	public static String replacePostfix(String strSrc, String pfSep, String postfix) {
		int sep = strSrc.lastIndexOf(pfSep);
		return strSrc.substring(0, sep + 1) + postfix;
	}

	private static SimpleDateFormat sdfTime = new SimpleDateFormat(XDC_FMT_TIMESTAMP);
	private static SimpleDateFormat sdfDate = new SimpleDateFormat(XDC_FMT_DATE);

	public static String strTime(Date d) {
		synchronized (sdfTime) {
			return sdfTime.format(d);
		}
	}

	public static String strTime() {
		return strTime(new Date());
	}

	public static String strDate(Date d) {
		synchronized (sdfDate) {
			return sdfDate.format(d);
		}
	}

	public static String strDate() {
		return strDate(new Date());
	}

	public static String getHash2(String str, String sep) {
		int hash = str.hashCode();

		int mask = 255;
		int h1 = hash & mask;
		int h2 = (hash >> 8) & mask;

		return String.format("%02x%s%02x", h1, sep, h2);
	}

	public static <RetType> RetType deepCopyIsh(RetType ob) {

		if ((null == ob) || ob.getClass().isPrimitive() || (ob instanceof String)) {
			return ob;
		}
		
		Object ret = ob;

		if (ob instanceof Map) {
			Map r = new HashMap();
			for (Map.Entry<Object, Object> me : ((Map<Object, Object>) ob).entrySet()) {
				r.put(me.getKey(), deepCopyIsh(me.getValue()));
			}
		} else if (ob instanceof Set) {
			Set r = new HashSet();
			for (Object m : ((Set<Object>) ob)) {
				r.add(deepCopyIsh(m));
			}
		} else if (ob instanceof List) {
			List r = new ArrayList();
			for (Object m : ((List<Object>) ob)) {
				r.add(deepCopyIsh(m));
			}
		}

		return (RetType) ret;
	}

}
