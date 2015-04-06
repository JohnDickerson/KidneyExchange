package edu.cmu.cs.dickerson.kpd.dynamic.arrivals;

import java.util.Random;

public class TruncatedNormalIntegerArrivalDistribution extends ArrivalDistribution<Integer> {

	// Truncate the normal at +/- this many standard deviations from mean
	// (Warning: setting this to a small value will result in long draw times)
	private double stdevTrunc = Math.abs( 2.0 );

	public TruncatedNormalIntegerArrivalDistribution(int min, int max) {
		super(min, max);
	}

	/**
	 * Samples from the standard normal distribution, truncates to +/-2 stdevs,
	 * then scales this range to within [min, max], and rounds to nearest int
	 * @param min Minimum value returned by draw()
	 * @param max Maximum value returned by draw()
	 * @param random Random number generator to be used, if provided
	 */
	public TruncatedNormalIntegerArrivalDistribution(int min, int max, Random random) {
		super(min, max, random);
	}

	@Override
	public Integer draw() {

		if(min==max || // point interval, return single point
				stdevTrunc <= 0 // point Gaussian distribution, return mean (halfway point between min and max)
				) { 
			return expectedDraw();
		}

		// Repeatedly sample until we're within two unit stdevs of the mean
		double sample = 0.0;
		do {
			sample = random.nextGaussian();
		} while(sample > stdevTrunc || sample < -stdevTrunc);

		// Scales the sample to [0,1]
		double scaledSample = (sample+stdevTrunc)/(2*stdevTrunc);

		// Scale the sample to [min, max] and rounds to an integer
		return min + (int)Math.min(Math.round( scaledSample * (max-min) ), max);
	}

	@Override
	public Integer expectedDraw() {
		return min+(max-min)/2;   // Not worried about int truncation; caller is responsible for not doing a dumb thing here
	}

	@Override
	public String toString() {
		return "TruncatedNormal( [" + min + " " + max + "] stdev=" + stdevTrunc + ")";
	}
	
}
