package edu.cmu.cs.dickerson.kpd.drivers;

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
import edu.cmu.cs.dickerson.kpd.io.FailureSensitivityOutput;
import edu.cmu.cs.dickerson.kpd.io.FailureSensitivityOutput.Col;
import edu.cmu.cs.dickerson.kpd.rematch.RematchUtil;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSLoader;
import edu.cmu.cs.dickerson.kpd.structure.real.exception.LoaderException;

public class DriverFailureSensitivity {

	private static final Logger logger = Logger.getLogger(DriverFailureSensitivity.class.getSimpleName());

	// Parameters we don't iterate over
	private final int cycleCap;

	// Maybe-they'll-change parameters
	private Random r;
	private long seed;

	// Iterate-over-these parameters
	private List<File> matchDirList;
	private List<Double> realFailureRateList;
	private List<Double> assumedFailureRateList;
	private List<Integer> chainCapList;

	// Experiment significance parameters
	private int numReps;

	public DriverFailureSensitivity() { 

		// Invariant parameters
		this.cycleCap = 3;

		// Maybe they'll change parameters
		this.r = new Random();

		// Track, manually increment if needed within loops
		this.seed = 1422684007877L;   // Setting seed for Non-adaptive runs to same as Adaptive runs
		//long seed = System.currentTimeMillis();  // Set seed to something new each time

		this.matchDirList = Arrays.asList((new File(IOUtil.getBaseUNOSFilePath())).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return file.isDirectory() && !name.toLowerCase().endsWith(".zip");
			}
		}));

		// List of constant edge failure rates we want to use -- true underlying rates
		this.realFailureRateList = Arrays.asList(new Double[] {
				0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9,
		});

		// List of constant edge failure rates we want to use -- lie to the optimizer
		this.assumedFailureRateList = Arrays.asList(new Double[] {
				0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9,
		});


		// List of chain caps we want to use
		this.chainCapList = Arrays.asList(new Integer[] {
				//0,
				4,
				//10,
		});

		// Number of repetitions for each parameter vector
		this.numReps = 50;
	}

	public void run() {

		// Store output for the EC runs and for the cost-based AAAI runs
		FailureSensitivityOutput out = null;
		try {
			long currTime = System.currentTimeMillis();
			out = new FailureSensitivityOutput("sensitivity_" + currTime + "_mansci.csv");
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}


		for(File matchDir : this.matchDirList) {

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

			// Load the pool from donor, recipient, edge files
			Pool pool = null;
			boolean forceUnitWeights = true;
			try {
				pool = loader.loadFromFile(donorFilePath, recipientFilePath, edgeFilePath, forceUnitWeights);
			} catch(LoaderException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			String generatorName = matchRunID;
			for(Double realFailureRate : realFailureRateList) {
				for(int repCt=0; repCt < numReps; repCt++) {
					// Maintain the same seed as we increment numRematches
					seed++;
					r.setSeed(seed);

					// Generate pool with REAL expected failure rates so we simulate failures, then flip to ASSUMED rates for optimizer
					FailureProbabilityUtil.setFailureProbability(pool, FailureProbabilityUtil.ProbabilityDistribution.CONSTANT, r, realFailureRate);

					// Keep track of the failure rates generated for each (we'll reset the pool a bunch of times)
					Map<Edge, Double> edgeFailureRateMap = new HashMap<Edge, Double>();
					// Simulate a ground truth failure for each edge in the pool (but don't tell optimizer yet)
					Map<Edge, Boolean> edgeFailedMap = new HashMap<Edge, Boolean>();
					for(Edge e : pool.edgeSet()) {
						edgeFailureRateMap.put(e, e.getFailureProbability());
						edgeFailedMap.put(e, r.nextDouble() < e.getFailureProbability());
					}

					for(Double assumedFailureRate : assumedFailureRateList) {
						for(Integer chainCap : chainCapList) {
							logger.info("realFailureRate=" + realFailureRate + ", assumedFailureRate=" + assumedFailureRate + ", rep=" + (repCt+1) + "/" + numReps);

							// Lie to the optimizer about the failure rate
							FailureProbabilityUtil.setFailureProbability(pool, FailureProbabilityUtil.ProbabilityDistribution.CONSTANT, r, assumedFailureRate);

							try {
								// Solve the failure-aware objective using the wrong failure rates on edges
								CycleGenerator cg = new CycleGenerator(pool);
								List<Cycle> cycles = cg.generateCyclesAndChains(cycleCap, chainCap, true);
								Solution sol = (new CycleFormulationCPLEXSolver(
										pool, 
										cycles,
										new CycleMembership(pool, cycles)
										)
										).solve();
								double solUtil = sol.getObjectiveValue();
								cycles = null; cg = null;

								// Get the actual number of transplants realized by the algorithm
								double numActualTransplants = RematchUtil.calculateNumTransplants(sol, pool, edgeFailedMap);

								logger.info("Objective: " + solUtil);

								// Record keeping
								out.set(Col.SEED, seed);
								out.set(Col.CYCLE_CAP, cycleCap);
								out.set(Col.CHAIN_CAP, chainCap);
								out.set(Col.GENERATOR, generatorName);
								out.set(Col.TRUE_FAILURE_PROB, realFailureRate);
								out.set(Col.ASSUMED_FAILURE_PROB, assumedFailureRate);
								out.set(Col.EXPECTED_TRANSPLANTS, solUtil);
								out.set(Col.REALIZED_TRANSPLANTS, numActualTransplants);
								
								try {
									out.record();
								} catch(IOException e) {
									IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
									e.printStackTrace();
									System.exit(-1);
								}

							} catch(SolverException e) {
								e.printStackTrace();
								logger.severe("Solver Exception thrown!" + e);
							}
						} // end chainCapList
					} // end assumedFailureRateList
				} // end repCt
			} // end realFailureRateList
		} // end of matchDir


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

	public static void main(String[] args) {
		(new DriverFailureSensitivity()).run();
	}
}
