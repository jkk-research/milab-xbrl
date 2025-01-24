package com.xbrldock.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevMonitor;

public class XbrlDockUtilsFile implements XbrlDockUtilsConsts {

	public static boolean checkPathBound(File root, String path) throws IOException {
		File f = new File(root, path);
		return f.getCanonicalPath().startsWith(root.getCanonicalPath());
	}

	public static boolean storeRelativePath(File root, File f, Object target, Object... keyPath) throws IOException {
		if (null != f) {
			String path = f.getCanonicalPath().substring(root.getCanonicalPath().length() + 1);
			XbrlDockUtils.simpleSet(target, path, keyPath);
			return true;
		} else {
			return false;
		}
	}

	public static class FileCollector implements GenAgent {
		Set<File> found = new HashSet<>();
		public int limit = Integer.MAX_VALUE;

		@Override
		public Object process(String cmd, Map<String, Object> params) throws Exception {
			boolean ret = true;

			switch (cmd) {
			case XDC_CMD_GEN_Init:
				found.clear();
				break;
			case XDC_CMD_GEN_Process:
				File f = (File) params.get(XDC_GEN_TOKEN_target);
				found.add(f);
				ret = found.size() < limit;
				break;
			default:
				break;
			}

			return ret;
		}

		public Collection<File> getFound() {
			return found;
		}
	};

	public static class FileMonitorFilter implements FileFilter {
		XbrlDockDevMonitor monitor;

		public FileMonitorFilter(XbrlDockDevMonitor monitor) {
			this.monitor = monitor;
		}
		
		@Override
		public boolean accept(File f) {
			if ( f.isFile() ) {
				monitor.step();
			}
			return true;
		}
	};

	public static class FileCounter implements GenAgent {
		long folders;
		long filesize;

		@Override
		public Object process(String cmd, Map<String, Object> params) throws Exception {
			File f = (File) params.get(XDC_GEN_TOKEN_target);
			boolean ret = true;

			switch (cmd) {
			case XDC_CMD_GEN_Init:
				folders = filesize = 0;
				break;
			case XDC_CMD_GEN_Begin:
				++folders;
				break;
			case XDC_CMD_GEN_Process:
				if ( f.isHidden() ) {
					break;
				}

				filesize += f.length();
				break;
			default:
				break;
			}

			return ret;
		}

		public long getFilesize() {
			return filesize;
		}

		public long getFolders() {
			return folders;
		}
	};

	public static class FileCopy implements GenAgent {
		String fromRoot;
		File target;

		long folders;
		long filesize;
		
		public FileCopy(File target) throws Exception {
			setTargetRoot(target);
		}

		@Override
		public Object process(String cmd, Map<String, Object> params) throws Exception {
			File f = (File) params.get(XDC_GEN_TOKEN_target);
			boolean ret = true;

			try {
				switch (cmd) {
				case XDC_CMD_GEN_Init:
					fromRoot = f.getCanonicalPath();
					break;
				case XDC_CMD_GEN_Begin:
					++folders;
					ensureDir(getTarget(f));
					break;
				case XDC_CMD_GEN_Process:
					if ( f.isHidden() ) {
						break;
					}
					filesize += f.length();
					File copy = getTarget(f);
					if ( copy.isFile() && (f.length() == copy.length())) {
						break;
					}
					
					Files.copy(f.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
					break;
				default:
					break;
				}
			} catch (Throwable e) {
				XbrlDockException.wrap(null, "Process error", f.getAbsolutePath(), "under source root", fromRoot);
			}

			return ret;
		}

		void setTargetRoot(File target) throws Exception {
			this.target = target;
			
			ensureDir(target);
		}

		File getTarget(File f) throws Exception {
			String cp = f.getCanonicalPath();
			if (cp.startsWith(fromRoot)) {
				String fn = cp.substring(fromRoot.length());
				return new File(target, fn);
			} else {
				return XbrlDockException.wrap(null, "Invalid path", cp, "not under source root", fromRoot);
			}
		}

		public long getFolders() {
			return folders;
		}
	};

	public static long backup(File from, File to) throws Exception {
		FileCounter cnt = new FileCounter();
		XbrlDockDevMonitor monitor = new XbrlDockDevMonitor("Count", 1000) {
			protected void logState(long ts) {
				XbrlDock.log(EventLevel.Trace, name, "current count", count, " segment time", (ts - tsLast));
			}
		};
		FileMonitorFilter fmt = new FileMonitorFilter(monitor);
		Map<String, Object> cp = new TreeMap<String, Object>();

		cnt.process(XDC_CMD_GEN_Init, XbrlDockUtils.setParamMap(cp, XDC_GEN_TOKEN_target, from));
		
		processFiles(from, cnt, fmt, true, false);
		
		long totalSize = cnt.getFilesize();
		
		if ( null != to) {
			FileCopy cpy = new FileCopy(to);
			cpy.process(XDC_CMD_GEN_Init, XbrlDockUtils.setParamMap(cp, XDC_GEN_TOKEN_target, from));
			
			XbrlDockDevMonitor cpm = new XbrlDockDevMonitor("Count", 1000) {
				protected void logState(long ts) {
					XbrlDock.log(EventLevel.Trace, name, "current amount", cpy.filesize, "current percent", (int) ((100.0 * cpy.filesize) / totalSize), " segment time", (ts - tsLast));
				}
			};
			FileMonitorFilter cpf = new FileMonitorFilter(cpm);
			processFiles(from, cpy, cpf, true, false);
		}
		
		return totalSize;
	};

	public static int processFiles(File f, GenAgent proc) throws Exception {
		return processFiles(f, proc, null, false, false);
	}

	public static int processFiles(File f, GenAgent proc, FileFilter fileFilter) throws Exception {
		return processFiles(f, proc, fileFilter, false, false);
	}

	public static int processFiles(File f, GenAgent proc, FileFilter fileFilter, boolean dirCallBefore, boolean dirCallAfter) throws Exception {
		int count = 0;
		Map<String, Object> cp = new TreeMap<String, Object>();

		if (f.exists()) {
			if (f.isFile()) {
				if ((null == fileFilter) || fileFilter.accept(f)) {
					if ((boolean) proc.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParamMap(cp, XDC_GEN_TOKEN_target, f))) {
						count = 1;
					}
				}
			} else {
				if (dirCallBefore) {
					if (!(boolean) proc.process(XDC_CMD_GEN_Begin, XbrlDockUtils.setParamMap(cp, XDC_GEN_TOKEN_target, f))) {
						return 0;
					}
				}

				for (File c : f.listFiles()) {
					count += processFiles(c, proc, fileFilter, dirCallBefore, dirCallAfter);
				}

				if (dirCallAfter) {
					proc.process(XDC_CMD_GEN_End, XbrlDockUtils.setParamMap(cp, XDC_GEN_TOKEN_target, f));
				}
			}
		}

		return count;
	}

	public static File ensureDir(String fName) throws IOException {
		if ( XbrlDockUtils.isEmpty(fName)) {
			return null;
		}
		File f = new File(fName);
		ensureDir(f);
		return f;
	}

	public static void ensureDir(File f) throws IOException {
		if (!f.isDirectory() && !f.mkdirs()) {
			throw new IOException("failed to create directory " + f);
		}
	}

	public static void extractWithApacheZipFile(File destFile, File zipFile, String name) throws Exception {
		try (ZipFile zf = new ZipFile(zipFile, XDC_CHARSET_UTF8)) {

			if (null == name) {

				for (Enumeration<ZipArchiveEntry> ee = zf.getEntries(); ee.hasMoreElements();) {
					ZipArchiveEntry ze = ee.nextElement();
					File f = new File(destFile, ze.getName());
					if (ze.isDirectory()) {
						ensureDir(f);
					} else {
						unzipEntry(zf, ze, f);
					}
				}

			} else {
				ZipArchiveEntry ze = zf.getEntry(name);
				unzipEntry(zf, ze, destFile);
			}
		}
	}

	public static void unzipEntry(ZipFile zipFile, ZipArchiveEntry zipEntry, File toFile) throws IOException {
		ensureDir(toFile.getParentFile());
		try (OutputStream o = Files.newOutputStream(toFile.toPath())) {
			IOUtils.copy(zipFile.getInputStream(zipEntry), o);
		}
	}

}
