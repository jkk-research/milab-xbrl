package com.gollywolly.dustcomp.dust.io.poi;

import java.util.EnumSet;

import com.gollywolly.dustcomp.DustCompComponents;
import com.gollywolly.dustcomp.DustCompUtils;
import com.gollywolly.dustcomp.api.DustCompApiUtils;

public interface DustCompPoiComponents extends DustCompComponents, DustCompPoiNames {
	
	DustIdentifierFactory idFact = DustCompApiUtils.getPathBuilder();
	DustIdentifier VISIT_LABEL = idFact.accessPath(DustIdentifier.ICTX_VISIT, new DustIdentifier[]{TYPE_TEXT_HUMANINFO, FLD_LABEL});
	
	DustIdentifier FLD_SHEETNAME = DustCompUtils.getPathBuilder().simpleId("sheetName");
	DustIdentifier FLD_TEMPLATEPATH = DustCompUtils.getPathBuilder().simpleId("templatePath");
	DustIdentifier FLD_TARGETPATH = DustCompUtils.getPathBuilder().simpleId("targetPath");
	DustIdentifier FLD_ROWOPS = DustCompUtils.getPathBuilder().simpleId("RowOps");

	DustIdentifier FLD_OPSOURCE = DustCompUtils.getPathBuilder().simpleId("tmplOpSource");
	DustIdentifier FLD_OPTARGET = DustCompUtils.getPathBuilder().simpleId("tmplOpTarget");
	DustIdentifier FLD_OPMODE = DustCompUtils.getPathBuilder().simpleId("tmplOpMode");
	DustIdentifier FLD_OPFLAGS = DustCompUtils.getPathBuilder().simpleId("tmplOpFlags");
	
	DustIdentifier FLD_OPCOPYTYPE = DustCompUtils.getPathBuilder().simpleId("tmplOpCopyType");
	
	DustIdentifier FLD_OPFINDSTART = DustCompUtils.getPathBuilder().simpleId("tmplOpFindStart");
	DustIdentifier FLD_OPFINDEND = DustCompUtils.getPathBuilder().simpleId("tmplOpFindEnd");
	
	
	
	int IDX_UNSET = -1;

	enum TemplateOp {
		copy, locateRow
	}

	enum TemplateCopyType {
		string, numeric
	}

	enum TemplateOpFlag {
		mandatory,singleValue,mustFind
	}
	
	EnumSet<TemplateOpFlag> NOFLAGS = EnumSet.noneOf(TemplateOpFlag.class);

}
