package hu.sze.uni.xbrl.edgar;

import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.uni.xbrl.XbrlConsts;

public interface XbrlEdgarConsts extends XbrlConsts {
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
		
		__FilingCount, __FormTypes
	}
	
	String EDGAR_HEAD_HEADER = DustUtils.sbAppend(null, "\t", true, (Object[])EdgarHeadFields.values()).toString();

}
