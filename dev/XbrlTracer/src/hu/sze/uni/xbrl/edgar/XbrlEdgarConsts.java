package hu.sze.uni.xbrl.edgar;

import java.util.EnumSet;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustMetaConsts;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsConsts;

public interface XbrlEdgarConsts extends DustMetaConsts, DustUtilsConsts {
	String EDGAR_URL_DATA = "https://www.sec.gov/Archives/edgar/data/";
	String EDGAR_APIHDR_USER = "User-Agent: Szechenyi Istvan University kedves.lorand.laszlo@sze.hu";
	String EDGAR_APIHDR_ENCODING = "Accept-Encoding: gzip, deflate";
	String EDGAR_APIHDR_HOST = "Host: www.sec.gov";
	
	String EDGAR_COMPANY_INDEX = "companies.json";

	enum EdgarSubmissionAtt {
		CIK, accessionNumber, form, size, reportDate, filingDate, acceptanceDateTime,

		isXBRL, isInlineXBRL, act(true), fileNumber, filmNumber, items(true), primaryDocument(true), primaryDocDescription(true),

		;

		public final boolean str;

		private EdgarSubmissionAtt() {
			this(false);
		}

		private EdgarSubmissionAtt(boolean str) {
			this.str = str;
		}
	}

	String EDGAR_FILING_HEADER = DustUtils.sbAppend(null, "\t", true, (Object[]) EdgarSubmissionAtt.values()).toString();

	enum EdgarHeadFields {
		cik, name, formerNames, tickers, exchanges, stateOfIncorporation,

		sic, entityType, category, description, phone, website, investorWebsite,

		ein, fiscalYearEnd, flags, insiderTransactionForIssuerExists, insiderTransactionForOwnerExists,

		__FilingCount, __FormTypes, __PathPrefix,
	}

	String EDGAR_HEAD_HEADER = DustUtils.sbAppend(null, "\t", true, (Object[]) EdgarHeadFields.values()).toString();

	enum EdgarFactField {
		cik, form, accn, filed, fp, fy, frame, taxonomy, concept, unit, instant, start, end, val,

	}

	String EDGAR_FACT_HEADER = DustUtils.sbAppend(null, "\t", true, (Object[]) EdgarFactField.values()).toString();

	EnumSet<EdgarFactField> EDGAR_FACT_EXT = EnumSet.of(EdgarFactField.form, EdgarFactField.accn, EdgarFactField.filed, EdgarFactField.fp, EdgarFactField.fy, EdgarFactField.frame, EdgarFactField.start,
			EdgarFactField.end, EdgarFactField.val);
	
	String EDGARMETA_UNIT = "0";
	
	public static MindHandle EDGARMETA_ATT_JSONDOM = Dust.recall(EDGARMETA_UNIT + DUST_SEP_ID);

}
