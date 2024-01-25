package hu.sze.uni.xbrl.edgar;

import java.io.File;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsApache;
import hu.sze.milab.dust.utils.DustUtilsFile;

public class XbrlEdgarAgentUnzip extends DustAgent implements XbrlEdgarConsts {

	@Override
	public MindHandle agentBegin() throws Exception {
		File fDataRoot = DustUtilsFile.getFile(MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_TARGET, RESOURCE_ATT_URL_PATH);
		Dust.access(MIND_TAG_CONTEXT_SELF, MIND_TAG_ACCESS_SET, fDataRoot, MISC_ATT_CONN_TARGET, MISC_ATT_VARIANT_VALUE);
		
		return MIND_TAG_RESULT_READACCEPT;
	}

	@Override
	public MindHandle agentProcess() throws Exception {
		String name = Dust.access(MIND_TAG_CONTEXT_TARGET, MIND_TAG_ACCESS_PEEK, "", RESOURCE_ATT_URL_PATH);

		if ( name.endsWith(DUST_EXT_JSON) ) {
			ZipArchiveEntry zipArchiveEntry = Dust.access(MIND_TAG_CONTEXT_TARGET, MIND_TAG_ACCESS_PEEK, null, RESOURCE_ASP_STREAM);
			
			String fName = DustUtils.getPostfix(name, File.separator);
			String id = DustUtils.cutPostfix(fName, ".");
			id = id.split("-")[0];

			String idHash = DustUtils.getHash2(id, File.separator);
			
			File fDataRoot = Dust.access(MIND_TAG_CONTEXT_SELF, MIND_TAG_ACCESS_PEEK, null, MISC_ATT_CONN_TARGET, MISC_ATT_VARIANT_VALUE);			
			File fDir = new File(fDataRoot, idHash);
			File fData = new File(fDir, fName);
			
			if ( fDir.isDirectory() ) {
				if ( fData.isFile() && (zipArchiveEntry.getSize() == fData.length())) {
					return MIND_TAG_RESULT_READACCEPT;
				}
			} else {
				fDir.mkdirs();
			}
			
			ZipFile zipFile = Dust.access(MIND_TAG_CONTEXT_TARGET, MIND_TAG_ACCESS_PEEK, null, MISC_ATT_VARIANT_VALUE);

			DustUtilsApache.unzipEntry(zipFile, zipArchiveEntry, fData);
			
		}

		return MIND_TAG_RESULT_READACCEPT;
	}
}
