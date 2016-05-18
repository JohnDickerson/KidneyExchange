package edu.cmu.cs.dickerson.kpd.rematch;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.rematch.RematchCPLEXSolver.RematchConstraintType;
import edu.cmu.cs.dickerson.kpd.rematch.RematchECOutput.Col;
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
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;

/**
 * Experiments for EC-2015 paper / NSF grant
 * Also experiments for cost-based edge testing
 * @author John P Dickerson
 *
 */
public class DriverRematch {

	private static final Logger logger = Logger.getLogger(DriverRematch.class.getSimpleName());

	// Parameters we don't iterate over
	private final int cycleCap;
	private final int numPairs;
	private final int numAlts;
	private final int maxNumRematchesEC2015;
	private final double maxAvgEdgesPerVertex;
	private final RematchConstraintType rematchType;

	// Maybe-they'll-change parameters
	private Random r;
	private long seed;

	// Iterate-over-these parameters
	private List<PoolGenerator> genList;
	private List<File> matchDirList;
	private List<Double> failureRateList;
	private List<Integer> chainCapList;
	private List<Integer> hardMaxPerVertexList;

	// Experiment significance parameters
	private int numReps;
	private boolean onlyPlotMaxRematch;

	public DriverRematch() { 

		// Invariant parameters
		this.cycleCap = 2;
		this.numPairs = 250;
		this.numAlts = 0;
		this.maxNumRematchesEC2015 = 5;
		this.maxAvgEdgesPerVertex = Double.MAX_VALUE;
		this.rematchType = RematchConstraintType.ADAPTIVE_DETERMINISTIC;		

		// Maybe they'll change parameters
		this.r = new Random();

		// Track, manually increment if needed within loops
		this.seed = 1422684007877L;   // Setting seed for Non-adaptive runs to same as Adaptive runs
		//long seed = System.currentTimeMillis();  // Set seed to something new each time

		// List of generators we want to use
		this.genList = Arrays.asList(new PoolGenerator[] {
				new SaidmanPoolGenerator(r),
				//UNOSGenerator.makeAndInitialize(IOUtil.getBaseUNOSFilePath(), ',', r),	
		});

		this.matchDirList = Arrays.asList((new File(IOUtil.getBaseUNOSFilePath())).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return file.isDirectory() && !name.toLowerCase().endsWith(".zip");
			}
		}));

		// List of constant edge failure rates we want to use
		this.failureRateList = Arrays.asList(new Double[] {
				0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9,
				//0.5,
		});

		// List of chain caps we want to use
		this.chainCapList = Arrays.asList(new Integer[] {
				0,//4,
				//10,
		});

		// List of hard max tests per vertex
		this.hardMaxPerVertexList = Arrays.asList(new Integer[] {
				//5, 10, 
				Integer.MAX_VALUE-1,
		});

		// Flip to true if we only want data for the max number of rematches performed, false performs for #rematches={0..Max}
		this.onlyPlotMaxRematch = false;

		// Number of repetitions for each parameter vector
		this.numReps = 100;
	}

	public void run() {

		// Store output for the EC runs and for the cost-based AAAI runs
		RematchECOutput outEC = null;
		RematchAAAIOutput outAAAI = null;
		try {
			long currTime = System.currentTimeMillis();
			outEC = new RematchECOutput("rematch_" + currTime + "_ec.csv");
			outAAAI = new RematchAAAIOutput("rematch_" + currTime + "_aaai.csv");
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		for(PoolGenerator gen : genList) { 
			for(int generatedCt=0; generatedCt<50; generatedCt++) {

				boolean usingGeneratedData = true; String matchRunID = "";
				Pool pool = gen.generate(numPairs, 0);//numPairs/20);  // For Saidman, take 250 vertices and ~5% altruists

				/*for(File matchDir : matchDirList) {

			usingGeneratedData = false;   // records the match ID instead of the generator name in the CSV file
			UNOSLoader loader = new UNOSLoader(',');

			// We assume a lot about filenames here.  Figure out which .csv files matter
			matchRunID = "", donorFilePath = "", recipientFilePath = "", edgeFilePath = "";
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
				 */

				String generatorName = usingGeneratedData ? gen.getClass().getSimpleName() : matchRunID;
				for(Double failureRate : failureRateList) {
					for(Integer chainCap : chainCapList) {
						for(Integer hardMaxPerVertex : hardMaxPerVertexList) {
							logger.info("failureRate=" + failureRate);
							for(int repCt=0; repCt < numReps; repCt++) {

								// Maintain the same seed as we increment numRematches
								seed++;
								r.setSeed(seed);

								// Generate pool with expected failure rate of failureRate
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
											new CycleMembership(pool, cycles)
											)
											).solve();
									double oracleMatchUtil = oracleSolution.getObjectiveValue();
									cycles = null; cg = null;

									// Reset the pool's edges to their failure probabilities, not failure statuses
									resetPoolEdgeTestsToUnknown(edgeFailureRateMap);

									// Call the EC-15 solver for this specific pool and record in the EC-15 file
									doRematchEC2015(outEC, pool, chainCap, edgeFailedMap, edgeFailureRateMap, generatorName, hardMaxPerVertex, failureRate, oracleMatchUtil);

									
								} catch(SolverException e) {
									e.printStackTrace();
									logger.severe("Solver Exception thrown!" + e);
								}
							} // end numReps
						} // end hardMaxPerVertexList
					} // end chainCapList
				} // end failureRateList
			} // end of generated count repetition (not needed for individual UNOS runs)
		} // end genList

		// Clean up output stream
		logger.info("Attempting output stream, etc, cleanup ...");
		if(null != outEC) {
			try {
				outEC.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		if(null != outAAAI) {
			try {
				outAAAI.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		logger.info("Successfully cleaned up; exiting ...");
		return;
	}


	/**
	 * If we previously"tested" an edge and, via that result and set its failure
	 * probability to 0.0 (=failed) or 1.0 (=succeeded), this method returns that
	 * failure probability to its originally-generated value in [0.0, 1.0]
	 * @param edgeFailureRateMap map of Edge -> original failure rate
	 */
	private void resetPoolEdgeTestsToUnknown(Map<Edge, Double> edgeFailureRateMap) {
		// Reset the pool's edges to their failure probabilities, not failure statuses
		for(Map.Entry<Edge, Double> entry : edgeFailureRateMap.entrySet()) {
			entry.getKey().setFailureProbability(entry.getValue());
		}
	}

	
	/**
	 * Given a recommended matching, simulates that matching on the omniscient
	 * pool where we know all the edge tests and returns the successful utility
	 * gained from that matching
	 * 
	 * @param sol recommended matching
	 * @param pool underlying pool of vertices and edges
	 * @param edgeFailedMap mapping of edges -> whether they exist or not
	 * @return number of successful transplants
	 */
	private double calculateNumTransplants(
			Solution sol,
			Pool pool,
			Map<Edge, Boolean> edgeFailedMap
			) {
		
		double numActualTransplants=0;
		for(Cycle c : sol.getMatching()) {

			if(Cycle.isAChain(c, pool)) {
				// Chains succeed incrementally (starting from altruist up to first edge failure)
				ListIterator<Edge> reverseEdgeIt = c.getEdges().listIterator(c.getEdges().size());
				int successCt = 0;
				while(reverseEdgeIt.hasPrevious()) {
					Edge e = reverseEdgeIt.previous();
					if(successCt == 0 && !pool.getEdgeSource(e).isAltruist()) {
						System.err.println("Chain generated in a different way than expected; chain check isn't going to perform correctly.");
						System.exit(-1);
					}
					if(edgeFailedMap.get(e)) {
						break;
					}
					successCt++;
				}

				if(successCt == c.getEdges().size()) {
					successCt -= 1;    // if nothing failed, don't count dummy edge going back to altruist
				}
				numActualTransplants += successCt;
			} else {
				// Cycles fail or succeed in entirety
				boolean failed = false;
				for(Edge e : c.getEdges()) {
					failed |= edgeFailedMap.get(e);  // even one edge failure -> entire cycle fails completely
				}
				if(!failed) { numActualTransplants += c.getEdges().size(); } // if cycle succeeds, count all verts in it

			}  // end of isAChain conditional
		}
		return numActualTransplants;
	}
	

	/**
	 * Runs the experiments for the EC-15 paper
	 * 
	 * @param out
	 * @param pool
	 * @param chainCap
	 * @param edgeFailedMap
	 * @param edgeFailureRateMap
	 * @param generatorName
	 * @param hardMaxPerVertex
	 * @param failureRate
	 * @param oracleMatchUtil
	 * @throws SolverException
	 */
	private void doRematchEC2015(
			RematchECOutput out,
			Pool pool, 
			int chainCap,
			Map<Edge, Boolean> edgeFailedMap,       // Omniscience, which edges fail/don't fail in reality
			Map<Edge, Double> edgeFailureRateMap,   // Generated failure rates for each edge
			String generatorName,
			int hardMaxPerVertex,
			double failureRate,
			double oracleMatchUtil
			) throws SolverException {

		// Get a set of edges that we should formally test (maps time period -> set of edges to test)
		CycleGenerator cg = new CycleGenerator(pool);
		List<Cycle> cycles = cg.generateCyclesAndChains(cycleCap, chainCap, true);
		Map<Integer, Set<Edge>> edgesToTestMap = (new RematchCPLEXSolver(
				pool,
				cycles,
				new CycleMembership(pool, cycles))
				).solve(maxNumRematchesEC2015, rematchType, edgeFailedMap, maxAvgEdgesPerVertex);

		// Some of the rematchers change edge failure probabilities; reset here
		resetPoolEdgeTestsToUnknown(edgeFailureRateMap);

		// Keep track of how many incoming edges to each vertex have been checked
		Map<Vertex, Set<Edge>> perVertexEdgeTested = new HashMap<Vertex, Set<Edge>>();
		for(Vertex v : pool.vertexSet()) {
			perVertexEdgeTested.put(v, new HashSet<Edge>()); 
		}

		// Get non-prescient match utilities for increasing number of allowed rematches
		Set<Edge> edgesToTestSet = new HashSet<Edge>(); // incrementally keep track of edges to test
		for(int numRematches=0; numRematches<=maxNumRematchesEC2015; numRematches++) {
			// If we only want data for the last (highest) #rematches, skip there
			if(onlyPlotMaxRematch) {
				numRematches = maxNumRematchesEC2015;
				// Add all #rematches' edge sets to the set of edges to test
				for(Map.Entry<Integer, Set<Edge>> reSet : edgesToTestMap.entrySet()) {
					edgesToTestSet.addAll( reSet.getValue() );
				}
			} else {
				// Add this #rematches' edge set to the total set of edges to test
				if(edgesToTestMap.containsKey(numRematches) && null!=edgesToTestMap.get(numRematches)) {
					edgesToTestSet.addAll( edgesToTestMap.get(numRematches) );
				}
			}

			// Initial bookkeeping
			out.set(Col.SEED, seed);
			out.set(Col.CYCLE_CAP, cycleCap);
			out.set(Col.CHAIN_CAP, chainCap);
			out.set(Col.NUM_PAIRS, pool.getPairs().size());
			out.set(Col.NUM_ALTRUISTS, pool.getAltruists().size());
			out.set(Col.NUM_EDGES, pool.getNumNonDummyEdges());
			out.set(Col.GENERATOR, generatorName);
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
			double numActualTransplants = calculateNumTransplants(rematchSolution, pool, edgeFailedMap);
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

	} // end of doRematchEC2015 method



	public static void main(String[] args) {
		(new DriverRematch()).run();
	}
}
