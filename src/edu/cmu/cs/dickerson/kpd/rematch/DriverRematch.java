package edu.cmu.cs.dickerson.kpd.rematch;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.rematch.RematchCPLEXSolver.RematchConstraintType;
import edu.cmu.cs.dickerson.kpd.rematch.strats.RematchStrat;
import edu.cmu.cs.dickerson.kpd.rematch.strats.RematchStratEC2015;
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
		this.cycleCap = 3;
		this.numPairs = 100;
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
				//new SaidmanPoolGenerator(r),
				UNOSGenerator.makeAndInitialize(IOUtil.getBaseUNOSFilePath(), ',', r),	
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
				//0,
				4,
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
		
		// Shared constants over all the rematch run types
		RematchStrat.init(cycleCap, numPairs, numAlts, maxAvgEdgesPerVertex, rematchType, onlyPlotMaxRematch);
	}

	public void run() {

		// Store output for the EC runs and for the cost-based AAAI runs
		RematchOutput outEC = null;
		RematchOutput outAAAI = null;
		try {
			long currTime = System.currentTimeMillis();
			outEC = new RematchOutput("rematch_" + currTime + "_ec.csv");
			outAAAI = new RematchOutput("rematch_" + currTime + "_aaai.csv");
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
									RematchUtil.resetPoolEdgeTestsToUnknown(edgeFailureRateMap);
									cg = new CycleGenerator(pool);
									cycles = cg.generateCyclesAndChains(cycleCap, chainCap, true);
									
									// Run the EC-15 experiments 
									RematchStrat rematchStrat = new RematchStratEC2015(pool, chainCap, edgeFailedMap, edgeFailureRateMap, generatorName, hardMaxPerVertex, failureRate, oracleMatchUtil, seed);
									RematchCPLEXSolver solverEC = new RematchBatchCPLEXSolver(
											pool,
											cycles,
											new CycleMembership(pool, cycles));
									int numEdgesTestedByEC = rematchStrat.runRematch(outEC, this.maxNumRematchesEC2015, solverEC, RematchConstraintType.ADAPTIVE_DETERMINISTIC);
									cycles = null; cg = null;

									
									// Reset the pool's edges to their failure probabilities, not failure statuses	
									RematchUtil.resetPoolEdgeTestsToUnknown(edgeFailureRateMap);
									cg = new CycleGenerator(pool);
									cycles = cg.generateCyclesAndChains(cycleCap, chainCap, true);
									
									// Now call the AAAI solver for this same pool + edge results, record in AAAI file							
									RematchCPLEXSolver solverAAAI = new RematchSequentialCPLEXSolver(
											pool,
											cycles,
											new CycleMembership(pool, cycles));
									int numEdgesTestedByAAAI = rematchStrat.runRematch(outAAAI, numEdgesTestedByEC, solverAAAI, RematchConstraintType.FULLY_SEQUENTIAL);
									cycles = null; cg = null;

									
									logger.info("EC tested " + numEdgesTestedByEC + "; AAAI tested " + numEdgesTestedByAAAI + " edges");
									
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

	public static void main(String[] args) {
		(new DriverRematch()).run();
	}
}
