package edu.cmu.cs.dickerson.kpd.helper;

public final class MathUtil {

	private MathUtil() {}
	
	public static boolean isInteger(double d) {
		return (d == Math.rint(d) && !Double.isInfinite(d) && !Double.isNaN(d));
	}
}
