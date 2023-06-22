package hu.sze.milab.dust.utils;

import hu.sze.milab.dust.DustConsts;
import hu.sze.milab.dust.DustException;

@SuppressWarnings("unchecked")
public class DustUtils implements DustConsts {
	
	public static boolean isEqual(Object o1, Object o2) {
		return (null == o1) ? (null == o2) : (null != o2) && o1.equals(o2);
	}

	public static String toString(Object ob) {
		return toString(ob, ", ");
	}

	public static String toString(Object ob, String sep) {
		if ( null == ob ) {
			return "";
		} else if ( ob.getClass().isArray() ) {
			StringBuilder sb = null;
			for ( Object oo : (Object[]) ob) {
				sb = sbAppend(sb, sep, false, oo);
			}
			return ( null == sb ) ? "" : sb.toString();
		} else { 
			return ob.toString();
		}
	}
	
	public static boolean isEmpty(String str) {
		return (null == str) || str.isEmpty();
	}
	
	public static StringBuilder sbAppend(StringBuilder sb, Object sep, boolean strict, Object... objects) {
		for (Object ob : objects) {
			String str = toString(ob);

			if ( strict || (0 < str.length()) ) {
				if ( null == sb ) {
					sb = new StringBuilder(str);
				} else {
					if ( 0 < sb.length() ) {
						sb.append(sep);
					}
					sb.append(str);
				}
			}
		}
		
		return sb;
	}
	
	public static <RetType> RetType createInstance(ClassLoader cl, String className) {
		try {
			return (RetType) cl.loadClass(className).getConstructor().newInstance();
		} catch (Throwable e) {
			return DustException.wrap(e);
		}
	}

	public static boolean isAccessAdd(MindAccess acc) {
		switch ( acc ) {
		case Get:
		case Insert:
		case Set:
			return true;
		default:
			return false;
		}
	}

	public static String wrapCollSize(MindColl coll, Number size) {
		switch (coll) {
		case Arr:
			return "[" + size + "]";
		case Map:
			return "{" + size + "}";
		case One:
			return  size.toString() ;
		case Set:
			return "(" + size + ")";			
		}
		
		return "?";
	}	
}
