package com.xbrldock.utils;

import java.io.File;

public class XbrlDockUtilsFile {
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
}
