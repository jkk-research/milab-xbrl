package hu.sze.milab.dustsimple;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("unchecked")
public class DustUtils implements DustConsts {
	
	public static boolean isEqual(Object o1, Object o2) {
		return (null == o1) ? (null == o2) : (null != o2) && o1.equals(o2);
	}

	public static String toString(Object ob) {
		return toString(ob, ", ");
	}

	public static String toString(Object ob, String sep) {
		if ( null == ob ) {
			return "";
		} else if ( ob.getClass().isArray() ) {
			StringBuilder sb = null;
			for ( Object oo : (Object[]) ob) {
				sb = sbAppend(sb, sep, false, oo);
			}
			return ( null == sb ) ? "" : sb.toString();
		} else { 
			return ob.toString();
		}
	}
	
	public static boolean isEmpty(String str) {
		return (null == str) || str.isEmpty();
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
	
	public static <RetType> RetType createInstance(ClassLoader cl, String className) {
		try {
			return (RetType) cl.loadClass(className).getConstructor().newInstance();
		} catch (Throwable e) {
			return DustException.wrap(e);
		}
	}

	public static boolean isAccessAdd(MindAccess acc) {
		switch ( acc ) {
		case Get:
		case Insert:
		case Set:
			return true;
		default:
			return false;
		}
	}

	public static String wrapCollSize(MindColl coll, Number size) {
		switch (coll) {
		case Arr:
			return "[" + size + "]";
		case Map:
			return "{" + size + "}";
		case One:
			return  size.toString() ;
		case Set:
			return "(" + size + ")";			
		}
		
		return "?";
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
}
