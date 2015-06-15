package edu.cmu.cs.dickerson.kpd.dynamic.simulator;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import edu.cmu.cs.dickerson.kpd.competitive.MatchingStrategy;
import edu.cmu.cs.dickerson.kpd.dynamic.arrivals.ExponentialArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.helper.Pair;
import edu.cmu.cs.dickerson.kpd.helper.VertexTimeComparator;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;

public class CompetitiveDynamicSimulator extends DynamicSimulator {

	private static final Logger logger = Logger.getLogger(CompetitiveDynamicSimulator.class.getSimpleName());

	private double gamma;  			// gamma fraction of incoming pairs go to both pools, 1-gamma choose only one pool
	private double alpha;  			// alpha fraction of pairs who choose one pool go to Greedy, 1-alpha go to Patient pool
	private double m;      			// expect m vertices to arrive each time period, distributed as Poisson dist. with parameter m
	private double lambda; 			// vertex lifespan is drawn from Poisson dist. with parameter lambda
	private PoolGenerator poolGen;  // generates new vertices/edges from some distribution (e.g., Saidman, UNOS)
	private MatchingStrategy matchingStrategy; // how do we tiebreak to select a match for a vertex?
	private Random r;      			// set seeds for repeatability

	private ExponentialArrivalDistribution arrivalTimeGen;
	private ExponentialArrivalDistribution lifespanTimeGen;

	public enum VertexMarketChoice {GREEDY, PATIENT, COMPETITIVE};

	public CompetitiveDynamicSimulator(double gamma, double alpha, double m, double lambda, PoolGenerator poolGen, MatchingStrategy matchingStrategy, Random r) {
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

		if(null == poolGen) {
			throw new IllegalArgumentException("Must pass in a pool generator");
		}
		this.poolGen = poolGen;

		if(null == matchingStrategy) {
			throw new IllegalArgumentException("Must pass in a matching strategy to tiebreak vertex matches");
		}
		this.matchingStrategy = matchingStrategy;

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


	public CompetitiveDynamicSimulatorData run(double timeLimit) {

		if(timeLimit <= 0.0) { 
			throw new IllegalArgumentException("Time limit must be positive.");
		}

		CompetitiveDynamicSimulatorData data = new CompetitiveDynamicSimulatorData();
		
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
		Queue<Pair<Double, Vertex>> verticesByExitTime = new PriorityQueue<Pair<Double, Vertex>>(entryTimes.size(), new VertexTimeComparator());

		// Track which vertices are in which markets
		Map<VertexMarketChoice, Set<Vertex>> marketVertexMap = new HashMap<VertexMarketChoice, Set<Vertex>>();
		for(VertexMarketChoice market : VertexMarketChoice.values()) {
			marketVertexMap.put(market, new HashSet<Vertex>());
		}

		
		// Bookkeeping for the matching process
		int o_totalMatched = 0;
		int o_totalPatientMatched = 0;
		int o_totalGreedyMatched = 0;
		int o_totalExpired = 0;
		int o_totalSeen = 0;
		
		// Simulate the entry/departure of vertices in the competitive market
		currTime = 0.0;
		while(true) {

			// Deal with critical vertices between the current time and the next entry time (inclusive)
			double nextEntryTime = entryTimes.isEmpty() ? Double.MAX_VALUE : entryTimes.peek();
			while(
					!verticesByExitTime.isEmpty() && 
					verticesByExitTime.peek().getLeft() <= nextEntryTime  // this vertex will expire before next vertex enters
					) {

				// Get the vertex that is going critical now (and make sure it hasn't been matched already)
				Vertex critVertex = verticesByExitTime.poll().getRight();
				if(!pool.vertexSet().contains(critVertex)) { continue; }
				
				// Get the set of possible vertices to match with this vertex
				Pool patientPool = getPatientSubPool(pool, marketVertexMap);
				Set<Vertex> verticesToRemove = new HashSet<Vertex>();
				
				if(!patientPool.containsVertex(critVertex)) {
					// The vertex is in the Greedy market only; no chance to match, it expires (remove from full pool)
					verticesToRemove.add(critVertex);
					o_totalExpired += 1;
				} else {
					// If the vertex is in the patient or competitive markets, try to match it with the patient or competitive pool
					// --> remove matched pairs (success) or just remove it (failure)
					Cycle matchedCycle = matchingStrategy.match(critVertex, patientPool);
					if(null == matchedCycle) {
						// No cycle found for this vertex; it expires unmatched
						verticesToRemove.add(critVertex);
						o_totalExpired += 1;
					} else {
						Set<Vertex> matchedVertices = Cycle.getConstituentVertices(matchedCycle, pool);
						o_totalMatched += matchedVertices.size();
						o_totalPatientMatched += matchedVertices.size();
						verticesToRemove.addAll(matchedVertices);
					}
				}
				
				for(Vertex v : verticesToRemove) {
					pool.removeVertex(v);
				}
				
			}  // end of expiring vertices matching loop

			
			// If there are no new vertices left to enter the pool, and we are done
			// matching all the vertices in the pool now (while loop above), break
			if(entryTimes.isEmpty()) { 
				break; 
			}

			// How many vertices enter at the next time point?  (probably just one, sometimes more)
			currTime = entryTimes.pop(); int newVertCount = 1;
			while(!entryTimes.isEmpty() && entryTimes.peek() == currTime) {
				entryTimes.pop(); newVertCount++;
			}
			o_totalSeen += newVertCount;

			// Add those new vertices to the full pool, and track their exit characteristics/market entries
			Set<Vertex> newVertices = poolGen.addVerticesToPool(pool, newVertCount, 0);
			for(Vertex newVertex : newVertices) {
				// Track when this new vertex will leave the pool, and which market it will enter
				verticesByExitTime.add(new Pair<Double, Vertex>(exitTimes.pop(), newVertex));
				marketVertexMap.get( marketChoices.pop() ).add(newVertex);
			}

			// For any new vertices that entered into Greedy or Competitive, try matching now
			for(Vertex newVertex : newVertices) {
				
				// Get the set of vertices that can see the greedy pool
				Pool greedyPool = this.getGreedySubPool(pool, marketVertexMap);
				if(!greedyPool.vertexSet().contains(newVertex)) { continue; }
				
				// Find a match for this Greedy/Competitive vertex in the Greedy pool
				Cycle matchedCycle = matchingStrategy.match(newVertex, greedyPool);
				if(null == matchedCycle) {
					// Do nothing; the vertex was unmatched, so it sticks around until it
					// is matched through some other vertex or until it goes critical and dies
				} else {
					// Matched!  Remove all vertices in matched cycle from the main pool
					Set<Vertex> matchedVerts = Cycle.getConstituentVertices(matchedCycle, pool);
					o_totalMatched += matchedVerts.size();
					o_totalGreedyMatched += matchedVerts.size();
					for(Vertex v : matchedVerts) {
						pool.removeVertex(v);
					}
				}
			}

		}
		
		logger.info("Done with run(" + timeLimit + "):" + 
		"\n * Total entered:     " + o_totalSeen +  
		"\n * Matched (overall): " + o_totalMatched + 
		"\n * Matched (greedy):  " + o_totalGreedyMatched + 
		"\n * Matched (patient): " + o_totalPatientMatched + 
		"\n * Expired:           " + o_totalExpired);
		
		data.setTotalVerticesSeen(o_totalSeen);
		data.setTotalVerticesMatched(o_totalMatched);
		data.setTotalVerticesMatchedByGreedy(o_totalGreedyMatched);
		data.setTotalVerticesMatchedByPatient(o_totalPatientMatched);
		data.setTotalVerticesExpired(o_totalExpired);
		
		return data;
	}

	
	
	/*
	 * Returns the subpool of the full pool that contains vertices that can be 
	 * matched in the greedy market (so COMPETITIVE and GREEDY, but not PATIENT)
	 */
	private Pool getGreedySubPool(Pool fullPool, Map<VertexMarketChoice, Set<Vertex>> marketVertexMap) {
		return getMarketSubPool(fullPool, marketVertexMap, new HashSet<VertexMarketChoice>(Arrays.asList(
				VertexMarketChoice.GREEDY,
				VertexMarketChoice.COMPETITIVE
				)));
	}
	
	/*
	 * Returns the subpool of the full pool that contains vertices that can be 
	 * matched in the patient market (so COMPETITIVE and PATIENT, but not GREEDY)
	 */
	private Pool getPatientSubPool(Pool fullPool, Map<VertexMarketChoice, Set<Vertex>> marketVertexMap) {
		return getMarketSubPool(fullPool, marketVertexMap, new HashSet<VertexMarketChoice>(Arrays.asList(
				VertexMarketChoice.PATIENT,
				VertexMarketChoice.COMPETITIVE
				)));
	}
	
	/**
	 * Returns the subpool of the full pool that contains vertices who entered
	 * any of the markets in marketChoices
	 * @param fullPool full set of vertices
	 * @param marketVertexMap mapping of market types to the vertices in them
	 * @param marketChoices set of market types to include in the full pool
	 * @return a subpool of Pool that contains only vertices who have
	 * 		   VertexMarketChoice in the set marketChoices
	 */
	private Pool getMarketSubPool(Pool fullPool, Map<VertexMarketChoice, Set<Vertex>> marketVertexMap, Set<VertexMarketChoice> marketChoices) {
		
		// Get the set of vertices that are in at least one of the available market types
		Set<Vertex> includedVertices = new HashSet<Vertex>();
		for(VertexMarketChoice marketChoice : marketChoices) {
			includedVertices.addAll(marketVertexMap.get(marketChoice));
		}
		
		// Take the subpool of the full pool consisting of only those vertices 
		return fullPool.makeSubPool(includedVertices);
	}
}
