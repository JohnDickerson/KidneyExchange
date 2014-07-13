package edu.cmu.cs.dickerson.kpd.fairness;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.fairness.alg.FairnessUtil;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.io.ExperimentalOutput;
import edu.cmu.cs.dickerson.kpd.io.ExperimentalOutput.Col;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.solver.solution.SolutionUtils;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSLoader;
import edu.cmu.cs.dickerson.kpd.structure.real.exception.LoaderException;

public class DriverUNOS {


	public static void main(String args[]) {

		// Possibly use different max cycle and chain sizes
		List<Integer> cycleCapList = Arrays.asList(3);
		List<Integer> chainCapList = Arrays.asList(7);//Integer.MAX_VALUE);

		// Number of times to run each experiment with the same parameters, except random seed
		int numRepeats = 1;   // need >1 if we're doing, e.g., bimodal probabilities

		// We value a highly-sensitized candidate at (1+alpha), whereas a normal candidate is just value 1
		List<Double> alphaStarValList = new ArrayList<Double>();
		for(double alphaStarVal=0.0; alphaStarVal<=0.0; alphaStarVal += 0.05) {
			alphaStarValList.add(alphaStarVal);
		}

		// Random seed (recorded in experimental file for reproducibility) -- used for failure probabilities
		long seed = System.currentTimeMillis();

		// Are we using failure probabilities, and if so what kind?
		boolean usingFailureProbabilities = false;
		FailureProbabilityUtil.ProbabilityDistribution failDist = FailureProbabilityUtil.ProbabilityDistribution.CONSTANT;
		if(!usingFailureProbabilities) {
			failDist = FailureProbabilityUtil.ProbabilityDistribution.NONE;
		}
		
		// Initialize our experimental output to .csv writer
		String path = "unos_nonlex_" + System.currentTimeMillis() + ".csv";
		ExperimentalOutput eOut = null;
		try {
			eOut = new ExperimentalOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		// Want to solve for each match run in a master directory consisting of single match directories
		//String baseUNOSpath = "C:\\amem\\kpd\\files_real_runs\\zips\\";
		//String baseUNOSpath = "/usr0/home/jpdicker/amem/kpd/files_real_runs/zips/";
		String baseUNOSpath = "/Users/spook/amem/kpd/files_real_runs/zips/";
		File baseUNOSDir = new File(baseUNOSpath);
		List<File> matchDirList = Arrays.asList(baseUNOSDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return file.isDirectory() && !name.toLowerCase().endsWith(".zip");
			}
		}));

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

			// Load the pool from donor, recipient, edge files
			Pool pool = null;
			try {
				pool = loader.loadFromFile(donorFilePath, recipientFilePath, edgeFilePath);
			} catch(LoaderException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			Integer numPairs = pool.getNumPairs();
			Integer numAlts = pool.getNumAltruists();
			double highlySensitizedThresh = 0.8;   // UNOS data is explicitly marked as highly or not highly sensitized   

			//IOUtil.dPrintln("% Highly-sensitized O-AB Pairs: " + FairnessUtil.getOnlyHighlySensitizedPairs(pool.getPairsOfType(BloodType.O, BloodType.AB), highlySensitizedThresh).size() / (double) pool.getNumPairs());
			
			for(int repeatIdx=0; repeatIdx<numRepeats; repeatIdx++) {

				// Generate a compatibility graph
				Random r = new Random(++seed);

				// If we're setting failure probabilities, do that here:
				if(usingFailureProbabilities) {
					FailureProbabilityUtil.setFailureProbability(pool, failDist, r);
				}

				for(Double alphaStarVal : alphaStarValList) {

					for(Integer cycleCap : cycleCapList) {
						for(Integer chainCap : chainCapList) {

							eOut.set(Col.START_TIME, new Date());
							eOut.set(Col.NUM_PAIRS, numPairs);
							eOut.set(Col.NUM_ALTS, numAlts);
							eOut.set(Col.CYCLE_CAP, cycleCap);
							eOut.set(Col.CHAIN_CAP, chainCap);
							eOut.set(Col.HIGHLY_SENSITIZED_CPRA, highlySensitizedThresh);
							eOut.set(Col.RANDOM_SEED, seed);
							eOut.set(Col.GENERATOR, matchRunID);
							eOut.set(Col.FAILURE_PROBABILITIES_USED, usingFailureProbabilities);
							eOut.set(Col.FAILURE_PROBABILITY_DIST, failDist.toString());

							IOUtil.dPrintln("---: a* = " + alphaStarVal + ", looking at UNOS file: " + matchRunID);

							// Split pairs into highly- and not highly-sensitized patients 
							Set<Vertex> highV = new HashSet<Vertex>();
							for(VertexPair pair : pool.getPairs()) {
								if(pair.getPatientCPRA() >= highlySensitizedThresh) {
									highV.add(pair);
								}
							}
							eOut.set(Col.HIGHLY_SENSITIZED_COUNT, highV.size());

							// Set weights of edges targeting non-highly-sensitized patients to 1.0, and
							// edges targeting highly-sensitized patients to (1.0 + alphaStar)
							eOut.set(Col.ALPHA_STAR, alphaStarVal);
							FairnessUtil.setFairnessEdgeWeights(pool, alphaStarVal, highV);
							
							// Generate all 3-cycles and somecap-chains
							CycleGenerator cg = new CycleGenerator(pool);
							List<Cycle> cycles = cg.generateCyclesAndChains(cycleCap, chainCap, usingFailureProbabilities);

							// For each vertex, get list of cycles that contain this vertex
							CycleMembership membership = new CycleMembership(pool, cycles);

							// Solve the model
							try {

								// We can just use the basic cycle formulation solver here --
								//  - no failure case: the weights of cycles and chains take 1+alphaStar into account during generation
								//  - failure case: utilities of cycles/chains take adjusted edge weights into account, too
								CycleFormulationCPLEXSolver s = new CycleFormulationCPLEXSolver(pool, cycles, membership);

								Solution fairSol = s.solve();
								eOut.set(Col.FAIR_OBJECTIVE, fairSol.getObjectiveValue());
								eOut.set(Col.FAIR_HIGHLY_SENSITIZED_MATCHED, SolutionUtils.countVertsInMatching(pool, fairSol, highV, false));
								eOut.set(Col.FAIR_TOTAL_CARDINALITY_MATCHED, SolutionUtils.countVertsInMatching(pool, fairSol, pool.vertexSet(), false));
								eOut.set(Col.FAIR_EXPECTED_HIGHLY_SENSITIZED_MATCHED, SolutionUtils.countExpectedTransplantsInMatching(pool, fairSol, highV));
								eOut.set(Col.FAIR_EXPECTED_TOTAL_CARDINALITY_MATCHED, SolutionUtils.countExpectedTransplantsInMatching(pool, fairSol, pool.vertexSet()));
								
								//Solution unfairSol = s.solve(0.0);
								//eOut.set(Col.UNFAIR_OBJECTIVE, unfairSol.getObjectiveValue());
								//eOut.set(Col.UNFAIR_HIGHLY_SENSITIZED_MATCHED, SolutionUtils.countVertsInMatching(pool, unfairSol, highV, false));
								//eOut.set(Col.UNFAIR_TOTAL_CARDINALITY_MATCHED, SolutionUtils.countVertsInMatching(pool, unfairSol, pool.vertexSet(), false));
								//eOut.set(Col.UNFAIR_EXPECTED_HIGHLY_SENSITIZED_MATCHED, SolutionUtils.countExpectedTransplantsInMatching(pool, unfairSol, highV));
								//eOut.set(Col.UNFAIR_EXPECTED_TOTAL_CARDINALITY_MATCHED, SolutionUtils.countExpectedTransplantsInMatching(pool, unfairSol, pool.vertexSet()));


								IOUtil.dPrintln("Solved main IP with objective: " + fairSol.getObjectiveValue());

							} catch(SolverException e) {
								e.printStackTrace();
							}


							// Write the experimental row of data
							try {
								eOut.record();
							} catch(IOException e) {
								IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
								e.printStackTrace();
								return;
							}

						} // chainCap & chainCapList
					} // cycleCap & cycleCapList
				} // alphaStarVal & alphaStarValList
			} // repeatIdx & numRepeats		
		} // matchDir & matchDirList

		// Flush and kill the CSV writer
		if(null != eOut) {
			try {
				eOut.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		IOUtil.dPrintln("All done with UNOS runs!");

		return;
	}
}
