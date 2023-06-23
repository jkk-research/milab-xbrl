package hu.sze.milab.dust;

public final class DustException extends RuntimeException implements DustConsts {
	private static final long serialVersionUID = 1L;

	public DustException(Throwable src) {
		super(src);
	}

	public static void swallow(Throwable src, Object... params) {
		Dust.dumpObs(src, params);
		src.printStackTrace();
	}

	public static <FakeRet> FakeRet wrap(Throwable src, Object... params) {
		if ( src instanceof DustException ) {
			throw (DustException) src;
		}
	
		Dust.dumpObs(src, params);
		throw new DustException(src);
	}
}