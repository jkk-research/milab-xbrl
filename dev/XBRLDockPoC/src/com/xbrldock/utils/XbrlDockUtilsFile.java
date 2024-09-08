package com.xbrldock.utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

public class XbrlDockUtilsFile implements XbrlDockUtilsConsts {
	public interface FileProcessor {
		boolean process(File f);
	}
	
	public static int processFiles(File f, FileProcessor proc) {
		return processFiles(f, proc, null, false, false);
	}
	
	public static int processFiles(File f, FileProcessor proc, FileProcessor fileFilter) {
		return processFiles(f, proc, fileFilter, false, false);
	}
	
	public static int processFiles(File f, FileProcessor proc, FileProcessor fileFilter, boolean dirCallBefore, boolean dirCallAfter) {
		int count = 0; 
		
		if ( f.exists() ) {
			if ( f.isFile() ) {
				if ((null == fileFilter) || fileFilter.process(f) ) {
					if ( proc.process(f) ) {
						count = 1;
					}
				}
			} else {
				if (dirCallBefore) {
					if ( !proc.process(f) ) {
						return 0;
					}
				}
				
				for ( File c : f.listFiles() ) {
					count += processFiles(c, proc, fileFilter, dirCallBefore, dirCallAfter);
				}
				
				if (dirCallAfter) {
					proc.process(f);
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
		try (ZipFile zf = new ZipFile(zipFile, XBRLDOCK_CHARSET_UTF8)) {

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
