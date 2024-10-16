package com.xbrldock.utils;

import org.mvel2.MVEL;

public class XbrlDockUtilsMvel implements XbrlDockUtilsConsts {
	
	public interface MvelDataWrapper {
		Number getNum(String conceptId);
	}
	
	public static Object compile(String expr) {
		return MVEL.compileExpression(expr);
	}
	
	@SuppressWarnings("unchecked")
	public static <RetType> RetType evalCompiled(Object o , Object ctx) {
		return (RetType) MVEL.executeExpression(o, ctx);
	}

}
