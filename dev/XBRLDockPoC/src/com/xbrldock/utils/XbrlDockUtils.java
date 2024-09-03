package com.xbrldock.utils;

import java.util.ArrayList;
import java.util.Map;
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
		for ( int i = arr.size(); i <= idx; ++i ) {
			arr.add(null);
		}
	}

	public static int safeArrPut(ArrayList arr, int index, Object value, boolean overwrite) {
		int idx;
		int s = arr.size();

		if ( KEY_ADD == index ) {
			idx = s;
			arr.add(value);
		} else {
			if ( index < s ) {
				if ( overwrite ) {
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
		
		if ( src instanceof Map ) {
			prefix.append(sep);
			for ( Map.Entry<String, Object> e : ((Map<String, Object>)src).entrySet() ) {
				prefix.setLength(l);
				prefix.append(e.getKey());
				toFlatMap(target, prefix, sep, e.getValue());
			}
		} else if ( src instanceof Iterable) {
			int idx = 0;
			prefix.append(sep);
			for ( Object o : (Iterable) src) {
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
		if ( null == ob ) {
			return "";
		} else if ( ob.getClass().isArray() ) {
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

			if ( strict || (0 < str.length()) ) {
				if ( null == sb ) {
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
			if ( null == curr ) {
				break;
			}
			if ( p instanceof Integer ) {
				int idx = (Integer) p;
				ArrayList l = (ArrayList) curr;
				curr = ((0 < idx) && (idx < l.size())) ? l.get(idx) : null;
			} else {
				if ( p instanceof Enum ) {
					p = p.toString();
				}
				curr = ((Map) curr).get(p);
			}
		}

		return (RetType) curr;
	}

	public static <RetType> RetType safeGet(Object map, Object key, ItemCreator<RetType> creator, Object... hints) {
		synchronized (map) {
			RetType ret = ((Map<Object, RetType>) map).get(key);
			if ( (null == ret) && (null != creator) ) {
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

	public static String getHash2(String str, String sep) {
		int hash = str.hashCode();

		int mask = 255;
		int h1 = hash & mask;
		int h2 = (hash >> 8) & mask;

		return String.format("%02x%s%02x", h1, sep, h2);
	}
	


}
