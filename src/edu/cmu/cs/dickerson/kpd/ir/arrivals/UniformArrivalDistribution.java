package edu.cmu.cs.dickerson.kpd.ir.arrivals;

import java.util.Random;

public class UniformArrivalDistribution extends ArrivalDistribution {

	public UniformArrivalDistribution(int min, int max) {
		super(min, max);
	}
	
	/**
	 * Samples from the uniformal interval [min, max] inclusive, returns int
	 * @param min Minimum value returned by draw()
	 * @param max Maximum value returned by draw()
	 * @param random Random number generator to be used, if provided
	 */
	public UniformArrivalDistribution(int min, int max, Random random) {
		super(min, max, random);
	}

	@Override
	public int draw() {
		if(max == min) { 
			return min; 
		} else {
			return min + random.nextInt(max-min);
		}
	}

	@Override
	public String toString() {
		return "Uniform( [" + min + ", " + max + "] )";
	}
}
