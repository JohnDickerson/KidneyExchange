package edu.cmu.cs.dickerson.kpd.dynamic.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sun.istack.internal.logging.Logger;

import edu.cmu.cs.dickerson.kpd.dynamic.arrivals.ExponentialArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.dynamic.arrivals.PoissonArrivalDistribution;

public class CompetitiveDynamicSimulator extends DynamicSimulator {

	private static final Logger logger = Logger.getLogger(CompetitiveDynamicSimulator.class);

	private double gamma;  // gamma fraction of incoming pairs go to both pools, 1-gamma choose only one pool
	private double alpha;  // alpha fraction of pairs who choose one pool go to Greedy, 1-alpha go to Patient pool
	private double m;         // expect m vertices to arrive each time period, distributed as Poisson dist. with parameter m
	private double lambda;    // vertex lifespan is drawn from Poisson dist. with parameter lambda
	private Random r;      // set seeds for repeatability

	private ExponentialArrivalDistribution arrivalTimeGen;
	private ExponentialArrivalDistribution lifespanTimeGen;

	public enum VertexMarketChoice {GREEDY, PATIENT, COMPETITIVE};

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

		this.arrivalTimeGen = new ExponentialArrivalDistribution(m, r);
		this.lifespanTimeGen = new ExponentialArrivalDistribution(lambda, r);

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

		// Preload all arrival and exit times for vertices, as well as their market entry decisions
		List<Double> entryTimes = new ArrayList<Double>();
		List<Double> exitTimes = new ArrayList<Double>();
		List<VertexMarketChoice> marketChoices = new ArrayList<VertexMarketChoice>();

		double currTime=0.0;
		while(true) {	
			
			// Draw 
			// It is okay if arrivalTime == exitTime; the vertex enters and is critical simultaneously
			
			double arrivalTime = currTime + arrivalTimeGen.draw();
			double exitTime = arrivalTime + lifespanTimeGen.draw();
			currTime = arrivalTime;
			
			if(currTime > timeLimit) {
				break;
			}
			
			VertexMarketChoice marketChoice = this.r.nextDouble() < gamma ? VertexMarketChoice.COMPETITIVE : 
				this.r.nextDouble() < alpha ? VertexMarketChoice.GREEDY : VertexMarketChoice.PATIENT;

			entryTimes.add(arrivalTime);
			exitTimes.add(exitTime);
			marketChoices.add(marketChoice);
		};
		
		logger.info("Pre-generated entry times; we will see " + entryTimes.size() + " vertices in total.");
		
	}


}
