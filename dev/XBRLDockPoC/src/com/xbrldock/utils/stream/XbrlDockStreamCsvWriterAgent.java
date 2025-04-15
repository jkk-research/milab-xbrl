package com.xbrldock.utils.stream;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;

import com.xbrldock.XbrlDockConsts.GenAgent;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;

@SuppressWarnings({ "rawtypes" /*, "unchecked"*/ })
public class XbrlDockStreamCsvWriterAgent implements XbrlDockStreamConsts, GenAgent, Closeable, AutoCloseable {
	private final String sep = "\t";
	private Object[] columns;

	private Object target;
	private Writer writer;

	@Override
	public Object process(String cmd, Map params) throws Exception {
		switch (cmd) {
		case XbrlDockStreamCsv.XDC_CMD_GEN_Init:
			Object[] old = (Object[]) params.get(XDC_GEN_TOKEN_members);
			columns = (0 < old.length) ? Arrays.copyOf(old, old.length) : null;
			break;
		case XbrlDockStreamCsv.XDC_CMD_GEN_Begin:
			target = params.get(XDC_GEN_TOKEN_target);
			break;
		case XbrlDockStreamCsv.XDC_CMD_GEN_Process:
			Map m = (Map) params.get(XDC_EXT_TOKEN_value);

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
		case XbrlDockStreamCsv.XDC_CMD_GEN_End:
			close();
			break;
		case XbrlDockStreamCsv.XDC_CMD_GEN_Release:
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
			String str = (null == val) ? "" : XbrlDockStreamCsv.csvOptEscape(XbrlDockUtils.toString(val), sep);
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