package com.xbrldock.utils;

import java.util.Map;

import org.mvel2.MVEL;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlDockUtilsMvel implements XbrlDockUtilsConsts {
	
	public interface MvelDataWrapper {
		Number getNum(String conceptId);
		boolean exists(Object key);
		Object get(String key);
	}

	
	public interface MvelUtilsGen {
		boolean exists(Object key);
	}

	
	public static Object compile(String expr) {
		return MVEL.compileExpression(expr);
	}
	
	public static <RetType> RetType evalCompiled(Object o, Object ctx) {				
		return (RetType) ((ctx instanceof Map) ?  MVEL.executeExpression(o, null, (Map) ctx) : MVEL.executeExpression(o, ctx));
	}

	public static <RetType> RetType evalCompiled(Object o, Object ctx, Map data) {				
		return (RetType) MVEL.executeExpression(o, ctx, data);
	}

}
