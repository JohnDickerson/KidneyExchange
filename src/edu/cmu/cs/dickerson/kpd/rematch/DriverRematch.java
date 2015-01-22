package edu.cmu.cs.dickerson.kpd.rematch;

import java.io.File;
import java.io.FilenameFilter;
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
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.UNOSGenerator;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSLoader;
import edu.cmu.cs.dickerson.kpd.structure.real.exception.LoaderException;

/**
 * Experiments for EC-2015 submission / NSF grant
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

		File baseUNOSDir = new File(IOUtil.getBaseUNOSFilePath());
		List<File> matchDirList = Arrays.asList(baseUNOSDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return file.isDirectory() && !name.toLowerCase().endsWith(".zip");
			}
		}));

		// List of constant edge failure rates we want to use
		List<Double> failureRateList = Arrays.asList(new Double[] {
				//0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9,
				0.5,
		});

		// List of chain caps we want to use
		List<Integer> chainCapList = Arrays.asList(new Integer[] {
				0,4,10,
		});
		
		// Invariant parameters
		int cycleCap = 3;
		int numPairs = 250;
		int numAlts = 0;
		int maxNumRematches = 100;
		double maxAvgEdgesPerVertex = Double.MAX_VALUE;
		int hardMaxPerVertex = 5;//Integer.MAX_VALUE;//5;
		RematchConstraintType rematchType = RematchConstraintType.REMOVE_MATCHED_CYCLES;

		// Flip to true if we only want data for the max number of rematches performed, false performs for #rematches={0..Max}
		boolean onlyPlotMaxRematch = true;

		// Number of repetitions for each parameter vector
		int numReps = 1000;

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

		//for(PoolGenerator gen : genList) {

		for(File matchDir : matchDirList) {

			UNOSLoader loader = new UNOSLoader(',');

			// We assume a lot about filenames here.  Figure out which .csv files matter
			String matchRunID = "", donorFilePath = "", recipientFilePath = "", edgeFilePath = "";
			File[] csvFiles = matchDir.listFiles(new FilenameFilter() {  @Override public boolean accept(File file, String name) { return name.endsWith(".csv"); } });
			if(null == csvFiles || csvFiles.length < 1) { continue; }

			for(File csvFile : Arrays.asList(csvFiles)) {
				if(csvFile.getName().toUpperCase().contains("DONOR")) {
					donorFilePath = csvFile.getAbsolutePath();
					matchRunID = csvFile.getName().substring(0,8);
				} else if(csvFile.getName().toUpperCase().contains("RECIPIENT")) {
					recipientFilePath = csvFile.getAbsolutePath();
				} else if(csvFile.getName().toUpperCase().contains("EDGEWEIGHTS")) {
					edgeFilePath = csvFile.getAbsolutePath();
				}
			}

			// Make sure we're actually looking at a UNOS match run
			if(donorFilePath.isEmpty() || recipientFilePath.isEmpty() || edgeFilePath.isEmpty() || matchRunID.isEmpty()) {
				IOUtil.dPrintln("Couldn't figure out this directory!");
				System.exit(-1);
			}

			// Load the pool from donor, recipient, edge files -- force unit weights
			Pool pool = null;
			try {
				pool = loader.loadFromFile(donorFilePath, recipientFilePath, edgeFilePath, true);
			} catch(LoaderException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			for(Double failureRate : failureRateList) {
				for(Integer chainCap : chainCapList) {
				logger.info("failureRate=" + failureRate);
				for(int repCt=0; repCt < numReps; repCt++) {

					// Maintain the same seed as we increment numRematches
					seed++;

					// Generate pool with expected failure rate of failureRate
					//Pool pool = gen.generate(numPairs, numAlts);  // uncomment if we ever switch back to generator, not real
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
								).solve(maxNumRematches, rematchType, maxAvgEdgesPerVertex);

						// Keep track of how many incoming edges to each vertex have been checked
						Map<Vertex, Set<Edge>> perVertexEdgeTested = new HashMap<Vertex, Set<Edge>>();
						for(Vertex v : pool.vertexSet()) {
							perVertexEdgeTested.put(v, new HashSet<Edge>()); 
						}
						
						// Get non-prescient match utilities for increasing number of allowed rematches
						Set<Edge> edgesToTestSet = new HashSet<Edge>(); // incrementally keep track of edges to test
						for(int numRematches=0; numRematches<=maxNumRematches; numRematches++) {
							// If we only want data for the last (highest) #rematches, skip there
							if(onlyPlotMaxRematch) {
								numRematches = maxNumRematches;
								// Add all #rematches' edge sets to the set of edges to test
								for(Map.Entry<Integer, Set<Edge>> reSet : edgesToTestMap.entrySet()) {
									edgesToTestSet.addAll( reSet.getValue() );
								}
							} else {
								// Add this #rematches' edge set to the total set of edges to test
								edgesToTestSet.addAll( edgesToTestMap.get(numRematches) );
							}

							// Initial bookkeeping
							out.set(Col.SEED, seed);
							out.set(Col.CYCLE_CAP, cycleCap);
							out.set(Col.CHAIN_CAP, chainCap);
							out.set(Col.NUM_PAIRS, pool.getPairs().size());
							out.set(Col.NUM_ALTRUISTS, pool.getAltruists().size());
							out.set(Col.NUM_EDGES, pool.getNumNonDummyEdges());
							out.set(Col.GENERATOR, matchRunID);
							out.set(Col.MAX_AVG_EDGES_PER_VERT, maxAvgEdgesPerVertex);
							out.set(Col.HARD_MAX_EDGES_PER_VERT, hardMaxPerVertex);
							out.set(Col.REMATCH_TYPE, rematchType);
							out.set(Col.FAILURE_RATE, failureRate);
							out.set(Col.NUM_REMATCHES, numRematches);
							out.set(Col.ORACLE_MATCH_UTIL, oracleMatchUtil);

							// Update the pool with tested edges
							out.set(Col.NUM_EDGE_TESTS, edgesToTestSet.size());
							for(Edge e : edgesToTestSet) {
								Vertex dst = pool.getEdgeTarget(e);
								// If the destination vertex has remaining credits for testing edges, test this edge
								if(perVertexEdgeTested.get(dst).size() < hardMaxPerVertex) {
									e.setFailureProbability( edgeFailedMap.get(e) ? 1.0 : 0.0);
									perVertexEdgeTested.get(dst).add(e);
								}
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
			} // end chainCapList
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
