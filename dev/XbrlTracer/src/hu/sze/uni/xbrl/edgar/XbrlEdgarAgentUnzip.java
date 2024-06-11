package hu.sze.uni.xbrl.edgar;

import java.io.File;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;
import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsApache;

public class XbrlEdgarAgentUnzip extends DustAgent implements XbrlEdgarConsts {
	
	DustDevCounter cntMulti = new DustDevCounter("Multi-files", true);

	@Override
	protected MindHandle agentBegin() throws Exception {		
		Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_TARGET);

		return MIND_TAG_RESULT_READACCEPT;
	}

	@Override
	protected MindHandle agentProcess() throws Exception {
		String name = Dust.access(MindAccess.Peek, "", MIND_TAG_CONTEXT_TARGET, RESOURCE_ATT_URL_PATH);

		if ( name.endsWith(DUST_EXT_JSON) ) {
			ZipArchiveEntry zipArchiveEntry = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, RESOURCE_ASP_STREAM);
			
			String fName = DustUtils.getPostfix(name, File.separator);
			String id = DustUtils.cutPostfix(fName, ".");
			String[] idSplit = id.split("-");
			id = idSplit[0];
			
			String idHash = DustUtils.getHash2(id, File.separator);
			
			File fDataRoot = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_TARGET, DUST_ATT_IMPL_DATA);			
			File fDir = new File(fDataRoot, idHash);
			File fData = new File(fDir, fName);
			
			if ( 1 < idSplit.length ) {
				cntMulti.add(id);
				Dust.log(EVENT_TAG_TYPE_TRACE, "Multi file", fName, "to", fData.getCanonicalPath());
			}
			
			if ( fDir.isDirectory() ) {
				if ( fData.isFile() && (zipArchiveEntry.getSize() == fData.length())) {
					return MIND_TAG_RESULT_READACCEPT;
				}
			} else {
				fDir.mkdirs();
			}
			
			ZipFile zipFile = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_TARGET, DUST_ATT_IMPL_DATA);
			DustUtilsApache.unzipEntry(zipFile, zipArchiveEntry, fData);
			
//			ZipFile zipFile = Dust.access(MIND_TAG_CONTEXT_TARGET, MIND_TAG_ACCESS_PEEK, null, MISC_ATT_VARIANT_VALUE);
//		DustUtilsApache.unzipEntry(zipFile, zipArchiveEntry, fData);
			
//			Dust.access(MindAccess.Insert, idHash, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_TARGET, MISC_ATT_CONN_MEMBERSET);
			
		}

		return MIND_TAG_RESULT_READACCEPT;
	}
	
	@Override
	protected MindHandle agentEnd() throws Exception {
		Dust.log(EVENT_TAG_TYPE_INFO, cntMulti);
		Dust.access(MindAccess.Commit, MIND_TAG_ACTION_PROCESS, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_TARGET);
		return MIND_TAG_RESULT_ACCEPT;
	}
}
