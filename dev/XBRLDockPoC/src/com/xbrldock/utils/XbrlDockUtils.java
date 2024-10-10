package com.xbrldock.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlDockUtils implements XbrlDockUtilsConsts {

	public static boolean isEmpty(String str) {
		return (null == str) || str.isEmpty();
	}

	public static boolean isEqual(Object o1, Object o2) {
		return (null == o1) ? (null == o2) : (null != o2) && o1.equals(o2);
	}

	public static String camel2Label(String str) {
		if (isEmpty(str)) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		boolean upBefore = true;
		boolean toUpper = true;

		for (int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);

			if (Character.isUpperCase(c)) {
				if (!upBefore) {
					sb.append(" ");
					upBefore = true;
				}
			} else if ( c == '_' ){
				sb.append("::");
				toUpper = true;
				continue;
			} else {
				upBefore = false;
			}
			
			if ( toUpper ) {
				c = Character.toUpperCase(c);
				toUpper = false;
			}
			
			sb.append(c);
		}

		return sb.toString();
	}

	public static String getCommonPrefix(String a, String b) {
		if (isEmpty(a)) {
			return b;
		} else if (isEmpty(b)) {
			return a;
		} else {
			int minLength = Math.min(a.length(), b.length());
			for (int i = 0; i < minLength; i++) {
				if (a.charAt(i) != b.charAt(i)) {
					return a.substring(0, i);
				}
			}
			return a.substring(0, minLength);
		}
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

	public static int getInt(Object root, Object... path) {
		return ((Number) simpleGet(root, path)).intValue();
	}

	public static <RetType> RetType simpleGet(Object root, Object... path) {
		if ((path.length == 1) && ((String) path[0]).contains(".")) {
			path = ((String) path[0]).split("\\.");
		}

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
		if ((path.length == 1) && ((String) path[0]).contains(".")) {
			path = ((String) path[0]).split("\\.");
		}

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
		return (-1 == sep) ? strSrc : strSrc.substring(sep + pfSep.length());
	}

	public static String cutPostfix(String strSrc, String pfSep) {
		int sep = strSrc.lastIndexOf(pfSep);
		return (-1 == sep) ? strSrc : strSrc.substring(0, sep);
	}

	public static String replacePostfix(String strSrc, String pfSep, String postfix) {
		int sep = strSrc.lastIndexOf(pfSep);
		return (-1 == sep) ? strSrc : strSrc.substring(0, sep + pfSep.length()) + postfix;
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

	public static <RetType> RetType createObject(Object owner, Map config) throws Exception {
		RetType ob = (RetType) Class.forName((String) config.get(XDC_CFGTOKEN_javaClass)).getConstructor().newInstance();

		if (ob instanceof GenAgent) {
			((GenAgent) ob).initModule(config);
		}

		return ob;
	}

	public static Map ensureMap(Map target, boolean clear) {
		if (null == target) {
			target = new HashMap<String, Object>();
		} else if (clear) {
			target.clear();
		}

		return target;
	}

	public static String optCleanUrl(String url) {
		int psp = url.indexOf(XDC_URL_PSEP) + XDC_URL_PSEP.length();
		String rr = url.substring(psp);
		if (rr.contains("//")) {
			url = url.substring(0, psp) + rr.replaceAll("/+", "/");
		}

		return url;
	}

	public static String optExtendRef(String refPath, String currPath) {
		String realRef = refPath;

		if (!realRef.contains(XDC_URL_PSEP)) {
			if (realRef.startsWith(XDC_URL_HERE)) {
				realRef = currPath + realRef.substring(1);
			} else if (realRef.startsWith(XDC_URL_UP)) {
				do {
					currPath = XbrlDockUtils.cutPostfix(currPath, "/");
					realRef = realRef.substring(XDC_URL_UP.length());
				} while (realRef.startsWith(XDC_URL_UP));

				realRef = currPath + "/" + realRef;
			} else {
				realRef = currPath + "/" + realRef;
			}
		}

		return optCleanUrl(realRef);
	}

}
