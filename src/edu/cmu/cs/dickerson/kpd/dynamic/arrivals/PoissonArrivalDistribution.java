package edu.cmu.cs.dickerson.kpd.dynamic.arrivals;

import java.util.Random;

public class PoissonArrivalDistribution extends ArrivalDistribution<Integer> {

	private int lambda;
	private double L;

	public PoissonArrivalDistribution(int lambda) {
		this(lambda, new Random());
	}

	public PoissonArrivalDistribution(int lambda, Random random) {
		super(0, Integer.MAX_VALUE, random);
		this.lambda = lambda;
		this.L = Math.exp(-lambda);
	}

	
	@Override
	public Integer draw() {
		// Knuth implementation, not great for large lambdas, but my lambdas are small
		double p = 1.0;
		int k = 0;
		do {
			k++;
			p *= super.random.nextDouble();
		} while(p > this.L);

		return k-1;
	}

	@Override
	public Integer expectedDraw() {
		// Expectation of Poisson(lambda) is lambda
		return lambda;
	}

	@Override
	public String toString() {
		return "Poisson(" + this.lambda + ")";
	}
}
