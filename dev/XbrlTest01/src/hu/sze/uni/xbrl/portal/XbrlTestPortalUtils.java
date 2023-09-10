package hu.sze.uni.xbrl.portal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

import hu.sze.milab.dust.utils.DustUtilsFile;

public class XbrlTestPortalUtils extends DustUtilsFile implements XbrlTestPortalConsts {

//	private static final ArchiveStreamFactory af = new ArchiveStreamFactory();

	public static void extractWithApacheZipFile(String name, File zipFile, File destFile) throws Exception {
		try (ZipFile zf = new ZipFile(zipFile)) {

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

	public static void extractWithApacheZipFile(File zipFile, File destFile, String name) throws Exception {
		try (ZipFile zf = new ZipFile(zipFile)) {
			ZipArchiveEntry ze = zf.getEntry(name);
			unzipEntry(zf, ze, destFile);
		}
	}

	public static void extractWithApacheZipFile(File zipFile, File destFile, FilenameFilter fnf) throws Exception {
		try (ZipFile zf = new ZipFile(zipFile)) {
			for (Enumeration<ZipArchiveEntry> ee = zf.getEntries(); ee.hasMoreElements();) {
				ZipArchiveEntry ze = ee.nextElement();
				if ( fnf.accept(null, ze.getName())) {
					unzipEntry(zf, ze, destFile);
					return;
				}
			}
		}
	}

	public static void unzipEntry(ZipFile zipFile, ZipArchiveEntry zipEntry, File toFile) throws IOException {
		ensureDir(toFile.getParentFile());
		try (OutputStream o = Files.newOutputStream(toFile.toPath())) {
			IOUtils.copy(zipFile.getInputStream(zipEntry), o);
		}
	}

	public static void extractWithApache(String name, File zipFile, File destDir) throws Exception {

//		ZipArchiveInputStream i = new ZipArchiveInputStream(new FileInputStream(zipFile), CHARSET_UTF8, true, true, true);

		try (ArchiveInputStream i = new ZipArchiveInputStream(new FileInputStream(zipFile), CHARSET_UTF8, true, true, true)) {
//		try (ArchiveInputStream i = af.createArchiveInputStream("zip", new FileInputStream(zipFile))) {
			for (ArchiveEntry entry = i.getNextEntry(); entry != null; entry = i.getNextEntry()) {

				String en = entry.getName();

				if ( en.equals(name) ) {
					File f = new File(destDir, "test.xhtml");
					OutputStream o = Files.newOutputStream(f.toPath());
					IOUtils.copy(i, o);
					return;
				}

//				if ( !i.canReadEntryData(entry) ) {
//					// log something?
//					continue;
//				}
//				File f = new File(destDir, entry.getName());
//				if ( entry.isDirectory() ) {
//					if ( !f.isDirectory() && !f.mkdirs() ) {
//						throw new IOException("failed to create directory " + f);
//					}
//				} else {
//					File parent = f.getParentFile();
//					if ( !parent.isDirectory() && !parent.mkdirs() ) {
//						throw new IOException("failed to create directory " + parent);
//					}
//					try (OutputStream o = Files.newOutputStream(f.toPath())) {
//						IOUtils.copy(i, o);
//					}
//				}
			}
		}
	}

	public static void testUnzip(File fromFile, File destDir) throws Exception {
//		byte[] buffer = new byte[1024];

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(fromFile))) {
			for (ZipEntry zipEntry = zis.getNextEntry(); zipEntry != null; zipEntry = zis.getNextEntry()) {
				File newFile = new File(destDir, zipEntry.getName());

				String destDirPath = destDir.getCanonicalPath();
				String destFilePath = newFile.getCanonicalPath();

				if ( !destFilePath.startsWith(destDirPath + File.separator) ) {
					throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
				}

//				if ( zipEntry.isDirectory() ) {
//					if ( !newFile.isDirectory() && !newFile.mkdirs() ) {
//						throw new IOException("Failed to create directory " + newFile);
//					}
//				} else {
//					File parent = newFile.getParentFile();
//					if ( !parent.isDirectory() && !parent.mkdirs() ) {
//						throw new IOException("Failed to create directory " + parent);
//					}
//
//					FileOutputStream oo = new FileOutputStream(newFile);
//					int len;
//					while ((len = zis.read(buffer)) > 0) {
//						oo.write(buffer, 0, len);
//					}
//					oo.close();
//				}
			}

			zis.closeEntry();
		}
	}
}
