package edu.cmu.cs.dickerson.kpd.rematch;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.sun.istack.internal.logging.Logger;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.rematch.RematchCPLEXSolver.RematchConstraintType;
import edu.cmu.cs.dickerson.kpd.rematch.RematchOutput.Col;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.UNOSGenerator;

/**
 * Experiments for SODA-2014 submission / NSF grant
 * @author John P Dickerson
 *
 */
public class DriverRematch {

	private static final Logger logger = Logger.getLogger(DriverRematch.class);

	public static void main(String[] args) {

		Random r = new Random();

		// List of generators we want to use
		List<PoolGenerator> genList = Arrays.asList(new PoolGenerator[] {
				//new SaidmanPoolGenerator(r),
				UNOSGenerator.makeAndInitialize(IOUtil.getBaseUNOSFilePath(), ',', r),	
		});

		// List of constant edge failure rates we want to use
		List<Double> failureRateList = Arrays.asList(new Double[] {
				0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9,
		});

		// Invariant parameters
		int cycleCap = 3;
		int chainCap = 0;
		int numPairs = 250;
		int numAlts = 0;
		int maxNumRematches = 10;
		RematchConstraintType rematchType = RematchConstraintType.REMOVE_MATCHED_EDGES;
		
		// Number of repetitions for each parameter vector
		int numReps = 50;

		// Store output
		String path = "rematch_" + System.currentTimeMillis() + ".csv";
		RematchOutput out = null;
		try {
			out = new RematchOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		// Track, manually increment if needed within loops
		long seed = System.currentTimeMillis();

		for(PoolGenerator gen : genList) {
			for(Double failureRate : failureRateList) {

				logger.info("Gen=" + gen + ", failureRate=" + failureRate);
				for(int repCt=0; repCt < numReps; repCt++) {

					// Maintain the same seed as we increment numRematches
					seed++;

					// Generate pool with expected failure rate of failureRate
					Pool pool = gen.generate(numPairs, numAlts);
					FailureProbabilityUtil.setFailureProbability(pool, FailureProbabilityUtil.ProbabilityDistribution.CONSTANT, r, failureRate);

					// Keep track of the failure rates generated for each (we'll reset the pool a bunch of times)
					Map<Edge, Double> edgeFailureRateMap = new HashMap<Edge, Double>();
					// Simulate a ground truth failure for each edge in the pool (but don't tell optimizer yet)
					Map<Edge, Boolean> edgeFailedMap = new HashMap<Edge, Boolean>();
					for(Edge e : pool.edgeSet()) {
						edgeFailureRateMap.put(e, e.getFailureProbability());
						edgeFailedMap.put(e, r.nextDouble() < e.getFailureProbability());
					}

					// Do a prescient max utility matching; this is the best we could do if we knew every edge success/failure
					for(Map.Entry<Edge, Boolean> entry : edgeFailedMap.entrySet()) {
						Edge e = entry.getKey();
						Boolean failed = entry.getValue();
						e.setFailureProbability(failed ? 1.0 : 0.0);
					}

					try {
						// All we care about is the value of the prescient solution; solve and scrap everything else
						CycleGenerator cg = new CycleGenerator(pool);
						List<Cycle> cycles = cg.generateCyclesAndChains(cycleCap, chainCap, true);
						Solution oracleSolution = (new CycleFormulationCPLEXSolver(
								pool, 
								cycles,
								new CycleMembership(pool, cycles))
								).solve();
						double oracleMatchUtil = oracleSolution.getObjectiveValue();
						cycles = null;

						// Reset the pool's edges to their failure probabilities, not failure statuses
						for(Map.Entry<Edge, Double> entry : edgeFailureRateMap.entrySet()) {
							entry.getKey().setFailureProbability(entry.getValue());
						}
						
						// Get a set of edges that we should formally test (maps time period -> set of edges to test)
						cg = new CycleGenerator(pool);
						cycles = cg.generateCyclesAndChains(cycleCap, chainCap, true);
						Map<Integer, Set<Edge>> edgesToTestMap = (new RematchCPLEXSolver(
								pool,
								cycles,
								new CycleMembership(pool, cycles))
								).solve(maxNumRematches, rematchType);

						// Get non-prescient match utilities for increasing number of allowed rematches
						Set<Edge> edgesToTestSet = new HashSet<Edge>(); // incrementally keep track of edges to test
						for(int numRematches=0; numRematches<=maxNumRematches; numRematches++) {

							// Initial bookkeeping
							out.set(Col.SEED, seed);
							out.set(Col.CYCLE_CAP, cycleCap);
							out.set(Col.CHAIN_CAP, chainCap);
							out.set(Col.NUM_PAIRS, numPairs);
							out.set(Col.NUM_ALTRUISTS, numAlts);
							out.set(Col.NUM_EDGES, pool.edgeSet().size());  // will break if we start including chains (dummies)
							out.set(Col.GENERATOR, gen);
							out.set(Col.REMATCH_TYPE, rematchType);
							out.set(Col.FAILURE_RATE, failureRate);
							out.set(Col.NUM_REMATCHES, numRematches);
							out.set(Col.ORACLE_MATCH_UTIL, oracleMatchUtil);

							// Add this #rematches' edge set to the total set of edges to test
							edgesToTestSet.addAll( edgesToTestMap.get(numRematches) );
							
							// Update the pool with tested edges
							out.set(Col.NUM_EDGE_TESTS, edgesToTestSet.size());
							for(Edge e : edgesToTestSet) {
								e.setFailureProbability( edgeFailedMap.get(e) ? 1.0 : 0.0);
							}

							// Do a max utility matching on this updated pool
							cycles = cg.generateCyclesAndChains(cycleCap, chainCap, true);
							Solution rematchSolution = (new CycleFormulationCPLEXSolver(
									pool, 
									cycles, 
									new CycleMembership(pool, cycles))
									).solve();
							// Now count the number of matches that actually went to transplant
							// TODO WILL BREAK IF WE HAVE CHAINS (need to do incremental execution of partial chain failure)
							double numActualTransplants=0;
							for(Cycle c : rematchSolution.getMatching()) {
								boolean failed = false;
								for(Edge e : c.getEdges()) {
									failed |= edgeFailedMap.get(e);  // even one edge failure -> entire cycle fails completely
								}
								if(!failed) { numActualTransplants += c.getEdges().size(); } // if cycle succeeds, count all verts in it
							}
							out.set(Col.REMATCH_UTIL, numActualTransplants);
							cycles = null;

							// Write the  row of data
							try {
								out.record();
							} catch(IOException e) {
								IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
								e.printStackTrace();
								System.exit(-1);
							}
						} // end numRematchesList

					} catch(SolverException e) {
						e.printStackTrace();
						logger.severe("Solver Exception thrown!", e);
					}
				} // end numReps
			} // end failureRateList
		} // end genList

		// Clean up output stream
		logger.info("Attempting output stream, etc, cleanup ...");
		if(null != out) {
			try {
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		logger.info("Successfully cleaned up; exiting ...");
		return;
	}
}
