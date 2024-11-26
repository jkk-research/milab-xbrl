package com.xbrldock.poc.conn.concordance;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.XbrlDockPocRefactorUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockConnConcordance
		implements XbrlDockConnConcordanceConsts, XbrlDockPocRefactorUtils, XbrlDockConsts.GenAgent {

	File dirStore;
	File dirInput;

	public XbrlDockConnConcordance() {
		// TODO Auto-generated constructor stub
	}

	public void initModule(Map config) throws Exception {

		dirInput = new File((String) XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirInput));
		XbrlDockUtilsFile.ensureDir(dirInput);

		dirStore = new File((String) XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirStore));
		XbrlDockUtilsFile.ensureDir(dirStore);
	}

	@Override
	public Object process(String command, Object... params) throws Exception {
		Object ret = null;

		switch (command) {
		case XDC_CMD_GEN_Init:
			initModule((Map) params[0]);
			break;
		case XDC_CMD_GEN_REFRESH:
			ret = refresh((Collection<String>) params[0]);
			break;

		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}

		return ret;
	}

//	@Override
	public int refresh(Collection<String> updated) throws Exception {
		int newCount = 0;

		if (null != updated) {
			updated.clear();
		}

		return (null == updated) ? newCount : updated.size();
	}
}
