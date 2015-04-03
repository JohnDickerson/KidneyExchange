package edu.cmu.cs.dickerson.kpd.dynamic.simulator;

import java.util.Random;

import com.sun.istack.internal.logging.Logger;

public class CompetitiveDynamicSimulator extends DynamicSimulator {

	private static final Logger logger = Logger.getLogger(CompetitiveDynamicSimulator.class);

	private double gamma;  // gamma fraction of incoming pairs go to both pools, 1-gamma choose only one pool
	private double alpha;  // alpha fraction of pairs who choose one pool go to Greedy, 1-alpha go to Patient pool
	private double m;      // expect m vertices to arrive each time period, distributed as Poisson dist. with parameter m
	private double lambda; // vertex lifespan is drawn from Poisson dist. with parameter lambda
	private Random r;      // set seeds for repeatability

	public CompetitiveDynamicSimulator(double gamma, double alpha, double m, double lambda, Random r) {
		super();
		
		if(alpha < 0.0 || alpha > 1.0) {
			throw new IllegalArgumentException("alpha must be in [0,1]");
		}
		this.alpha = alpha;

		if(gamma < 0.0 || gamma > 1.0) {
			throw new IllegalArgumentException("gamma must be in [0,1]");
		}
		this.gamma = gamma;

		if(m <= 0.0) {
			throw new IllegalArgumentException("m must be positive");
		}
		this.m = m;

		if(lambda <= 0.0) {
			throw new IllegalArgumentException("lambda must be positive");
		}
		this.lambda = lambda;

		if(null == r) {
			throw new IllegalArgumentException("Must pass in a non-null Random number generator");
		}
		this.r = r;
		
		logger.info("Constructed CompetitiveDynamicSimulator with parameters:" + 
					"\n * alpha  = " + alpha + 
					"\n * gamma  = " + gamma + 
					"\n * m      = " + m +
					"\n * lambda = " + lambda);
	}
	
	
	public void run(double timeLimit) {
		
		if(timeLimit <= 0.0) { 
			throw new IllegalArgumentException("Time limit must be positive.");
		}
		
		double currTime=0.0;
		do {	
			// First, draw new vertices that enter the pool this tick (in order)
			
			
		} while(currTime <= timeLimit);
	}
	
	
}
