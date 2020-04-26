package edu.cmu.cs.dickerson.kpd.variation;

import edu.cmu.cs.dickerson.kpd.dynamic.arrivals.ExponentialArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.io.VariationOutput.Col;
import edu.cmu.cs.dickerson.kpd.io.VariationOutput;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.*;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/*
 * Class written by Rachel Freedman, UC Berkeley, 2020.
 */

public class VariationDriver {

	// Experimental conditions
	public enum Condition {
		EQUAL_WEIGHTS,			// All edges have weight 1.0
		PATIENT_WEIGHTS,		// Edge weights depend on receiving patient only (BT model)
		DONOR_PATIENT_WEIGHTS	// Edge weights depend on donor and receiving patient (BLP model)
	}

	// Probabilities generated based on a match frequency of 1 day
	static final int CYCLE_CAP = 3;
	static final int EXPECTED_PAIRS = 4;
	static final int ITERATIONS = 365*5;
	static final int NUM_RUNS = 50;
	static final double DEATH = 0.000580725433182381168050643691;
	static final double PATIENCE = 0.02284;
	static final double RENEGE = .5;

	// File Directories
	public static final String INPUT_PATH = "src/edu/cmu/cs/dickerson/kpd/variation/input/";
	public static final String OUTPUT_PATH = "src/edu/cmu/cs/dickerson/kpd/variation/output/";

	// Toggle printing debugging info to console
	static final boolean DEBUG = false;

	public static void runExperiments(){
		String expID = Long.toString(System.currentTimeMillis());
		runSimulationWithEqualWeights(expID);
		runSimulationWithPatientWeights(expID);
		runSimulationWithDonorPatientWeights(expID);
	}

	/*
	 * For each run in NUM_RUNS, creates a simulation of ITERATIONS days.
	 * Break ties (equal-cardinality matchings) using weights based on donor
	 * preference and recipient profile (calculated with BLP model).
	 */
	public static void runSimulationWithDonorPatientWeights(String expId) {
		long startTime = System.currentTimeMillis();

		// Store output for csv export
		String condition = Condition.DONOR_PATIENT_WEIGHTS.toString();
		String path = OUTPUT_PATH + condition + "_" + expId + ".csv";
		VariationOutput out = null;
		try {
			out = new VariationOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		for (int n = 0; n < NUM_RUNS; n++) {

			if (DEBUG) { System.out.println("\n****RUN "+n+"****"); }

			long seed = 100L + n;
			long rFailureSeed = seed + 1L;
			long rEntranceSeed = seed + 2L;
			long rDepartureSeed = seed + 3L;
			long rArrivalSeedM = seed + 4L;

			Random rFailure = new Random(rFailureSeed);
			Random rEntrance = new Random(rEntranceSeed);
			Random rDeparture = new Random(rDepartureSeed);
			Random rArrivalM = new Random(rArrivalSeedM);

			VariationPoolGenerator poolGen = new VariationPoolGenerator(rEntrance);
			ExponentialArrivalDistribution m = new ExponentialArrivalDistribution(1.0/EXPECTED_PAIRS, rArrivalM);
			Pool pool = new Pool(Edge.class);
			ArrayList<Cycle> matches = new ArrayList<Cycle>();

			// Record parameters for this set of runs
			out.set(Col.CONDITION, condition);
			out.set(Col.SEED, seed);
			out.set(Col.NUM_ITERATIONS, ITERATIONS);
			out.set(Col.ARRIVAL_PAIRS, EXPECTED_PAIRS);

			// Accumulators for this run
			int totalPairsSeen = 0;
			int totalPairsDeparted = 0;
			ArrayList<Cycle> totalMatches = new ArrayList<Cycle>();
			Map<Integer, Integer> totalPairsSeenByProfile = new HashMap<Integer, Integer>();
			for(int vertType=1; vertType<=8; vertType++) {
				totalPairsSeenByProfile.put(vertType, 0);
			}

			for (int i = 0; i < ITERATIONS; i++) {
				// Add new vertices to the pool
				int pairs = m.draw().intValue();
				totalPairsSeen += pairs;
				if (DEBUG) { System.out.println("ITERATION: "+i+"\t"+pairs+" new pairs"); }

				// Add vertices with VariationVertexPair weights for edge weights
				Set<Vertex> addedVertices = null;
				addedVertices = poolGen.addVerticesWithDonorPatientEdgeWeights(pool, pairs);

				// Keep track of how many of each patient profile arrive in the pool
				for(Vertex v : addedVertices) {
					VariationVertexPair eV = (VariationVertexPair) v;
					totalPairsSeenByProfile.put(eV.getProfileID(), totalPairsSeenByProfile.get(eV.getProfileID())+1);
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
						if (DEBUG) {
							System.out.println("Matched cycle: ");
							for (Vertex v : Cycle.getConstituentVertices(ci, pool)) {
								System.out.print(v.getID() + "~");
							}
							System.out.println("");
						}
						// Remove all vertices in the match from the pool
						pool.removeAllVertices(Cycle.getConstituentVertices(ci, pool));
						iter.remove();
					}
				}

				if (DEBUG) { printPool(pool, false); }

				// Match the vertex pairs in the pool
				CycleGenerator cg = new CycleGenerator(pool);
				List<Cycle> cycles = cg.generateCyclesAndChains(CYCLE_CAP, 0, true);
				CycleMembership membership = new CycleMembership(pool, cycles);

				try{
					VariationCPLEXSolver s = new VariationCPLEXSolver(pool, cycles, membership);

					// Temporarily store all weights
					Map<Edge, Double> storedEdgeWeights = new HashMap<Edge, Double>();
					for (Edge e : pool.getNonDummyEdgeSet()) {
						storedEdgeWeights.put(e, pool.getEdgeWeight(e));
					}
					// Set all weights to 1
					setBinaryEdgeWeights(pool);
					// Solve w/no cardinality constraint
					Solution intermediate = s.solve(0);
					// Set minimum cardinality
					double min_cardinality = intermediate.getObjectiveValue();
					// Restore original weights
					setStoredEdgeWeights(pool, storedEdgeWeights);
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

			}

			// Run complete. Record data.
			if (DEBUG) { System.out.println("\nSolved with weights condition "+condition); }

			// Count vertices of each profile ID matched
			Map<Integer, Integer> totalPairsMatchedByProfile = new HashMap();
			for(int vertType=1; vertType<=8; vertType++) {
				totalPairsMatchedByProfile.put(vertType, 0);
			}

			//Count edges of each rank matched
			Map<Integer, Integer> totalPairsMatchedByRank = new HashMap();
			for(int rank=1; rank<=8; rank++) {
				totalPairsMatchedByRank.put(rank, 0);
			}

			for (Cycle c : totalMatches) {
				for (Vertex v : Cycle.getConstituentVertices(c, pool)) {
					Integer profileID = ((VariationVertexPair) v).getProfileID();
					totalPairsMatchedByProfile.put(profileID, totalPairsMatchedByProfile.get(profileID)+1);
				}
				for (Edge edge : c.getEdges()) {
					VariationVertexPair fromVertex = (VariationVertexPair) pool.getEdgeSource(edge);
					VariationVertexPair toVertex = (VariationVertexPair) pool.getEdgeTarget(edge);
					int rank = fromVertex.getRank(toVertex);
					totalPairsMatchedByRank.put(rank, totalPairsMatchedByRank.get(rank)+1);
				}
			}

			if (DEBUG) {
				System.out.println("Total pairs matched by profile:");
				System.out.println(Arrays.asList(totalPairsMatchedByProfile));
				System.out.println("Total pairs matched by rank:");
				System.out.println(Arrays.asList(totalPairsMatchedByProfile));
			}

			// Record results for this entire run
			out.set(Col.SEEN_PAIRS, totalPairsSeen);
			out.set(Col.DEPARTED_PAIRS, totalPairsDeparted);

			for(Integer vertType : totalPairsMatchedByProfile.keySet()) {
				out.set(Col.valueOf("MATCHED_TYPE"+vertType), totalPairsMatchedByProfile.get(vertType));
			}
			for(Integer vertRank : totalPairsMatchedByRank.keySet()) {
				out.set(Col.valueOf("MATCHED_RANK"+vertRank), totalPairsMatchedByRank.get(vertRank));
			}
			for(Integer vertType : totalPairsSeenByProfile.keySet()) {
				out.set(Col.valueOf("SEEN_TYPE"+vertType), totalPairsSeenByProfile.get(vertType));
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
		}

		// All runs complete
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
	 * For each run in NUM_RUNS, creates a simulation of ITERATIONS days.
	 * Break ties (equal-cardinality matchings) using weights based only on
	 * recipient profile (calculated with BT model).
	 */
	public static void runSimulationWithPatientWeights(String expId) {
		long startTime = System.currentTimeMillis();

		// Store output for csv export
		String condition = Condition.PATIENT_WEIGHTS.toString();
		String path = OUTPUT_PATH + condition + "_" + expId + ".csv";
		VariationOutput out = null;
		try {
			out = new VariationOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		for (int n = 0; n < NUM_RUNS; n++) {

			if (DEBUG) { System.out.println("\n****RUN "+n+"****"); }

			long seed = 100L + n;
			long rFailureSeed = seed + 1L;
			long rEntranceSeed = seed + 2L;
			long rDepartureSeed = seed + 3L;
			long rArrivalSeedM = seed + 4L;

			Random rFailure = new Random(rFailureSeed);
			Random rEntrance = new Random(rEntranceSeed);
			Random rDeparture = new Random(rDepartureSeed);
			Random rArrivalM = new Random(rArrivalSeedM);

			VariationPoolGenerator poolGen = new VariationPoolGenerator(rEntrance);
			ExponentialArrivalDistribution m = new ExponentialArrivalDistribution(1.0/EXPECTED_PAIRS, rArrivalM);
			Pool pool = new Pool(Edge.class);
			ArrayList<Cycle> matches = new ArrayList<Cycle>();

			// Record parameters for this set of runs
			out.set(Col.CONDITION, condition);
			out.set(Col.SEED, seed);
			out.set(Col.NUM_ITERATIONS, ITERATIONS);
			out.set(Col.ARRIVAL_PAIRS, EXPECTED_PAIRS);

			// Accumulators for this run
			int totalPairsSeen = 0;
			int totalPairsDeparted = 0;
			ArrayList<Cycle> totalMatches = new ArrayList<Cycle>();
			Map<Integer, Integer> totalPairsSeenByProfile = new HashMap<Integer, Integer>();
			for(int vertType=1; vertType<=8; vertType++) {
				totalPairsSeenByProfile.put(vertType, 0);
			}

			for (int i = 0; i < ITERATIONS; i++) {
				// Add new vertices to the pool
				int pairs = m.draw().intValue();
				totalPairsSeen += pairs;
				if (DEBUG) { System.out.println("ITERATION: "+i+"\t"+pairs+" new pairs"); }

				// Add vertices with VariationVertexPair weights for edge weights
				Set<Vertex> addedVertices = null;
				addedVertices = poolGen.addVerticesWithPatientEdgeWeights(pool, pairs);

				// Keep track of how many of each patient profile arrive in the pool
				for(Vertex v : addedVertices) {
					VariationVertexPair eV = (VariationVertexPair) v;
					totalPairsSeenByProfile.put(eV.getProfileID(), totalPairsSeenByProfile.get(eV.getProfileID())+1);
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
						if (DEBUG) {
							System.out.println("Matched cycle: ");
							for (Vertex v : Cycle.getConstituentVertices(ci, pool)) {
								System.out.print(v.getID() + "~");
							}
							System.out.println("");
						}
						// Remove all vertices in the match from the pool
						pool.removeAllVertices(Cycle.getConstituentVertices(ci, pool));
						iter.remove();
					}
				}

				if (DEBUG) { printPool(pool, false); }

				// Match the vertex pairs in the pool
				CycleGenerator cg = new CycleGenerator(pool);
				List<Cycle> cycles = cg.generateCyclesAndChains(CYCLE_CAP, 0, true);
				CycleMembership membership = new CycleMembership(pool, cycles);

				try{
					VariationCPLEXSolver s = new VariationCPLEXSolver(pool, cycles, membership);

					// Temporarily store all weights
					Map<Edge, Double> storedEdgeWeights = new HashMap<Edge, Double>();
					for (Edge e : pool.getNonDummyEdgeSet()) {
						storedEdgeWeights.put(e, pool.getEdgeWeight(e));
					}
					// Set all weights to 1
					setBinaryEdgeWeights(pool);
					// Solve w/no cardinality constraint
					Solution intermediate = s.solve(0);
					// Set minimum cardinality
					double min_cardinality = intermediate.getObjectiveValue();
					// Restore original weights
					setStoredEdgeWeights(pool, storedEdgeWeights);
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

			}

			// Run complete. Record data.
			if (DEBUG) { System.out.println("\nSolved with weights condition "+condition); }

			// Count vertices of each profile ID matched
			Map<Integer, Integer> totalPairsMatchedByProfile = new HashMap();
			for(int vertType=1; vertType<=8; vertType++) {
				totalPairsMatchedByProfile.put(vertType, 0);
			}

			//Count edges of each rank matched
			Map<Integer, Integer> totalPairsMatchedByRank = new HashMap();
			for(int rank=1; rank<=8; rank++) {
				totalPairsMatchedByRank.put(rank, 0);
			}

			for (Cycle c : totalMatches) {
				for (Vertex v : Cycle.getConstituentVertices(c, pool)) {
					Integer profileID = ((VariationVertexPair) v).getProfileID();
					totalPairsMatchedByProfile.put(profileID, totalPairsMatchedByProfile.get(profileID)+1);
				}
				for (Edge edge : c.getEdges()) {
					VariationVertexPair fromVertex = (VariationVertexPair) pool.getEdgeSource(edge);
					VariationVertexPair toVertex = (VariationVertexPair) pool.getEdgeTarget(edge);
					int rank = fromVertex.getRank(toVertex);
					totalPairsMatchedByRank.put(rank, totalPairsMatchedByRank.get(rank)+1);
				}
			}

			if (DEBUG) {
				System.out.println("Total pairs matched by profile:");
				System.out.println(Arrays.asList(totalPairsMatchedByProfile));
				System.out.println("Total pairs matched by rank:");
				System.out.println(Arrays.asList(totalPairsMatchedByProfile));
			}

			// Record results for this entire run
			out.set(Col.SEEN_PAIRS, totalPairsSeen);
			out.set(Col.DEPARTED_PAIRS, totalPairsDeparted);

			for(Integer vertType : totalPairsMatchedByProfile.keySet()) {
				out.set(Col.valueOf("MATCHED_TYPE"+vertType), totalPairsMatchedByProfile.get(vertType));
			}
			for(Integer vertRank : totalPairsMatchedByRank.keySet()) {
				out.set(Col.valueOf("MATCHED_RANK"+vertRank), totalPairsMatchedByRank.get(vertRank));
			}
			for(Integer vertType : totalPairsSeenByProfile.keySet()) {
				out.set(Col.valueOf("SEEN_TYPE"+vertType), totalPairsSeenByProfile.get(vertType));
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
		}

		// All runs complete
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
	 * For each run in NUM_RUNS, creates a simulation of ITERATIONS days.
	 * Weight all edges equally. Break ties between equal-cardinality matching
	 * randomly.
	 */
	public static void runSimulationWithEqualWeights(String expId) {
		long startTime = System.currentTimeMillis();

		// Store output for csv export
		String condition = Condition.EQUAL_WEIGHTS.toString();
		String path = OUTPUT_PATH + condition + "_" + expId + ".csv";
		VariationOutput out = null;
		try {
			out = new VariationOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		for (int n = 0; n < NUM_RUNS; n++) {

			if (DEBUG) { System.out.println("\n****RUN "+n+"****"); }

			long seed = 100L + n;
			long rFailureSeed = seed + 1L;
			long rEntranceSeed = seed + 2L;
			long rDepartureSeed = seed + 3L;
			long rArrivalSeedM = seed + 4L;

			Random rFailure = new Random(rFailureSeed);
			Random rEntrance = new Random(rEntranceSeed);
			Random rDeparture = new Random(rDepartureSeed);
			Random rArrivalM = new Random(rArrivalSeedM);

			VariationPoolGenerator poolGen = new VariationPoolGenerator(rEntrance);
			ExponentialArrivalDistribution m = new ExponentialArrivalDistribution(1.0/EXPECTED_PAIRS, rArrivalM);
			Pool pool = new Pool(Edge.class);
			ArrayList<Cycle> matches = new ArrayList<Cycle>();

			// Record parameters for this set of runs
			out.set(Col.CONDITION, condition);
			out.set(Col.SEED, seed);
			out.set(Col.NUM_ITERATIONS, ITERATIONS);
			out.set(Col.ARRIVAL_PAIRS, EXPECTED_PAIRS);

			// Accumulators for this run
			int totalPairsSeen = 0;
			int totalPairsDeparted = 0;
			ArrayList<Cycle> totalMatches = new ArrayList<Cycle>();
			Map<Integer, Integer> totalPairsSeenByProfile = new HashMap<Integer, Integer>();
			for(int vertType=1; vertType<=8; vertType++) {
				totalPairsSeenByProfile.put(vertType, 0);
			}

			for (int i = 0; i < ITERATIONS; i++) {
				// Add new vertices to the pool
				int pairs = m.draw().intValue();
				totalPairsSeen += pairs;
				if (DEBUG) { System.out.println("ITERATION: "+i+"\t"+pairs+" new pairs"); }

				// Add vertices with VariationVertexPair weights for edge weights
				Set<Vertex> addedVertices = null;
				addedVertices = poolGen.addVerticesToPool(pool, pairs);

				// Keep track of how many of each patient profile arrive in the pool
				for(Vertex v : addedVertices) {
					VariationVertexPair eV = (VariationVertexPair) v;
					totalPairsSeenByProfile.put(eV.getProfileID(), totalPairsSeenByProfile.get(eV.getProfileID())+1);
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
						if (DEBUG) {
							System.out.println("Matched cycle: ");
							for (Vertex v : Cycle.getConstituentVertices(ci, pool)) {
								System.out.print(v.getID() + "~");
							}
							System.out.println("");
						}
						// Remove all vertices in the match from the pool
						pool.removeAllVertices(Cycle.getConstituentVertices(ci, pool));
						iter.remove();
					}
				}

				if (DEBUG) { printPool(pool, false); }

				// Match the vertex pairs in the pool
				CycleGenerator cg = new CycleGenerator(pool);
				List<Cycle> cycles = cg.generateCyclesAndChains(CYCLE_CAP, 0, true);
				CycleMembership membership = new CycleMembership(pool, cycles);

				try{
					// Solve w/no cardinality constraint
					VariationCPLEXSolver s = new VariationCPLEXSolver(pool, cycles, membership);
					Solution sol = s.solve(0);

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

			}

			// Run complete. Record data.
			if (DEBUG) { System.out.println("\nSolved with weights condition "+condition); }

			// Count vertices of each profile ID matched
			Map<Integer, Integer> totalPairsMatchedByProfile = new HashMap();
			for(int vertType=1; vertType<=8; vertType++) {
				totalPairsMatchedByProfile.put(vertType, 0);
			}

			//Count edges of each rank matched
			Map<Integer, Integer> totalPairsMatchedByRank = new HashMap();
			for(int rank=1; rank<=8; rank++) {
				totalPairsMatchedByRank.put(rank, 0);
			}

			for (Cycle c : totalMatches) {
				for (Vertex v : Cycle.getConstituentVertices(c, pool)) {
					Integer profileID = ((VariationVertexPair) v).getProfileID();
					totalPairsMatchedByProfile.put(profileID, totalPairsMatchedByProfile.get(profileID)+1);
				}
				for (Edge edge : c.getEdges()) {
					VariationVertexPair fromVertex = (VariationVertexPair) pool.getEdgeSource(edge);
					VariationVertexPair toVertex = (VariationVertexPair) pool.getEdgeTarget(edge);
					int rank = fromVertex.getRank(toVertex);
					totalPairsMatchedByRank.put(rank, totalPairsMatchedByRank.get(rank)+1);
				}
			}

			if (DEBUG) {
				System.out.println("Total pairs matched by profile:");
				System.out.println(Arrays.asList(totalPairsMatchedByProfile));
				System.out.println("Total pairs matched by rank:");
				System.out.println(Arrays.asList(totalPairsMatchedByProfile));
			}

			// Record results for this entire run
			out.set(Col.SEEN_PAIRS, totalPairsSeen);
			out.set(Col.DEPARTED_PAIRS, totalPairsDeparted);

			for(Integer vertType : totalPairsMatchedByProfile.keySet()) {
				out.set(Col.valueOf("MATCHED_TYPE"+vertType), totalPairsMatchedByProfile.get(vertType));
			}
			for(Integer vertRank : totalPairsMatchedByRank.keySet()) {
				out.set(Col.valueOf("MATCHED_RANK"+vertRank), totalPairsMatchedByRank.get(vertRank));
			}
			for(Integer vertType : totalPairsSeenByProfile.keySet()) {
				out.set(Col.valueOf("SEEN_TYPE"+vertType), totalPairsSeenByProfile.get(vertType));
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
		}

		// All runs complete
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

	// Helper method to print current state of pool
	public static void printPool(Pool pool, boolean printAllEdges) {
		System.out.println("\nPrinting pool.");
		System.out.println("pairs in pool: "+pool.getNumPairs());
		System.out.println("altruists in pool: "+pool.getNumAltruists());
		System.out.println("edges in pool: "+pool.getNumNonDummyEdges());
		System.out.println("Vertices:");
		for (VertexPair v : pool.getPairs()) {
			System.out.print(v+"~");
		}
		System.out.println("");
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
	public static void setStoredEdgeWeights(Pool pool, Map<Edge, Double> specialWeights) {
		for (Edge e : pool.getNonDummyEdgeSet()) {
			pool.setEdgeWeight(e, specialWeights.get(e));
		}
	}

	public static void main(String[] args) throws Exception {
		runExperiments();
	}

}