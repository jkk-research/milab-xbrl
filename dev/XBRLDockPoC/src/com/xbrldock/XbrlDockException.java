package com.xbrldock;

import java.io.PrintStream;

import com.xbrldock.utils.XbrlDockUtils;

public final class XbrlDockException extends RuntimeException implements XbrlDockConsts {
	private static final long serialVersionUID = 1L;

	public static PrintStream DUMP_STACK_TRACE = System.err;

	private XbrlDockException(Throwable src, Object... params) {
		super(XbrlDockUtils.toString(XbrlDockUtils.sbAppend(null, ",", false, params)), src);

		log(true, src, getMessage());
	}

	public static void swallow(Throwable src, Object... params) {
		log(true, src, XbrlDockUtils.toString(XbrlDockUtils.sbAppend(null, ",", false, params)));
	}

	public static <FakeRet> FakeRet wrap(Throwable src, Object... params) {
		if (src instanceof XbrlDockException) {
			throw (XbrlDockException) src;
		}

		throw new XbrlDockException(src, params);
	}

	private static void log(boolean thrown, Throwable src, String msg) {
		XbrlDock.log(EventLevel.Exception, src, msg);

		if ((null != DUMP_STACK_TRACE) && (null != src)) {
			src.printStackTrace(DUMP_STACK_TRACE);
		}
	}
}