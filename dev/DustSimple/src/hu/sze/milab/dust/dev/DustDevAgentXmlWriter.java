package hu.sze.milab.dust.dev;

import java.io.PrintStream;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts;
import hu.sze.milab.dust.DustMetaConsts;
import hu.sze.milab.dust.stream.DustStreamConsts;

public class DustDevAgentXmlWriter implements DustMetaConsts, DustStreamConsts, DustConsts.MindAgent {

	public MindHandle hTarget;
	public PrintStream ps;

	private StringBuilder rowPrefix = new StringBuilder();

	private boolean elementPending;

	private void optCloseElement() {
		if ( elementPending ) {
			ps.println(">");
			elementPending = false;
		}
	}

	@Override
	public MindStatus agentExecAction(MindAction action) throws Exception {

		switch ( action ) {
		case Begin:
			optCloseElement();
			ps.print(rowPrefix);
			ps.print("<");
			print(TEXT_ATT_NAMED_NAME);
			elementPending = true;

			rowPrefix.append("  ");
			break;
		case Process:
			Object itemType = Dust.access(hTarget, MindAccess.Peek, null, MIND_ATT_KNOWLEDGE_TAG);

			if ( itemType == XmlData.Attribute ) {
				ps.print(" ");
				print(TEXT_ATT_NAMED_NAME);
				ps.print("=\"");
				print(MISC_ATT_VARIANT_VALUE);
				ps.print("\"");
			} else if ( itemType == XmlData.Content ) {
				optCloseElement();
//				ps.print(rowPrefix);
				print(MISC_ATT_VARIANT_VALUE);
				ps.println();
			} else if ( itemType == XmlData.Element ) {
				optCloseElement();
				ps.print(rowPrefix);
				ps.print("<");
				print(TEXT_ATT_NAMED_NAME);
				ps.println("/>");
			}
			break;
		case End:
			optCloseElement();
			rowPrefix.setLength(rowPrefix.length() - 2);
			ps.print(rowPrefix);
			ps.print("</");
			print(TEXT_ATT_NAMED_NAME);
			ps.println(">");
			break;
		default:
			break;
		}

		return MindStatus.ReadAccept;
	}

	public MindStatus agentExecActionX(MindAction action) throws Exception {
		switch ( action ) {
		case End:
			rowPrefix.setLength(rowPrefix.length() - 2);
			break;
		default:
			break;
		}

		ps.print(rowPrefix);
		ps.print(action);

		print(MIND_ATT_KNOWLEDGE_TAG);
		print(TEXT_ATT_NAMED_NAME);
		print(MISC_ATT_VARIANT_VALUE);

		ps.println();

		switch ( action ) {
		case Begin:
			rowPrefix.append("  ");
			break;
		default:
			break;
		}

		return MindStatus.ReadAccept;
	}

	public void print(Object att) {
//		ps.print(" ");
		Object val = Dust.access(hTarget, MindAccess.Peek, "-", att);
		ps.print(val);
	}

}
