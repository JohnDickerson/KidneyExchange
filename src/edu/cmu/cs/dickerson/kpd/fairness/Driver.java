package edu.cmu.cs.dickerson.kpd.fairness;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.fairness.ExperimentalOutput.Col;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
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
import edu.cmu.cs.dickerson.kpd.structure.generator.HeterogeneousPoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SparseUNOSSaidmanPoolGenerator;

public class Driver {

	public enum RelevantGenerator { HETEROGENEOUS, SAIDMAN, SPARSE_UNOS_SAIDMAN };

	public static void main(String args[]) {

		// Random seed (recorded in experimental file for reproducibility)
		long seed = System.currentTimeMillis();

		// Number of times to run each experiment with the same parameters, except random seed
		int numRepeats = 10;

		// Are we using failure probabilities, and if so what kind?
		boolean usingFailureProbabilities = true;
		FailureProbabilityUtil.ProbabilityDistribution failDist = FailureProbabilityUtil.ProbabilityDistribution.CONSTANT;

		// Iterate over the cross product of num pairs and altruists
		List<Integer> numPairsList = Arrays.asList(10,25,50,100,150,200,250);//,500);
		//List<Double> altPctList = Arrays.asList(0.0, 0.01, 0.05, 0.10);
		List<Double> altPctList = Arrays.asList(0.0);

		// Possibly use different max cycle and chain sizes
		List<Integer> cycleCapList = Arrays.asList(3);
		List<Integer> chainCapList = Arrays.asList(0);  //4);

		// What's our threshold for high sensitization?
		List<Double> highlySensitizedThreshList = Arrays.asList(0.90);

		// Which pool generators should we test?
		List<RelevantGenerator> generatorTypeList = Arrays.asList(RelevantGenerator.SPARSE_UNOS_SAIDMAN, RelevantGenerator.HETEROGENEOUS, RelevantGenerator.SAIDMAN);
		//List<RelevantGenerator> generatorTypeList = Arrays.asList(RelevantGenerator.HETEROGENEOUS, RelevantGenerator.SAIDMAN);


		// Initialize our experimental output to .csv writer
		String path = "static_" + System.currentTimeMillis() + ".csv";
		ExperimentalOutput eOut = null;
		try {
			eOut = new ExperimentalOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		for(RelevantGenerator generatorType : generatorTypeList) {
			for(Integer numPairs : numPairsList) {
				for(Double altPct : altPctList) {
					Integer numAlts = (int) Math.round(numPairs * altPct);		
					for(Integer cycleCap : cycleCapList) {
						for(Integer chainCap : chainCapList) {

							for(Double highlySensitizedThresh : highlySensitizedThreshList) {
								for(int repeat=0; repeat<numRepeats; repeat++) {

									eOut.set(Col.START_TIME, new Date());
									eOut.set(Col.NUM_PAIRS, numPairs);
									eOut.set(Col.NUM_ALTS, numAlts);
									eOut.set(Col.CYCLE_CAP, cycleCap);
									eOut.set(Col.CHAIN_CAP, chainCap);
									eOut.set(Col.HIGHLY_SENSITIZED_CPRA, highlySensitizedThresh);
									eOut.set(Col.RANDOM_SEED, seed);
									eOut.set(Col.GENERATOR, generatorType.toString());
									eOut.set(Col.FAILURE_PROBABILITIES_USED, usingFailureProbabilities);
									eOut.set(Col.FAILURE_PROBABILITY_DIST, failDist.toString());
									
									// Generate a compatibility graph
									Random r = new Random(seed++);

									Pool pool = null;
									switch(generatorType) {
									case HETEROGENEOUS:
										pool = new HeterogeneousPoolGenerator(r).generate(numPairs, numAlts, 0.624);   // 0.624 taken from UNOS data for HIGH_CPRA
										break;
									case SPARSE_UNOS_SAIDMAN:
										pool = new SparseUNOSSaidmanPoolGenerator(r).generate(numPairs, numAlts);
										break;
									case SAIDMAN:
									default:	
										pool = new SaidmanPoolGenerator(r).generate(numPairs, numAlts);
									}
									
									// If we're setting failure probabilities, do that here:
									if(usingFailureProbabilities) {
										FailureProbabilityUtil.setFailureProbability(pool, failDist, r);
									}

									// Generate all 3-cycles and somecap-chains
									CycleGenerator cg = new CycleGenerator(pool);
									List<Cycle> cycles = cg.generateCyclesAndChains(cycleCap, chainCap, usingFailureProbabilities);

									// For each vertex, get list of cycles that contain this vertex
									CycleMembership membership = new CycleMembership(pool, cycles);

									// Split pairs into highly- and not highly-sensitized patients 
									Set<Vertex> highV = new HashSet<Vertex>();
									for(VertexPair pair : pool.getPairs()) {
										if(pair.getPatientCPRA() >= highlySensitizedThresh) {
											highV.add(pair);
										}
									}
									eOut.set(Col.HIGHLY_SENSITIZED_COUNT, highV.size());

									// Solve the model
									try {

										FairnessCPLEXSolver s = new FairnessCPLEXSolver(pool, cycles, membership, highV, usingFailureProbabilities);

										Solution alphaStarSol = s.solveForAlphaStar();
										Solution fairSol = s.solve(alphaStarSol.getObjectiveValue());
										eOut.set(Col.ALPHA_STAR, alphaStarSol.getObjectiveValue());
										eOut.set(Col.FAIR_OBJECTIVE, fairSol.getObjectiveValue());
										eOut.set(Col.FAIR_HIGHLY_SENSITIZED_MATCHED, SolutionUtils.countVertsInMatching(pool, fairSol, highV, false));
										eOut.set(Col.FAIR_TOTAL_CARDINALITY_MATCHED, SolutionUtils.countVertsInMatching(pool, fairSol, pool.vertexSet(), false));
										eOut.set(Col.FAIR_EXPECTED_HIGHLY_SENSITIZED_MATCHED, SolutionUtils.countExpectedTransplantsInMatching(pool, fairSol, highV));
										eOut.set(Col.FAIR_EXPECTED_TOTAL_CARDINALITY_MATCHED, SolutionUtils.countExpectedTransplantsInMatching(pool, fairSol, pool.vertexSet()));
										
										Solution unfairSol = s.solve(0.0);
										eOut.set(Col.UNFAIR_OBJECTIVE, unfairSol.getObjectiveValue());
										eOut.set(Col.UNFAIR_HIGHLY_SENSITIZED_MATCHED, SolutionUtils.countVertsInMatching(pool, unfairSol, highV, false));
										eOut.set(Col.UNFAIR_TOTAL_CARDINALITY_MATCHED, SolutionUtils.countVertsInMatching(pool, unfairSol, pool.vertexSet(), false));
										eOut.set(Col.UNFAIR_EXPECTED_HIGHLY_SENSITIZED_MATCHED, SolutionUtils.countExpectedTransplantsInMatching(pool, unfairSol, highV));
										eOut.set(Col.UNFAIR_EXPECTED_TOTAL_CARDINALITY_MATCHED, SolutionUtils.countExpectedTransplantsInMatching(pool, unfairSol, pool.vertexSet()));
										
										IOUtil.dPrintln("Solved main IP with objective: " + fairSol.getObjectiveValue());
										IOUtil.dPrintln("Without alpha, would've been:  " + unfairSol.getObjectiveValue());

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

								} // highlySensitizedThresh & highlySensitizedThreshList
							} // chainCap & chainCapList
						} // cycleCap & cycleCapList
					} // altPct & altPctList
				} // numPairs & numPairsList
			} // repeat & numRepeats
		} // generatorType & generatorTypeList

		// Flush and kill the CSV writer
		if(null != eOut) {
			try {
				eOut.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		IOUtil.dPrintln("Done with simulator runs!");
		return;
	}
}
