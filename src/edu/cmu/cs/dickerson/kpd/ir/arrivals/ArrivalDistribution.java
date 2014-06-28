package edu.cmu.cs.dickerson.kpd.ir.arrivals;

import java.util.Random;

public abstract class ArrivalDistribution {

	// Absolute max and min on #vertices arriving in a time period
	protected final int min;
	protected final int max;
	protected Random random;

	public ArrivalDistribution(int min, int max) {
		this(min, max, new Random());
	}
	
	public ArrivalDistribution(int min, int max, Random random) {
		if(max < min) { throw new IllegalArgumentException("Maximum arrival rate cannot be less than minimum arrival rate."); }
		if(min < 0) { throw new IllegalArgumentException("Maximum and minimum arrival rates must be nonnegative."); }
		this.min = min;
		this.max = max;
		this.random = random;
	}
	
	public abstract int draw();
	public abstract int expectedDraw();

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}
}
