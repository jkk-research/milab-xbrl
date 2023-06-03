package hu.sze.uni.xbrl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlHandlerXml extends DefaultHandler implements XbrlConsts {
	private final SimpleDateFormat SDF_PERIOD = new SimpleDateFormat("yyyy-MM-dd");

	protected static final String XBRL_PREFIX_XBRLI = "xbrli:";

	private static final String XBRL_TAG_ID = "xbrli:identifier";
	private static final String XBRL_ATT_SCHEME = "scheme";

	private static final String XBRL_TAG_CONTEXT = "xbrli:context";
	private static final String XBRL_TAG_UNIT = "xbrli:unit";
	private static final String XBRL_TAG_SCHEMAREF = "link:schemaRef";

	private static final String XBRL_TAG_DIVIDE = "xbrli:divide";
	private static final String XBRL_TAG_UNIT_MEASURE = "xbrli:measure";
	private static final String XBRL_TAG_UNIT_NUMERATOR = "xbrli:unitNumerator";
	private static final String XBRL_TAG_UNIT_DENOMINATOR = "xbrli:unitDenominator";

	private static final String XBRL_TAG_ENTITY = "xbrli:entity";

	private static final String XBRL_TAG_PERIOD_INSTANT = "xbrli:instant";
	private static final String XBRL_TAG_PERIOD_STARTDATE = "xbrli:startDate";
	private static final String XBRL_TAG_PERIOD_ENDDATE = "xbrli:endDate";

	private static final Collection<String> SPEC_TAGS = Arrays.asList(XBRL_TAG_CONTEXT, XBRL_TAG_UNIT, XBRL_TAG_SCHEMAREF);

	XbrlListener listener;

	String scheme;
	protected StringBuilder currentValue = new StringBuilder();

	protected Map fact = new HashMap();
	protected Map dim = new HashMap();

	Map spec = new HashMap();

	boolean inSpec;
	boolean divide;
	String measureVal;

	public void setListener(XbrlListener listener) {
		this.listener = listener;
	}

	@Override
	public void startDocument() {
//		System.out.println("Start Document");
		fact.put("dimensions", dim);
		inSpec = false;
	}

	@Override
	public void endDocument() {
//		System.out.println("End Document");
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if ( !inSpec ) {
			if ( SPEC_TAGS.contains(qName) ) {
				spec.clear();
				spec.put(KEY_XBRL_ID, attributes.getValue(KEY_XBRL_ID));
				inSpec = true;
			} else {
				currentValue.setLength(0);

				dim.clear();

				for (int i = attributes.getLength(); i-- > 0;) {
					String qn = attributes.getQName(i);
					dim.put(XbrlUtils.XML_TRANSLATE.getOrDefault(qn, qn), attributes.getValue(i));
				}
				dim.put("concept", qName);
			}
		} else {
			switch ( qName ) {
			case XBRL_TAG_UNIT_MEASURE:
			case XBRL_TAG_ENTITY:
				currentValue.setLength(0);
				break;
			case XBRL_TAG_ID:
				scheme = attributes.getValue(XBRL_ATT_SCHEME);
				break;
			case XBRL_TAG_DIVIDE:
				divide = true;
				break;
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if ( inSpec ) {
			if ( SPEC_TAGS.contains(qName) ) {
				XbrlInfoType xit = null;
				
				switch ( qName ) {
				case XBRL_TAG_CONTEXT:
					xit = XbrlInfoType.Context;
					break;
				case XBRL_TAG_UNIT:
					xit = XbrlInfoType.Unit;
					break;
				}
				inSpec = false;
				
				if ( null != xit ) {
					listener.handleXbrlInfo(xit, spec);
				}
			} else {
				String val = currentValue.toString().trim();
				int d = qName.lastIndexOf(":");
				String key = (-1 == d ) ? qName : qName.substring(d+1);
				switch ( qName ) {
				case XBRL_TAG_DIVIDE:
					divide = false;
					break;
				case XBRL_TAG_UNIT_MEASURE:
					if ( !divide ) {
						spec.put(key, val);
					} else {
						measureVal = val;				
					}
					break;
				case XBRL_TAG_UNIT_NUMERATOR:
				case XBRL_TAG_UNIT_DENOMINATOR:
					spec.put(key, measureVal);
					break;
				case XBRL_TAG_PERIOD_INSTANT:
				case XBRL_TAG_PERIOD_STARTDATE:
				case XBRL_TAG_PERIOD_ENDDATE:
					currentValue.setLength(0);
					try {
						spec.put(key, SDF_PERIOD.parse(val));
					} catch (ParseException e) {
						e.printStackTrace();
					}
					break;
				case XBRL_TAG_ENTITY:
					spec.put(KEY_XBRL_ENTITY, val);
					currentValue.setLength(0);
					break;
				case XBRL_TAG_ID:
					if ( null != scheme ) {
						currentValue.setLength(0);
						currentValue.append(scheme).append("#").append(val);
					}
					break;

				default:
					break;
				}
			}
		} else {
			fact.put("value", currentValue.toString());

			listener.handleXbrlInfo(XbrlInfoType.Fact, fact);
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		currentValue.append(ch, start, length);
	}
}