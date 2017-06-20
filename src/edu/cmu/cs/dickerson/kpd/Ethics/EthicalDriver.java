package edu.cmu.cs.dickerson.kpd.Ethics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.dynamic.arrivals.ExponentialArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.io.EthicalOutput;
import edu.cmu.cs.dickerson.kpd.io.EthicalOutput.Col;
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
	static final int ITERATIONS = 10;
	static final int NUM_RUNS = 5;
	static final double DEATH = 0.000580725433182381168050643691;
	static final double PATIENCE = 0.02284;
	static final double RENEGE = .5;
	
	//Version of weights used
	static final int WEIGHTS_VERSION = 1;

	public static void runSimulation() {
		
		long startTime = System.currentTimeMillis();
		
		// Store output
		String path = "ethical_" + System.currentTimeMillis() + ".csv";
		EthicalOutput out = null;
		try {
			out = new EthicalOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}


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
				
				// Write down parameters for this trajectory
				out.set(Col.VERSION, WEIGHTS_VERSION);
				out.set(Col.SEED, seed);
				out.set(Col.NUM_ITERATIONS, ITERATIONS);
				out.set(Col.ARRIVAL_PAIRS, EXPECTED_PAIRS);
				out.set(Col.ARRIVAL_ALTS, EXPECTED_ALTRUISTS);
				out.set(Col.ALG_TYPE, weightType);
				
				ArrayList<Cycle> totalMatches = new ArrayList<Cycle>();
				
				// Accumulators for vertices seen, matched over an entire trajectory
				int totalPairsSeen = 0;
				int totalAltsSeen = 0;
				int totalPairsDeparted = 0;
				Map<Integer, Integer> totalTypeSeenMap = new HashMap<Integer, Integer>();
				for(int vertType=1; vertType<=8; vertType++) {
					totalTypeSeenMap.put(vertType, 0);
				}
				
				
				for (int i = 0; i < ITERATIONS; i++) {
					// Add new vertices to the pool
					int pairs = m.draw().intValue();
					int alts = a.draw().intValue();
					totalPairsSeen += pairs;
					totalAltsSeen += alts;
					System.out.println("ITERATION: "+i+"\t"+pairs+" new pairs and "+alts+" new altruist(s)");

					//Add vertices with EthicalVertexPair weights for edge weights
					Set<Vertex> addedVertices = null;
					if (useSpecialWeights) {
						addedVertices = poolGen.addSpecialVerticesToPool(pool, pairs, alts);
					} 
					//or add vertices with edge weights 1
					else {
						addedVertices = poolGen.addVerticesToPool(pool, pairs, alts);
					}

					// Keep track of how many of each ethical type of vertex arrive in the pool
					for(Vertex v : addedVertices) {
						if(!v.isAltruist()) {
							EthicalVertexPair eV = (EthicalVertexPair) v;
							totalTypeSeenMap.put(eV.getProfileID(), totalTypeSeenMap.get(eV.getProfileID())+1);
						}
					}
					
					
					
					//TODO: Increment timeInPool for each vertex
					//Is there an efficient way to iterate through all vertices?
					
					// Remove all pairs where the patient dies
					ArrayList<VertexPair> rm = new ArrayList<VertexPair>();
					for (VertexPair v : pool.getPairs()) {
						if (rDeparture.nextDouble() <= DEATH) {
							totalPairsDeparted++;
							Iterator<Cycle> matchIterator = matches.iterator();
							while (matchIterator.hasNext()) {
								Cycle c = matchIterator.next();
								if (Cycle.getConstituentVertices(c, pool).contains(v)) {
									matchIterator.remove();							//Does this remove from matches?
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
								System.out.println("Matched a chain.");
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
								}
								pool.removeAllVertices(trm);
							}
							else{
								System.out.println("Matched cycle: ");
								for (Vertex v : Cycle.getConstituentVertices(ci, pool)) {
									System.out.print(v.getID() + "~");
								}
								System.out.println("");
								// Remove all vertices in the match from the pool
								pool.removeAllVertices(Cycle.getConstituentVertices(ci, pool));
							}
							// Remove this match from our current set of matchings
							iter.remove();
						}
					}

					//printPool(pool, false);
					
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
							sol = s.solve(0);					//TODO: Reset to min_cardinality
						}
						else {
							
							//Solve w/no cardinality constraint
							sol = s.solve(0);
						}

						for(Cycle c : sol.getMatching()){
							matches.add(c);
							totalMatches.add(c);
						}
					}
					catch(SolverException e){
						e.printStackTrace();
						System.exit(-1);
					}
					
					long endTime = System.currentTimeMillis();
					
					long totalTime = endTime-startTime;
					System.out.println("Time elapsed: " + totalTime);
				}
				
				//TODO: Export matches as CSV (Cycles -> Edges -> EthicalVertexPairs -> isYoung, isNonalcoholic, isHealthy?)
				System.out.println("\nSolved with "+weightType+" weights. Vertices matched:");
				Map<Integer, Integer> profileCounts = new HashMap<Integer, Integer>();
				for(int vertType=1; vertType<=8; vertType++) {
					profileCounts.put(vertType, 0);
				}
				for (Cycle c : totalMatches) {
					for (Vertex v : Cycle.getConstituentVertices(c, pool)) {
						Integer profileID = ((EthicalVertexPair) v).getProfileID();
						if (profileCounts.containsKey(profileID)) {
							profileCounts.put(profileID, profileCounts.get(profileID)+1);
						}
						else {
							profileCounts.put(profileID, 1);
						}
					}
				}
				System.out.println(Arrays.asList(profileCounts));
				
				
				// Write down results for this entire run
				out.set(Col.SEEN_PAIRS, totalPairsSeen);
				out.set(Col.SEEN_ALTS, totalAltsSeen);
				out.set(Col.DEPARTED_PAIRS, totalPairsDeparted);
				
				for(Integer vertType : profileCounts.keySet()) {
					out.set(Col.valueOf("MATCHED_TYPE"+vertType), profileCounts.get(vertType));
				}
				for(Integer vertType : totalTypeSeenMap.keySet()) {
					out.set(Col.valueOf("SEEN_TYPE"+vertType), totalTypeSeenMap.get(vertType));
				}
				
				
				// Keep me at the bottom of one run
				// Write the  row of data
				try {
					out.record();
				} catch(IOException e) {
					IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
					e.printStackTrace();
					System.exit(-1);
				}
				
			} // end of false/true use weights
		} // end of outer loop over NUM_ITER
		
		// clean up CSV writer
		if(null != out) {
			try {
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	//Helper method to print current state of pool
	public static void printPool(Pool pool, boolean printAllEdges) {
		System.out.println("\nPrinting pool.");
		System.out.println("pairs in pool: "+pool.getNumPairs());
		System.out.println("altruists in pool: "+pool.getNumAltruists());
		System.out.println("edges in pool: "+pool.getNumNonDummyEdges());
		
		System.out.println("Vertices:");
		for (VertexPair v : pool.getPairs()) {
			System.out.print(v+"~");
		}
		
		if (printAllEdges) {
			for (Edge e : pool.getNonDummyEdgeSet()) {
				System.out.println("edge "+e.toString()+" - w: "+pool.getEdgeWeight(e));
			}
		}
		System.out.println("");
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