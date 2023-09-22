package hu.sze.uni.xbrl.edgar;

import java.util.EnumSet;

import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.uni.xbrl.XbrlConsts;

public interface XbrlEdgarConsts extends XbrlConsts {
	String EDGAR_URL_DATA = "https://www.sec.gov/Archives/edgar/data/";
	String EDGAR_APIHDR_USER = "User-Agent: Szechenyi Istvan University kedves.lorand.laszlo@sze.hu";
	String EDGAR_APIHDR_ENCODING = "Accept-Encoding: gzip, deflate";
	String EDGAR_APIHDR_HOST = "Host: www.sec.gov";
	
	enum EdgarSubmissionAtt {
		CIK, accessionNumber, form, size, 
		reportDate, filingDate, acceptanceDateTime, 
		
		isXBRL, isInlineXBRL, act(true), fileNumber, filmNumber, 
		items(true), primaryDocument(true), primaryDocDescription(true), 
		
		;
		
		public final boolean str;
		
		private EdgarSubmissionAtt() {
			this(false);
		}		
		
		private EdgarSubmissionAtt(boolean str) {
			this.str = str;
		}
	}
	
	String EDGAR_FILING_HEADER = DustUtils.sbAppend(null, "\t", true, (Object[])EdgarSubmissionAtt.values()).toString();

	enum EdgarHeadFields {
		cik,
		name,
		formerNames,
		tickers,
		exchanges,
		stateOfIncorporation,
		
		sic,
		entityType,
		category,
		description,
		phone,
		website,
		investorWebsite,

		ein,
		fiscalYearEnd,
		flags,
		insiderTransactionForIssuerExists,
		insiderTransactionForOwnerExists,
		
		__FilingCount, __FormTypes, __PathPrefix,
	}
	
	String EDGAR_HEAD_HEADER = DustUtils.sbAppend(null, "\t", true, (Object[])EdgarHeadFields.values()).toString();

	enum EdgarFactField {
		cik,
		form,
		accn,
		filed,
		fp,
		fy,
		frame,
		taxonomy,
		concept,
		unit,
		instant,
		start,
		end,
		val,

	}
	
	String EDGAR_FACT_HEADER = DustUtils.sbAppend(null, "\t", true, (Object[])EdgarFactField.values()).toString();
	
	EnumSet<EdgarFactField> EDGAR_FACT_EXT = EnumSet.of(EdgarFactField.form,
			EdgarFactField.accn,
			EdgarFactField.filed,
			EdgarFactField.fp,
			EdgarFactField.fy,
			EdgarFactField.frame,
			EdgarFactField.start,
			EdgarFactField.end,
			EdgarFactField.val);

}
