package hu.sze.milab.dustsimple;

public final class DustException extends RuntimeException implements DustConsts {
	private static final long serialVersionUID = 1L;

	public DustException(Throwable src) {
		super(src);
	}

	public static void swallow(Throwable src, Object... params) {
		Dust.dump(src, params);
		src.printStackTrace();
	}

	public static <FakeRet> FakeRet wrap(Throwable src, Object... params) {
		if ( src instanceof DustException ) {
			throw (DustException) src;
		}
	
		Dust.dump(src, params);
		throw new DustException(src);
	}
}