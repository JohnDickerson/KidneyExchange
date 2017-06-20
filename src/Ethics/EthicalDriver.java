package Ethics;

import java.util.*;

import edu.cmu.cs.dickerson.kpd.dynamic.arrivals.ExponentialArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.GreedyPackingSolver;
import edu.cmu.cs.dickerson.kpd.solver.approx.CyclesSampleChainsIPPacker;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexAltruist;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SparseUNOSSaidmanPoolGenerator;

/*
 * Class written by Rachel Freedman, Duke University, summer 2017.
 * Used to simulate "ethical" kidney exchanges where societal preferences over
 * recipient attributes (age, alcohol, general health) are taken into account.
 */

public class EthicalDriver {
	// Probabilities generated based on a match frequency of 1 day
	static final int CHAIN_CAP = 4;
	static final int CYCLE_CAP = 3;
	static final int EXPECTED_PAIRS = 15;
	static final int EXPECTED_ALTRUISTS = 1;
	static final int ITERATIONS = 7;
	static final int NUM_RUNS = 1;
	static final double DEATH = 0.000580725433182381168050643691;
	static final double PATIENCE = 0.02284;
	static final double RENEGE = .5;

	public static void runSimulation() {
		
		long startTime = System.currentTimeMillis();
		
		for (int n = 0; n < NUM_RUNS; n++) {
			
			System.out.println("\n****TEST "+n+"****");
			
			long seed = 12345L + n;
			long rFailureSeed = seed + 1L;
			long rEntranceSeed = seed + 2L;
			long rDepartureSeed = seed + 3L;
			long rArrivalSeedM = seed + 4L;
			long rArrivalSeedA = seed + 6L;
			
			boolean[] wgt = {false, true};
			for (boolean useSpecialWeights : wgt) {
				
				Random rFailure = new Random(rFailureSeed);
				Random rEntrance = new Random(rEntranceSeed);
				Random rDeparture = new Random(rDepartureSeed);
				Random rArrivalM = new Random(rArrivalSeedM);
				Random rArrivalA = new Random(rArrivalSeedA);
				
				String weightType = (useSpecialWeights) ? "SPECIAL" : "STANDARD";
				System.out.println("\n--------------------------------------------\nRUNNING WITH " + weightType + " WEIGHTS.\n");
			
				EthicalPoolGenerator poolGen = new EthicalPoolGenerator(rEntrance);
				ExponentialArrivalDistribution m = new ExponentialArrivalDistribution(1.0/EXPECTED_PAIRS, rArrivalM);
				ExponentialArrivalDistribution a = new ExponentialArrivalDistribution(1.0/EXPECTED_ALTRUISTS, rArrivalA);
				Pool pool = new Pool(Edge.class);									
				ArrayList<Cycle> matches = new ArrayList<Cycle>();
				
				int totalSeen = 0;
				int totalMatched = 0;
				int totalFailedMatches = 0;
				int totalDeceased = 0;
				
				for (int i = 0; i < ITERATIONS; i++) {
					// Add new vertices to the pool
					int pairs = m.draw().intValue();
					int alts = a.draw().intValue();
					System.out.println("ITERATION: "+i+"\t"+pairs+" new pairs and "+alts+" new altruist(s)");
					
					//Add vertices with EthicalVertexPair weights for edge weights
					if (useSpecialWeights) {
						if(pairs > 0){
							totalSeen += poolGen.addSpecialVerticesToPool(pool, pairs, alts).size();
						}
					} 
					//or add vertices with edge weights 1
					else {
						if(pairs > 0){
							totalSeen += poolGen.addVerticesToPool(pool, pairs, alts).size();
						}
					}
					
					//TODO: Increment timeInPool for each vertex
					//Is there an efficient way to iterate through all vertices?
					
					// Remove all pairs where the patient dies
					ArrayList<VertexPair> rm = new ArrayList<VertexPair>();
					for (VertexPair v : pool.getPairs()) {
						if (rDeparture.nextDouble() <= DEATH) {
							totalDeceased++;
							Iterator<Cycle> matchIterator = matches.iterator();
							while (matchIterator.hasNext()) {
								Cycle c = matchIterator.next();
								if (Cycle.getConstituentVertices(c, pool).contains(v)) {
									matchIterator.remove();
								}
							}
							rm.add(v);
						}
					}
					for(VertexPair v : rm){
						pool.removeVertex(v);
					}
					// Remove all altruists that run out of patience
					Iterator<VertexAltruist> aiter = pool.getAltruists().iterator();
					ArrayList<VertexAltruist> toRemove = new ArrayList<VertexAltruist>();
					while (aiter.hasNext()) {
						VertexAltruist alt = aiter.next();
						if (rDeparture.nextDouble() <= PATIENCE) {
							toRemove.add(alt);
						}
					}
					pool.removeAllVertices(toRemove);
					
					// Remove edges in matchings
					Iterator<Cycle> iter = matches.iterator();
					while(iter.hasNext()) {
						Cycle ci = iter.next();
						boolean fail = false;
						for (Edge e : ci.getEdges()) {
							if (rFailure.nextDouble() <= e.getFailureProbability()) {
								iter.remove();
								totalFailedMatches++;
								fail = true;
								break;
							}
						}
						if(fail){
							continue;
						}
						//All edges in the Cycle remain, so we have a match!
						else {
							// We matched a chain, now we have to make the last
							// donor a bridge donor with some probability
							if (Cycle.isAChain(ci, pool)) {
								ArrayList<VertexPair> trm = new ArrayList<VertexPair>();
								List<Edge> le = new ArrayList<Edge>();
								for(Edge e : ci.getEdges()){
									le.add(e);
								}
								Collections.reverse(le);
								le.remove(le.size()-1);
								for(Edge e : le){
									// The bridge donor reneged, we stop the chain here
									if (rDeparture.nextDouble() <= RENEGE) {
										trm.add((VertexPair)pool.getEdgeTarget(e));
										break;
									} else {
										VertexPair bridge = (VertexPair)pool.getEdgeTarget(e);
										trm.add(bridge);
										VertexAltruist bridgeDonor = new VertexAltruist(bridge.getID(),
												bridge.getBloodTypeDonor());
										pool.addAltruist(bridgeDonor);
									}
									totalMatched++;
								}
								pool.removeAllVertices(trm);
							}
							else{
								// Remove all vertices in the match from the pool
								totalMatched += Cycle.getConstituentVertices(ci, pool).size();
								pool.removeAllVertices(Cycle.getConstituentVertices(ci, pool));
							}
							// Remove this match from our current set of matchings
							iter.remove();
						}
					}

					// Match the vertex pairs in the pool
					CycleGenerator cg = new CycleGenerator(pool);
					List<Cycle> cycles = cg.generateCyclesAndChains(CYCLE_CAP, 0, true);
					CycleMembership membership = new CycleMembership(pool, cycles);
					
					try{
						EthicalCPLEXSolver s = new EthicalCPLEXSolver(pool, cycles, membership);
						
						Solution sol = null;
						if (useSpecialWeights) {
							//Store all weights in specialWeights
							Map<Edge, Double> specialWeights = new HashMap<Edge, Double>();
							for (Edge e : pool.getNonDummyEdgeSet()) {
								specialWeights.put(e, pool.getEdgeWeight(e));
							}
							//Set all weights to 1
							setBinaryEdgeWeights(pool);
							//Solve w/no cardinality constraint
							Solution intermediate = s.solve(0);
							//Set minimum cardinality
							double min_cardinality = intermediate.getObjectiveValue();
							//Restore original weights
							setSpecialEdgeWeights(pool, specialWeights);
							//Solve with cardinality constraint
							sol = s.solve(min_cardinality);
						}
						else {
							
							//Solve w/no cardinality constraint
							sol = s.solve(0);
						}

						for(Cycle c : sol.getMatching()){
							matches.add(c);
						}
					}
					catch(SolverException e){
						e.printStackTrace();
						System.exit(-1);
					}
					
					System.out.println(totalSeen + " vertices were seen");
					System.out.println(totalMatched + " vertices were matched");
					System.out.println(totalFailedMatches + " matches failed");
					System.out.println(totalDeceased + " patients died");
					
					long endTime = System.currentTimeMillis();
					
					long totalTime = endTime-startTime;
					System.out.println("Time elapsed: " + totalTime);
					
					//TODO: Export matches as CSV (Cycles -> Edges -> EthicalVertexPairs -> isYoung, isNonalcoholic, isHealthy?)
					
				}
			}
		}
	}
	
	public static void setBinaryEdgeWeights(Pool pool) {
		for (Edge e : pool.getNonDummyEdgeSet()) {
			pool.setEdgeWeight(e, 1.0);
		}
	}
	
	public static void setSpecialEdgeWeights(Pool pool, Map<Edge, Double> specialWeights) {
		for (Edge e : pool.getNonDummyEdgeSet()) {
			pool.setEdgeWeight(e, specialWeights.get(e));
		}
	}
	
	public static void main(String[] args) {
		runSimulation();
	}

}