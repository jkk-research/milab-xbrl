package hu.sze.uni.xbrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlUtils implements XbrlConsts {
	public static final Map<String, String> XML_TRANSLATE = new HashMap<>();

	static {
		XML_TRANSLATE.put("contextRef", "period");
		XML_TRANSLATE.put("unitRef", "unit");
		XML_TRANSLATE.put("name", "concept");
	}
	
	public static StringBuilder sbAppend(StringBuilder sb, Object sep, boolean strict, Object... objects) {
		for (Object ob : objects) {
			String str = toString(ob);

			if ( strict || (0 < str.length()) ) {
				if ( null == sb ) {
					sb = new StringBuilder(str);
				} else {
					if ( 0 < sb.length() ) {
						sb.append(sep);
					}
					sb.append(str);
				}
			}
		}
		
		return sb;
	}
	
	public static void dump(Object sep, boolean strict, Object... objects) {
		StringBuilder sb = sbAppend(null, sep, strict, objects);
		
		if ( null != sb ) {
			System.out.println(sb);
		}
	}


	public static <RetType> RetType access(Object root, AccessCmd cmd, Object val, Object... path) {
		Object ret = null;

		Object curr = root;
		Object prev = null;
		Object lastKey = null;

		for (Object p : path) {
			prev = curr;
			lastKey = p;

			if ( curr instanceof ArrayList ) {
				curr = ((ArrayList) curr).get((Integer) p);
			} else if ( curr instanceof Map ) {
				curr = ((Map) curr).get(p);
			} else {
				curr = null;
			}

			if ( null == curr ) {
				break;
			}
		}

		ret = (null == curr) ? val : curr;

		if ( (cmd == AccessCmd.Set) && (null != lastKey) ) {
			if ( prev instanceof Map ) {
				((Map) prev).put(lastKey, val);
			}
		}

		return (RetType) ret;
	}

	public static File searchByName(File f, String name) {
		File ret = null;
		
		if ( name.toLowerCase().equals(f.getName().toLowerCase())) {
			return f;
		} else if (f.isDirectory()) {
			for (File ff : f.listFiles()) {
				ret = searchByName(ff, name);
				if ( null != ret ) {
					break;
				}
			}
	  }
		
		return ret;
	}
	
	public static String getHashName(String dirName) {
		int hash = dirName.hashCode();

		int mask = 255;
		int firstDir = hash & mask;
		int secondDir = (hash >> 8) & mask;

		return String.format("%02x%s%02x%s%s", firstDir, File.separator, secondDir, File.separator, dirName);
	}

	public static File getHashDir(File root, String dirName) {
		String path = getHashName( dirName);

		File f = new File(root, path);

		f.mkdirs();

		return f;
	}

	public static void unzip(File fromFile, File destDir) throws Exception {
		byte[] buffer = new byte[1024];

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(fromFile))) {
			for (ZipEntry zipEntry = zis.getNextEntry(); zipEntry != null; zipEntry = zis.getNextEntry()) {
				File newFile = new File(destDir, zipEntry.getName());

				String destDirPath = destDir.getCanonicalPath();
				String destFilePath = newFile.getCanonicalPath();

				if ( !destFilePath.startsWith(destDirPath + File.separator) ) {
					throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
				}

				if ( zipEntry.isDirectory() ) {
					if ( !newFile.isDirectory() && !newFile.mkdirs() ) {
						throw new IOException("Failed to create directory " + newFile);
					}
				} else {
					File parent = newFile.getParentFile();
					if ( !parent.isDirectory() && !parent.mkdirs() ) {
						throw new IOException("Failed to create directory " + parent);
					}

					FileOutputStream oo = new FileOutputStream(newFile);
					int len;
					while ((len = zis.read(buffer)) > 0) {
						oo.write(buffer, 0, len);
					}
					oo.close();
				}
			}

			zis.closeEntry();
		}
	}

	public static String toString(Object ob) {
		return (null == ob) ? "" : ob.toString();
	}
}