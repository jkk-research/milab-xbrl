package com.xbrldock.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;

import com.xbrldock.XbrlDockConsts.GenAgent;

@SuppressWarnings({ "rawtypes" /*, "unchecked"*/ })
public class XbrlDockUtilsCsvWriterAgent implements GenAgent, Closeable {
	private final String sep = "\t";
	private Object[] columns;

	private Object target;
	private Writer writer;

	@Override
	public Object process(String cmd, Object... params) throws Exception {
		switch (cmd) {
		case XbrlDockUtilsCsv.XDC_CMD_GEN_Init:
			columns = (0 < params.length) ? Arrays.copyOf(params, params.length) : null;
			break;
		case XbrlDockUtilsCsv.XDC_CMD_GEN_Begin:
			target = params[0];
			break;
		case XbrlDockUtilsCsv.XDC_CMD_GEN_Process:
			Map m = (Map) params[0];

			if (m.isEmpty()) {
				break;
			}

			if (null == columns) {
				columns = m.keySet().toArray();
			} else {
				boolean empty = true;
				for (Object c : columns) {
					if (null != m.get(c)) {
						empty = false;
						break;
					}
				}
				if (empty) {
					break;
				}
			}

			write(m);
			break;
		case XbrlDockUtilsCsv.XDC_CMD_GEN_End:
			close();
			break;
		case XbrlDockUtilsCsv.XDC_CMD_GEN_Release:
			close();
			break;
		}
		return true;
	}

	private void optBegin() throws Exception {
		if (null == writer) {
			if (target instanceof Writer) {
				this.writer = (Writer) target;
			} else {
				File f = null;

				if (target instanceof String) {
					f = new File((String) target);
				} else if (target instanceof File) {
					f = (File) target;
				}

				XbrlDockUtilsFile.ensureDir(f.getParentFile());
				this.writer = new FileWriter(f);

				write(null);
			}
		}
	}

	private void write(Map data) throws Exception {
		optBegin();

		int l = columns.length;
		int i = 0;
		for (Object ch : columns) {
			Object val = (null == data) ? ch : data.get(ch);
			String str = (null == val) ? "" : XbrlDockUtilsCsv.csvOptEscape(XbrlDockUtils.toString(val), sep);
			writer.write(str);
			writer.write(((++i) < l) ? sep : "\n");
		}
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		if (null != writer) {
			writer.flush();

			if (target != writer) {
				writer.close();
			}

			writer = null;
		}
	}
}