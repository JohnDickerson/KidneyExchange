package edu.cmu.cs.dickerson.kpd.dynamic.simulator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import com.sun.istack.internal.logging.Logger;

import edu.cmu.cs.dickerson.kpd.dynamic.arrivals.ExponentialArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.helper.Pair;
import edu.cmu.cs.dickerson.kpd.helper.VertexTimeComparator;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;

public class CompetitiveDynamicSimulator extends DynamicSimulator {

	private static final Logger logger = Logger.getLogger(CompetitiveDynamicSimulator.class);

	private double gamma;  			// gamma fraction of incoming pairs go to both pools, 1-gamma choose only one pool
	private double alpha;  			// alpha fraction of pairs who choose one pool go to Greedy, 1-alpha go to Patient pool
	private double m;      			// expect m vertices to arrive each time period, distributed as Poisson dist. with parameter m
	private double lambda; 			// vertex lifespan is drawn from Poisson dist. with parameter lambda
	private PoolGenerator poolGen;  // generates new vertices/edges from some distribution (e.g., Saidman, UNOS)
	private Random r;      			// set seeds for repeatability

	private ExponentialArrivalDistribution arrivalTimeGen;
	private ExponentialArrivalDistribution lifespanTimeGen;

	public enum VertexMarketChoice {GREEDY, PATIENT, COMPETITIVE};

	public CompetitiveDynamicSimulator(double gamma, double alpha, double m, double lambda, PoolGenerator poolGen, Random r) {
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

		this.arrivalTimeGen = new ExponentialArrivalDistribution(this.m, this.r);
		this.lifespanTimeGen = new ExponentialArrivalDistribution(this.lambda, this.r);

		logger.info("Constructed CompetitiveDynamicSimulator with parameters:" + 
				"\n * alpha  = " + this.alpha + 
				"\n * gamma  = " + this.gamma + 
				"\n * m      = " + this.m +
				"\n * lambda = " + this.lambda);
	}


	public void run(double timeLimit) {

		if(timeLimit <= 0.0) { 
			throw new IllegalArgumentException("Time limit must be positive.");
		}

		// Preload all arrival and exit times for vertices, as well as their market entry decisions
		Deque<Double> entryTimes = new ArrayDeque<Double>();
		Deque<Double> exitTimes = new ArrayDeque<Double>();
		Deque<VertexMarketChoice> marketChoices = new ArrayDeque<VertexMarketChoice>();
		double lastExitTime = -1;
		
		double currTime=0.0;
		while(true) {	
			
			// Draw arrival and departure times from exponential clocks
			// It is okay if entryTime == exitTime; the vertex enters and is critical simultaneously
			double entryTime = currTime + arrivalTimeGen.draw();
			double exitTime = entryTime + lifespanTimeGen.draw();
			currTime = entryTime;
			
			if(currTime > timeLimit) {
				// If this vertex would arrive after the time limit, we're done generating
				break;
			}
			if(exitTime > lastExitTime) {
				// Store the exit time of the final vertex in the pool (after all other vertices have gone critical)
				lastExitTime = exitTime;
			}
			
			// Choose both markets with prob gamma, greedy with prob (1-gamma)alpha, patient otherwise
			VertexMarketChoice marketChoice = this.r.nextDouble() < gamma ? VertexMarketChoice.COMPETITIVE : 
				this.r.nextDouble() < alpha ? VertexMarketChoice.GREEDY : VertexMarketChoice.PATIENT;

			entryTimes.addLast(entryTime);
			exitTimes.addLast(exitTime);
			marketChoices.addLast(marketChoice);
		};
		
		// It is possible that no vertices were generated (first entry draw > timeLimit); that's okay
		logger.info("Pre-generated entry times; we will see " + entryTimes.size() + " vertices in total.");
		
		
		// Initially, the pool is empty at time 0, no vertices are waiting to expire, and no vertices have made market choices
		Pool pool = new Pool(Edge.class);
		currTime = 0.0;
		Queue<Pair<Double, Vertex>> verticesByExitTime = new PriorityQueue<Pair<Double, Vertex>>(0, new VertexTimeComparator());
		
		// Track which vertices are in which markets
		Map<VertexMarketChoice, Set<Vertex>> marketVertexMap = new HashMap<VertexMarketChoice, Set<Vertex>>();
		for(VertexMarketChoice market : VertexMarketChoice.values()) {
			marketVertexMap.put(market, new HashSet<Vertex>());
		}
			
		// Simulate the entry/departure of vertices in the competitive market
		while(!entryTimes.isEmpty() || !verticesByExitTime.isEmpty()) {
			
			// Deal with critical vertices between the current time and the next entry time (inclusive)
			double nextEntryTime = entryTimes.isEmpty() ? Double.MAX_VALUE : entryTimes.peek();
			while(
					!verticesByExitTime.isEmpty() && 
					verticesByExitTime.peek().getLeft() <= nextEntryTime  // this vertex will expire before next vertex enters
					) {
				
				// Get the vertex that is going critical now
				Vertex critVertex = verticesByExitTime.poll().getRight();
				
				// If the vertex is in the patient or competitive markets, try to match it with the patient or competitive pool
				// --> remove matched pairs (success) or just remove it (failure)
				// If the vertex is just in the greedy market, tough; just remove it without matching
				// TODO
			}
			
			// How many vertices enter at the next time point?  (probably just one, sometimes more)
			currTime = entryTimes.pop(); int newVertCount = 1;
			while(!entryTimes.isEmpty() && entryTimes.peek() == currTime) {
				entryTimes.pop(); newVertCount++;
			}
			
			// Add those new vertices to the full pool, and track their exit characteristics/market entries
			Set<Vertex> newVertices = poolGen.addVerticesToPool(pool, newVertCount, 0);
			for(Vertex newVertex : newVertices) {
				// Track when this new vertex will leave the pool, and which market it will enter
				verticesByExitTime.add(new Pair<Double, Vertex>(exitTimes.pop(), newVertex));
				marketVertexMap.get( marketChoices.pop() ).add(newVertex);
			}
			
			// For any new vertices that entered into Greedy or Competitive, try matching now
			for(Vertex newVertex : newVertices) {
				if(marketVertexMap.get(VertexMarketChoice.GREEDY).contains(newVertex) ||
						marketVertexMap.get(VertexMarketChoice.COMPETITIVE).contains(newVertex)) {
					
					// TODO
				}
			}
			
			
		}
	}


}
