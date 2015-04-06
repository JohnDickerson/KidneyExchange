package edu.cmu.cs.dickerson.kpd.dynamic.arrivals;

import java.util.Random;

public abstract class ArrivalDistribution<T extends Number & Comparable<T>> {

	// Absolute max and min on #vertices arriving in a time period
	protected final T min;
	protected final T max;
	protected Random random;

	public ArrivalDistribution(T min, T max) {
		this(min, max, new Random());
	}
	
	public ArrivalDistribution(T min, T max, Random random) {
		if(max.compareTo(min) < 0) { throw new IllegalArgumentException("Maximum arrival rate cannot be less than minimum arrival rate."); }
		this.min = min;
		this.max = max;
		this.random = random;
	}
	
	public abstract T draw();
	public abstract T expectedDraw();

	public T getMin() {
		return min;
	}

	public T getMax() {
		return max;
	}

	public Random getRandom() {
		return random;
	}
}
