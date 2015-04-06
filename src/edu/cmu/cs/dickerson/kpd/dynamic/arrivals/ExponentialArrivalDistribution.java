package edu.cmu.cs.dickerson.kpd.dynamic.arrivals;

import java.util.Random;

public class ExponentialArrivalDistribution extends ArrivalDistribution<Double> {
	private double lambda;

	public ExponentialArrivalDistribution(double lambda) {
		this(lambda, new Random());
	}

	public ExponentialArrivalDistribution(double lambda, Random random) {
		super(0.0, Double.MAX_VALUE, random);
		if(lambda <= 0.0) { throw new IllegalArgumentException("Lambda must be positive."); }		
		this.lambda = lambda;
	}

	
	@Override
	public Double draw() {
		return -(Math.log(super.random.nextDouble()) / lambda); 
	}

	@Override
	public Double expectedDraw() {
		assert(lambda!=0.0);
		return 1.0/this.lambda;
	}

	@Override
	public String toString() {
		return "Exponential(" + this.lambda + ")";
	}
}
