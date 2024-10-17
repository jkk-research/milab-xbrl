package com.xbrldock.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

public class XbrlDockUtilsFile implements XbrlDockUtilsConsts {
	
	public static boolean storeRelativePath(File root, File f, Object target, Object ... keyPath) throws IOException {
		if (null != f) {
			String path = f.getCanonicalPath().substring(root.getCanonicalPath().length() + 1);
			XbrlDockUtils.simpleSet(target, path, keyPath);
			return true;
		} else {
			return false;
		}
	}

	public interface FileProcessor extends GenProcessor<File> {
	}
	
	public static class FileCollector implements FileProcessor {
		Set<File> found = new HashSet<>();
		public int limit = Integer.MAX_VALUE;
		
		@Override
		public boolean process(File f, ProcessorAction action) {
			boolean ret = true;
			
			switch ( action ) {
			case Init:
				found.clear(); 
				break;
			case Process:
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
	
	public static int processFiles(File f, FileProcessor proc) throws Exception {
		return processFiles(f, proc, null, false, false);
	}
	
	public static int processFiles(File f, FileProcessor proc, FileFilter fileFilter) throws Exception {
		return processFiles(f, proc, fileFilter, false, false);
	}
	
	public static int processFiles(File f, FileProcessor proc, FileFilter fileFilter, boolean dirCallBefore, boolean dirCallAfter) throws Exception {
		int count = 0; 
		
		if ( f.exists() ) {
			if ( f.isFile() ) {
				if ((null == fileFilter) || fileFilter.accept(f) ) {
					if ( proc.process(f, ProcessorAction.Process) ) {
						count = 1;
					}
				}
			} else {
				if (dirCallBefore) {
					if ( !proc.process(f, ProcessorAction.Begin) ) {
						return 0;
					}
				}
				
				for ( File c : f.listFiles() ) {
					count += processFiles(c, proc, fileFilter, dirCallBefore, dirCallAfter);
				}
				
				if (dirCallAfter) {
					proc.process(f, ProcessorAction.End);
				}
			}
		}
		
		return count;
	}
	
	public static void ensureDir(File f) throws IOException {
		if ( !f.isDirectory() && !f.mkdirs() ) {
			throw new IOException("failed to create directory " + f);
		}
	}
	
	public static void extractWithApacheZipFile(File destFile, File zipFile, String name) throws Exception {
		try (ZipFile zf = new ZipFile(zipFile, XDC_CHARSET_UTF8)) {

			if ( null == name ) {

				for (Enumeration<ZipArchiveEntry> ee = zf.getEntries(); ee.hasMoreElements();) {
					ZipArchiveEntry ze = ee.nextElement();
					File f = new File(destFile, ze.getName());
					if ( ze.isDirectory() ) {
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
