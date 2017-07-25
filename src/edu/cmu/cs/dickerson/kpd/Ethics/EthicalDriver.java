package edu.cmu.cs.dickerson.kpd.Ethics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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
import edu.cmu.cs.dickerson.kpd.io.EthicalBloodOutput;
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
	static int days = 7;	//Days in a week
	static final int CHAIN_CAP = 4;
	static final int CYCLE_CAP = 3;
	static final int EXPECTED_PAIRS = 2;
	static final int EXPECTED_ALTRUISTS = 0;
	static final int ITERATIONS = 365;
	static final int NUM_RUNS = 1;
	static final double DEATH = 0.000580725433182381168050643691;
	static final double PATIENCE = 0.02284;
	static final double RENEGE = .5;

	// Toggle printing debugging info to console
	static final boolean DEBUG = false;

	/*
	 * For each run in NUM_RUNS, creates 2 simulations of ITERATIONS days. The "STANDARD"
	 * algorithm gives all patients weight 1, while the "SPECIAL" algorithm gives patients
	 * weights defined by their "ethical" characteristics.
	 */
	public static void runSimulation() {

		// Set which weights version used for "special" weights (versions stored in EthicalVertexPair)
		final int WEIGHTS_VERSION = 1;

		long startTime = System.currentTimeMillis();

		// Store output for csv export
		String path = "ethical_" + System.currentTimeMillis() + ".csv";
		EthicalOutput out = null;
		try {
			out = new EthicalOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		for (int n = 0; n < NUM_RUNS; n++) {

			if (DEBUG) { System.out.println("\n****TEST "+n+"****"); }

			long seed = 12345L + n;
			long rFailureSeed = seed + 1L;
			long rEntranceSeed = seed + 2L;
			long rDepartureSeed = seed + 3L;
			long rArrivalSeedM = seed + 4L;
			long rArrivalSeedA = seed + 6L;

			boolean[] wgt = {false, true};
			for (boolean useSpecialWeights : wgt) {

				String weightType = (useSpecialWeights) ? "SPECIAL" : "STANDARD";

				if (DEBUG) { System.out.println("\n-------------\nRUNNING WITH " + weightType + " WEIGHTS.\n"); }
				
				if (useSpecialWeights) {
					out.set(Col.VERSION, WEIGHTS_VERSION);
				}
				else {
					// STANDARD is weights version 0
					out.set(Col.VERSION, 0);
				}

				Random rFailure = new Random(rFailureSeed);
				Random rEntrance = new Random(rEntranceSeed);
				Random rDeparture = new Random(rDepartureSeed);
				Random rArrivalM = new Random(rArrivalSeedM);
				Random rArrivalA = new Random(rArrivalSeedA);

				EthicalPoolGenerator poolGen = new EthicalPoolGenerator(rEntrance, WEIGHTS_VERSION);
				ExponentialArrivalDistribution m = new ExponentialArrivalDistribution(1.0/EXPECTED_PAIRS, rArrivalM);
				ExponentialArrivalDistribution a = new ExponentialArrivalDistribution(1.0/EXPECTED_ALTRUISTS, rArrivalA);
				Pool pool = new Pool(Edge.class);									
				ArrayList<Cycle> matches = new ArrayList<Cycle>();			

				// Record parameters for this set of runs
				out.set(Col.SEED, seed);
				out.set(Col.NUM_ITERATIONS, ITERATIONS);
				out.set(Col.ARRIVAL_PAIRS, EXPECTED_PAIRS);
				out.set(Col.ARRIVAL_ALTS, EXPECTED_ALTRUISTS);
				out.set(Col.ALG_TYPE, weightType);

				// Accumulators for vertices seen, matched over an entire set of runs
				int totalPairsSeen = 0;
				int totalAltsSeen = 0;
				int totalPairsDeparted = 0;
				ArrayList<Cycle> totalMatches = new ArrayList<Cycle>();		
				Map<Integer, Integer> totalTypeSeenMap = new HashMap<Integer, Integer>();
				for(int vertType=1; vertType<=8; vertType++) {
					totalTypeSeenMap.put(vertType, 0);
				}

				for (int i = 0; i < ITERATIONS; i++) {
					// Add new vertices to the pool
					int pairs = m.draw().intValue();
					int alts = a.draw().intValue();
					// Count new vertices
					totalPairsSeen += pairs;
					totalAltsSeen += alts;
					if (DEBUG) { System.out.println("ITERATION: "+i+"\t"+pairs+" new pairs and "+alts+" new altruist(s)"); }

					// Add vertices with EthicalVertexPair weights for edge weights
					Set<Vertex> addedVertices = null;
					if (useSpecialWeights) {
						addedVertices = poolGen.addSpecialVerticesToPool(pool, pairs, alts);
					} 
					// ...or add vertices with edge weights 1
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

					// Remove all pairs where the patient dies
					ArrayList<VertexPair> rm = new ArrayList<VertexPair>();
					for (VertexPair v : pool.getPairs()) {
						if (rDeparture.nextDouble() <= DEATH) {
							totalPairsDeparted++;
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

					// Remove edges matched in previous iteration
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
							// We matched a chain, now we have to make the last donor a bridge donor with some probability
							if (Cycle.isAChain(ci, pool)) {
								if (DEBUG) { System.out.println("Matched a chain."); }
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
								if (DEBUG) {
									System.out.println("Matched cycle: ");
									for (Vertex v : Cycle.getConstituentVertices(ci, pool)) {
										System.out.println(v.getID() + "~");
									}
									System.out.println("");
								}
								// Remove all vertices in the match from the pool
								pool.removeAllVertices(Cycle.getConstituentVertices(ci, pool));
							}
							// Remove this match from our current set of matchings
							iter.remove();
						}
					}

					if (DEBUG) { printPool(pool, false); }

					// Match the vertex pairs in the pool
					CycleGenerator cg = new CycleGenerator(pool);
					List<Cycle> cycles = cg.generateCyclesAndChains(CYCLE_CAP, 0, true);
					CycleMembership membership = new CycleMembership(pool, cycles);

					try{
						EthicalCPLEXSolver s = new EthicalCPLEXSolver(pool, cycles, membership);

						Solution sol = null;
						// "SPECIAL" solution
						if (useSpecialWeights) {
							// Store all weights in specialWeights
							Map<Edge, Double> specialWeights = new HashMap<Edge, Double>();
							for (Edge e : pool.getNonDummyEdgeSet()) {
								specialWeights.put(e, pool.getEdgeWeight(e));
							}
							// Set all weights to 1
							setBinaryEdgeWeights(pool);
							// Solve w/no cardinality constraint
							Solution intermediate = s.solve(0);
							// Set minimum cardinality
							double min_cardinality = intermediate.getObjectiveValue();
							// Restore original weights
							setSpecialEdgeWeights(pool, specialWeights);
							// Solve with cardinality constraint
							sol = s.solve(min_cardinality);	
						}
						// "STANDARD" solution
						else {
							// Solve w/no cardinality constraint
							sol = s.solve(0);
						}
						// Record matches
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
					if (DEBUG) { System.out.println("Time elapsed: " + totalTime); }

				} // end of iterations loop

				if (DEBUG) { System.out.println("\nSolved with "+weightType+" weights. Vertices matched:"); }

				// Count vertices of each type matched
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
				
				//Close vertices-generated output file in EthicalPoolGenerator
				poolGen.close(seed);

				if (DEBUG) { System.out.println(Arrays.asList(profileCounts)); }

				// Record results for this entire run
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
				// Write the row of data
				try {
					out.record();
				} catch(IOException e) {
					IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
					e.printStackTrace();
					System.exit(-1);
				}
			} // end of special/standard weights loop
		} // end of runs loop

		// Experiment complete
		long time = (System.currentTimeMillis()-startTime)/60000;
		System.out.println("\n\nDone running "+NUM_RUNS+" tests of "+ITERATIONS+" iterations. Total time: "+time+" minutes.\n\n\n");

		// clean up CSV writer
		if(null != out) {
			try {
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * For each run in NUM_RUNS, creates a simulation of ITERATIONS days using each
	 * weights version passed in weightsToTest. The weights versions are enumerated 
	 * in EthicalVertexPair class.
	 */
	public static void runSimulationWithWeights(ArrayList<Integer> weightsToTest) {

		long startTime = System.currentTimeMillis();

		// Store output for csv export
		String path = "ethical_" + System.currentTimeMillis() + ".csv";
		EthicalOutput out = null;
		try {
			out = new EthicalOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}
		
		// Intialize file to write pool size stats to
		PrintWriter poolOut = null;
		try {
			poolOut = new PrintWriter("pool_size_" + System.currentTimeMillis() + ".csv");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		poolOut.println("iteration,size,run,weights_version\n");

		for (int n = 0; n < NUM_RUNS; n++) {

			if (DEBUG) { System.out.println("\n****TEST "+n+"****"); }

			long seed = 12345L + n;
			long rFailureSeed = seed + 1L;
			long rEntranceSeed = seed + 2L;
			long rDepartureSeed = seed + 3L;
			long rArrivalSeedM = seed + 4L;
			long rArrivalSeedA = seed + 6L;
			

			for (Integer weightsVersion : weightsToTest) {

				//if (DEBUG) { 
					System.out.println("\n-------------\nTesting weights version " + weightsVersion + ".\n"); //}

				Random rFailure = new Random(rFailureSeed);
				Random rEntrance = new Random(rEntranceSeed);
				Random rDeparture = new Random(rDepartureSeed);
				Random rArrivalM = new Random(rArrivalSeedM);
				Random rArrivalA = new Random(rArrivalSeedA);

				EthicalPoolGenerator poolGen = new EthicalPoolGenerator(rEntrance, weightsVersion);
				ExponentialArrivalDistribution m = new ExponentialArrivalDistribution(1.0/EXPECTED_PAIRS, rArrivalM);
				ExponentialArrivalDistribution a = new ExponentialArrivalDistribution(1.0/EXPECTED_ALTRUISTS, rArrivalA);
				Pool pool = new Pool(Edge.class);									
				ArrayList<Cycle> matches = new ArrayList<Cycle>();			

				// Record parameters for this set of runs
				out.set(Col.VERSION, weightsVersion);
				out.set(Col.SEED, seed);
				out.set(Col.NUM_ITERATIONS, ITERATIONS);
				out.set(Col.ARRIVAL_PAIRS, EXPECTED_PAIRS);
				out.set(Col.ARRIVAL_ALTS, EXPECTED_ALTRUISTS);
				if (weightsVersion == 0) {
					// STANDARD is weights version 0
					out.set(Col.ALG_TYPE, "STANDARD");
				}
				else {
					out.set(Col.ALG_TYPE, "SPECIAL");
				}
				
				// Accumulators for vertices seen, matched over an entire set of runs
				int totalPairsSeen = 0;
				int totalAltsSeen = 0;
				int totalPairsDeparted = 0;
				ArrayList<Cycle> totalMatches = new ArrayList<Cycle>();		
				Map<Integer, Integer> totalTypeSeenMap = new HashMap<Integer, Integer>();
				for(int vertType=1; vertType<=8; vertType++) {
					totalTypeSeenMap.put(vertType, 0);
				}
				


				for (int i = 0; i < ITERATIONS; i++) {
					// Add new vertices to the pool
					int pairs = m.draw().intValue();
					int alts = a.draw().intValue();
					// Count new vertices
					totalPairsSeen += pairs;
					totalAltsSeen += alts;
					if (DEBUG) { System.out.println("ITERATION: "+i+"\t"+pairs+" new pairs and "+alts+" new altruist(s)"); }

					// Add vertices with EthicalVertexPair weights for edge weights
					Set<Vertex> addedVertices = null;
					addedVertices = poolGen.addSpecialVerticesToPool(pool, pairs, alts);

					// Keep track of how many of each ethical type of vertex arrive in the pool
					for(Vertex v : addedVertices) {
						if(!v.isAltruist()) {
							EthicalVertexPair eV = (EthicalVertexPair) v;
							totalTypeSeenMap.put(eV.getProfileID(), totalTypeSeenMap.get(eV.getProfileID())+1);
						}
					}

					// Remove all pairs where the patient dies
					ArrayList<VertexPair> rm = new ArrayList<VertexPair>();
					for (VertexPair v : pool.getPairs()) {
						if (rDeparture.nextDouble() <= DEATH) {
							totalPairsDeparted++;
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

					// Remove edges matched in previous iteration
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
							// We matched a chain, now we have to make the last donor a bridge donor with some probability
							if (Cycle.isAChain(ci, pool)) {
								if (DEBUG) { System.out.println("Matched a chain."); }
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
								if (DEBUG) {
									System.out.println("Matched cycle: ");
									for (Vertex v : Cycle.getConstituentVertices(ci, pool)) {
										System.out.println(v.getID() + "~");
									}
									System.out.println("");
								}
								// Remove all vertices in the match from the pool
								pool.removeAllVertices(Cycle.getConstituentVertices(ci, pool));
							}
							// Remove this match from our current set of matchings
							iter.remove();
						}
					}

					if (DEBUG) { printPool(pool, false); }

					// Match the vertex pairs in the pool
					CycleGenerator cg = new CycleGenerator(pool);
					List<Cycle> cycles = cg.generateCyclesAndChains(CYCLE_CAP, 0, true);
					CycleMembership membership = new CycleMembership(pool, cycles);

					try{
						EthicalCPLEXSolver s = new EthicalCPLEXSolver(pool, cycles, membership);

						// Store all weights in specialWeights
						Map<Edge, Double> specialWeights = new HashMap<Edge, Double>();
						for (Edge e : pool.getNonDummyEdgeSet()) {
							specialWeights.put(e, pool.getEdgeWeight(e));
						}
						// Set all weights to 1
						setBinaryEdgeWeights(pool);
						// Solve w/no cardinality constraint
						Solution intermediate = s.solve(0);
						// Set minimum cardinality
						double min_cardinality = intermediate.getObjectiveValue();
						// Restore original weights
						setSpecialEdgeWeights(pool, specialWeights);

						// Solve with cardinality constraint
						Solution sol = s.solve(min_cardinality);	

						// Record matches
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
					if (DEBUG) { System.out.println("Time elapsed: " + totalTime); }

					//Track pool size
					int size = pool.getNumPairs() + pool.getNumAltruists();
					poolOut.write(i+","+size+","+n+","+weightsVersion+"\n");
					
				} // end of iterations loop
				
				//TODO: vertexOut.close();

				if (DEBUG) { System.out.println("\nSolved with weights version"+weightsVersion+". Vertices matched:"); }

				// Count vertices of each type matched
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
				
				//Close vertices-generated output file in EthicalPoolGenerator
				poolGen.close(seed);

				if (DEBUG) { System.out.println(Arrays.asList(profileCounts)); }

				// Record results for this entire run
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
				// Write the row of data
				try {
					out.record();
				} catch(IOException e) {
					IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
					e.printStackTrace();
					System.exit(-1);
				}
			} // end of weightsVersions loop
		} // end of runs loop

		// Experiment complete
		long time = (System.currentTimeMillis()-startTime)/60000;
		System.out.println("\n\nDone running "+NUM_RUNS+" tests of "+ITERATIONS+" iterations for "+weightsToTest.size()+
				" different weights versions. Total time: "+time+" minutes.\n\n\n");

		// close pool file
		poolOut.close();
		
		// clean up CSV writer
		if(null != out) {
			try {
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * Performs same function as runSimulationWithWeights, except also tracks and
	 * outputs blood types of patients matched using EthicalBloodOutput
	 */
	public static void runSimWeightsBlood(ArrayList<Integer> weightsToTest) throws Exception {

		long startTime = System.currentTimeMillis();

		// Store output for csv export
		String path = "ethical_" + System.currentTimeMillis() + ".csv";
		EthicalBloodOutput out = null;
		try {
			out = new EthicalBloodOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}
		
		// Intialize file to write pool size stats to
		PrintWriter poolOut = null;
		try {
			poolOut = new PrintWriter("pool_size_" + System.currentTimeMillis() + ".csv");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		poolOut.println("iteration,size,run,weights_version\n");

		//Start of runs loop (each run is a new test)
		for (int n = 0; n < NUM_RUNS; n++) {

			if (DEBUG) { System.out.println("\n****TEST "+n+"****"); }

			long seed = 12345L + n;
			long rFailureSeed = seed + 1L;
			long rEntranceSeed = seed + 2L;
			long rDepartureSeed = seed + 3L;
			long rArrivalSeedM = seed + 4L;
			long rArrivalSeedA = seed + 6L;
			
			for (Integer weightsVersion : weightsToTest) {

				if (DEBUG) { System.out.println("\n-------------\nTesting weights version " + weightsVersion + ".\n"); }

				Random rFailure = new Random(rFailureSeed);
				Random rEntrance = new Random(rEntranceSeed);
				Random rDeparture = new Random(rDepartureSeed);
				Random rArrivalM = new Random(rArrivalSeedM);
				Random rArrivalA = new Random(rArrivalSeedA);

				EthicalPoolGenerator poolGen = new EthicalPoolGenerator(rEntrance, weightsVersion);
				ExponentialArrivalDistribution m = new ExponentialArrivalDistribution(1.0/EXPECTED_PAIRS, rArrivalM);
				ExponentialArrivalDistribution a = new ExponentialArrivalDistribution(1.0/EXPECTED_ALTRUISTS, rArrivalA);
				Pool pool = new Pool(Edge.class);									
				ArrayList<Cycle> matches = new ArrayList<Cycle>();			

				// Record parameters for this set of runs
				out.set(EthicalBloodOutput.Col.VERSION, weightsVersion);
				out.set(EthicalBloodOutput.Col.SEED, seed);
				out.set(EthicalBloodOutput.Col.NUM_ITERATIONS, ITERATIONS);
				out.set(EthicalBloodOutput.Col.ARRIVAL_PAIRS, EXPECTED_PAIRS);
				out.set(EthicalBloodOutput.Col.ARRIVAL_ALTS, EXPECTED_ALTRUISTS);
				if (weightsVersion == 0) {
					// STANDARD is weights version 0
					out.set(EthicalBloodOutput.Col.ALG_TYPE, "STANDARD");
				}
				else {
					out.set(EthicalBloodOutput.Col.ALG_TYPE, "SPECIAL");
				}
				
				// Accumulators for vertices seen & matched over an entire run
				int totalPairsSeen = 0;
				int totalAltsSeen = 0;
				int totalPairsDeparted = 0;
				
				// Store vertices of each type SEEN
				String[] vertTypes = {"1_O_O", "1_A_O", "1_B_O", "1_AB_O", "1_O_A", "1_A_A", "1_B_A", "1_AB_A", "1_O_B", "1_A_B", "1_B_B", "1_AB_B", "1_O_AB", "1_A_AB", "1_B_AB", "1_AB_AB", "2_O_O", "2_A_O", "2_B_O", "2_AB_O", "2_O_A", "2_A_A", "2_B_A", "2_AB_A", "2_O_B", "2_A_B", "2_B_B", "2_AB_B", "2_O_AB", "2_A_AB", "2_B_AB", "2_AB_AB", "3_O_O", "3_A_O", "3_B_O", "3_AB_O", "3_O_A", "3_A_A", "3_B_A", "3_AB_A", "3_O_B", "3_A_B", "3_B_B", "3_AB_B", "3_O_AB", "3_A_AB", "3_B_AB", "3_AB_AB", "4_O_O", "4_A_O", "4_B_O", "4_AB_O", "4_O_A", "4_A_A", "4_B_A", "4_AB_A", "4_O_B", "4_A_B", "4_B_B", "4_AB_B", "4_O_AB", "4_A_AB", "4_B_AB", "4_AB_AB", "5_O_O", "5_A_O", "5_B_O", "5_AB_O", "5_O_A", "5_A_A", "5_B_A", "5_AB_A", "5_O_B", "5_A_B", "5_B_B", "5_AB_B", "5_O_AB", "5_A_AB", "5_B_AB", "5_AB_AB", "6_O_O", "6_A_O", "6_B_O", "6_AB_O", "6_O_A", "6_A_A", "6_B_A", "6_AB_A", "6_O_B", "6_A_B", "6_B_B", "6_AB_B", "6_O_AB", "6_A_AB", "6_B_AB", "6_AB_AB", "7_O_O", "7_A_O", "7_B_O", "7_AB_O", "7_O_A", "7_A_A", "7_B_A", "7_AB_A", "7_O_B", "7_A_B", "7_B_B", "7_AB_B", "7_O_AB", "7_A_AB", "7_B_AB", "7_AB_AB", "8_O_O", "8_A_O", "8_B_O", "8_AB_O", "8_O_A", "8_A_A", "8_B_A", "8_AB_A", "8_O_B", "8_A_B", "8_B_B", "8_AB_B", "8_O_AB", "8_A_AB", "8_B_AB", "8_AB_AB"};
				Map<String, Integer> totalTypeSeenMap = new HashMap<String, Integer>();
				for(String vertType : vertTypes) {
					totalTypeSeenMap.put(vertType, 0);		//Initialize each count to 0
				}
				
				// Store vertices of each type MATCHED
				Map<String, Integer> totalTypeMatchedMap = new HashMap<String, Integer>();
				for(String vertType : vertTypes) {
					totalTypeMatchedMap.put(vertType, 0);	//Initialize each count to 0
				}
				//Store all matched cycles (for later inclusion in totalTypeMatchedMap)
				ArrayList<Cycle> totalMatches = new ArrayList<Cycle>();		
				
				for (int i = 0; i < ITERATIONS; i++) {
					// Select num of new vertices to add to the pool
					int pairs = m.draw().intValue();
					int alts = a.draw().intValue();
					// Count new vertices
					totalPairsSeen += pairs;
					totalAltsSeen += alts;
					if (DEBUG) { System.out.println("ITERATION: "+i+"\t"+pairs+" new pairs and "+alts+" new altruist(s)"); }

					// Add vertices with EthicalVertexPair weights for edge weights
					Set<Vertex> addedVertices = null;
					addedVertices = poolGen.addSpecialVerticesToPool(pool, pairs, alts);

					// Keep track of how many of each type of vertex (profile _ patient blood _ donor blood) arrive in the pool
					for(Vertex v : addedVertices) {
						if(!v.isAltruist()) {
							EthicalVertexPair eV = (EthicalVertexPair) v;
							totalTypeSeenMap.put(eV.getBloodID(), totalTypeSeenMap.get(eV.getBloodID())+1);
						}
					}

					// Remove all pairs where the patient dies
					ArrayList<VertexPair> rm = new ArrayList<VertexPair>();
					for (VertexPair v : pool.getPairs()) {
						if (rDeparture.nextDouble() <= DEATH) {
							totalPairsDeparted++;
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

					// Remove edges matched in previous iteration
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
							// We matched a chain, now we have to make the last donor a bridge donor with some probability
							if (Cycle.isAChain(ci, pool)) {
								if (DEBUG) { System.out.println("Matched a chain."); }
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
								if (DEBUG) {
									System.out.println("Matched cycle: ");
									for (Vertex v : Cycle.getConstituentVertices(ci, pool)) {
										System.out.println(v.getID() + "~");
									}
									System.out.println("");
								}
								// Remove all vertices in the match from the pool
								pool.removeAllVertices(Cycle.getConstituentVertices(ci, pool));
							}
							// Remove this match from our current set of matchings
							iter.remove();
						}
					}

					if (DEBUG) { printPool(pool, false); }

					// Match the vertex pairs in the pool
					CycleGenerator cg = new CycleGenerator(pool);
					List<Cycle> cycles = cg.generateCyclesAndChains(CYCLE_CAP, 0, true);
					CycleMembership membership = new CycleMembership(pool, cycles);

					try{
						EthicalCPLEXSolver s = new EthicalCPLEXSolver(pool, cycles, membership);

						// Store all weights in specialWeights
						Map<Edge, Double> specialWeights = new HashMap<Edge, Double>();
						for (Edge e : pool.getNonDummyEdgeSet()) {
							specialWeights.put(e, pool.getEdgeWeight(e));
						}
						// Set all weights to 1
						setBinaryEdgeWeights(pool);
						// Solve w/no cardinality constraint
						Solution intermediate = s.solve(0);
						// Set minimum cardinality
						double min_cardinality = intermediate.getObjectiveValue();
						// Restore original weights
						setSpecialEdgeWeights(pool, specialWeights);

						// Solve with cardinality constraint
						Solution sol = s.solve(min_cardinality);	

						// Record matches
						for(Cycle c : sol.getMatching()){
							matches.add(c);
							totalMatches.add(c);	// Store cycles (for later inclusion in totalTypeMatchedMap)	
						}
					}
					catch(SolverException e){
						e.printStackTrace();
						System.exit(-1);
					}

					long endTime = System.currentTimeMillis();
					long totalTime = endTime-startTime;
					if (DEBUG) { System.out.println("Time elapsed: " + totalTime); }

					//Track pool size
					int size = pool.getNumPairs() + pool.getNumAltruists();
					poolOut.write(i+","+size+","+n+","+weightsVersion+"\n");
					
				} // end of iterations loop

				if (DEBUG) { System.out.println("\nSolved with weights version"+weightsVersion+". Vertices matched:"); }
				
				// Count all vertices from matched cycles and store in totalTypeMatchedMap
				for (Cycle c : totalMatches) {
					for (Vertex v : Cycle.getConstituentVertices(c, pool)) {
						String bloodID = ((EthicalVertexPair) v).getBloodID();
						totalTypeMatchedMap.put(bloodID, totalTypeMatchedMap.get(bloodID)+1);
					}
				}
				
				//Close vertices-generated output file in EthicalPoolGenerator
				poolGen.close(seed);

				if (DEBUG) { System.out.println(Arrays.asList(totalTypeMatchedMap)); }

				// Record results for this entire run
				out.set(EthicalBloodOutput.Col.SEEN_PAIRS, totalPairsSeen);
				out.set(EthicalBloodOutput.Col.SEEN_ALTS, totalAltsSeen);
				out.set(EthicalBloodOutput.Col.DEPARTED_PAIRS, totalPairsDeparted);

				// Check for MATCH > SEEN bug
				for(String vertType : totalTypeMatchedMap.keySet()) {
					int match = totalTypeMatchedMap.get(vertType);
					int seen = totalTypeSeenMap.get(vertType);
					if (match > seen) {
						System.out.println("\n\t\tERROR: MORE VERTICES MATCHED THEN SEEN.");
						System.out.println("\t\tType: "+vertType+"\tSeen: "+seen+"\tMatch: "+match);
						System.out.println("\t\tWeights Version: "+weightsVersion+"\tSeed: "+seed+"\tIterations: "+ITERATIONS);
						throw new java.lang.Exception("More vertices matched then seen. See details above.");
					}
				}
				
				// Write totalTypeMatchedMap and totalTypeSeenMap to file
				for(String vertType : totalTypeMatchedMap.keySet()) {
					out.set(EthicalBloodOutput.Col.valueOf("MATCH_"+vertType), totalTypeMatchedMap.get(vertType));
				}
				for(String vertType : totalTypeSeenMap.keySet()) {
					out.set(EthicalBloodOutput.Col.valueOf("SEEN_"+vertType), totalTypeSeenMap.get(vertType));
				}

				// Keep me at the bottom of one run
				// Write the row of data
				try {
					out.record();
				} catch(IOException e) {
					IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
					e.printStackTrace();
					System.exit(-1);
				}
			} // end of weightsVersions loop
		} // end of runs loop

		// Experiment complete
		long time = (System.currentTimeMillis()-startTime)/60000;
		System.out.println("\n\nDone running "+NUM_RUNS+" tests of "+ITERATIONS+" iterations for "+weightsToTest.size()+
				" different weights versions. Total time: "+time+" minutes.\n\n\n");

		// close pool file
		poolOut.close();
		
		// clean up CSV writer
		if(null != out) {
			try {
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}


	// Helper method to print current state of pool
	public static void printPool(Pool pool, boolean printAllEdges) {
		System.out.println("\nPrinting pool.");
		System.out.println("pairs in pool: "+pool.getNumPairs());
		System.out.println("altruists in pool: "+pool.getNumAltruists());
		System.out.println("edges in pool: "+pool.getNumNonDummyEdges());
		System.out.println("Vertices:");
		for (VertexPair v : pool.getPairs()) {
			System.out.println(v+"~");
		}
		if (printAllEdges) {
			for (Edge e : pool.getNonDummyEdgeSet()) {
				System.out.println("edge "+e.toString()+" - w: "+pool.getEdgeWeight(e));
			}
		}
		System.out.println("");
	}

	// Helper method to set all edge weights to 1.0
	public static void setBinaryEdgeWeights(Pool pool) {
		for (Edge e : pool.getNonDummyEdgeSet()) {
			pool.setEdgeWeight(e, 1.0);
		}
	}

	// Helper method to set all edge weights to stored values 
	public static void setSpecialEdgeWeights(Pool pool, Map<Edge, Double> specialWeights) {
		for (Edge e : pool.getNonDummyEdgeSet()) {
			pool.setEdgeWeight(e, specialWeights.get(e));
		}
	}

	public static void main(String[] args) throws Exception {
		//runSimulation();
		ArrayList<Integer> weightsToTest = new ArrayList<Integer>(Arrays.asList(1));
		runSimWeightsBlood(weightsToTest);
	}

}