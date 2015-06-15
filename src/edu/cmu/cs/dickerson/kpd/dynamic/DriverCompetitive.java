package edu.cmu.cs.dickerson.kpd.dynamic;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import edu.cmu.cs.dickerson.kpd.competitive.MatchingStrategy;
import edu.cmu.cs.dickerson.kpd.competitive.MaxWeightMatchingStrategy;
import edu.cmu.cs.dickerson.kpd.competitive.UniformRandomMatchingStrategy;
import edu.cmu.cs.dickerson.kpd.dynamic.simulator.CompetitiveDynamicSimulator;
import edu.cmu.cs.dickerson.kpd.dynamic.simulator.CompetitiveDynamicSimulatorData;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.io.CompetitiveOutput;
import edu.cmu.cs.dickerson.kpd.io.CompetitiveOutput.Col;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.UNOSGenerator;


/**
 * Driver for preliminary competitive exchange experiments for <i>AMMA-2015</i>
 * submission with Sanmay Das, Zhuoshi Li, and Tuomas Sandholm.
 * 
 * @author John P. Dickerson
 *
 */
public class DriverCompetitive {

	private static final Logger logger = Logger.getLogger(DriverCompetitive.class.getSimpleName());

	public static void main(String[] args) {

		Random rPool = new Random();
		Random rDynamic = new Random();
		Random rMatching = new Random();

		int cycleCap = 3;
		int chainCap = 0;  // doesn't work for chains right now (no altruists ever enter, cycle gen untested & probably breaks)

		// List of generators we want to use
		List<PoolGenerator> genList = Arrays.asList(new PoolGenerator[] {
				//new SaidmanPoolGenerator(rPool),
				UNOSGenerator.makeAndInitialize(IOUtil.getBaseUNOSFilePath(), ',', rPool),	
		});

		// List of gamma splits (vertices enter competitive pool with prob gamma, otherwise enter only one pool)
		List<Double> gammaList = Arrays.asList(new Double[] {
				//0.0, 0.1, 0.3, 0.5, 0.7, 
				0.9, 1.0,
		});

		// List of alpha splits (single pool vertices enter greedy with prob alpha, otherwise enter patient pool)
		List<Double> alphaList = Arrays.asList(new Double[] {
				0.0, 0.01, 0.1, 0.3, 0.5, 0.7, 0.9, 1.0,
		});

		// List of m parameters (for every one time period, expect m vertices to enter, Poisson process)
		List<Double> mList = Arrays.asList(new Double[] {
				//0.1, 0.2, 0.5, 1.0, 2.0,
				100.0,
		});

		// List of lambda parameters (every vertex has lifespan of exponential clock with parameter lambda)
		List<Double> lambdaList = Arrays.asList(new Double[] {
				//0.025, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 
				1.0, 
				//1.5, 2.0,
		});

		// List of time limits for simulation to run
		List<Double> timeLimitList = Arrays.asList(new Double[] {
				// Zhuoshu is using:	for m=1000, lambda=1.0, time_periods=100
				//						for m=10,   lambda=1.0, time_periods=5000
				100.0,
		});

		List<MatchingStrategy> matchingStrategyList = Arrays.asList(new MatchingStrategy[] {
				new UniformRandomMatchingStrategy(cycleCap, chainCap, false, false, 0, rMatching),
				new MaxWeightMatchingStrategy(cycleCap, chainCap, false, false, 0, rMatching),
		});

		// Number of times to repeat experiment for all the same parameters, but new random seed
		int numReps = 100;

		String path = "competitive_" + System.currentTimeMillis() + ".csv";
		CompetitiveOutput out = null;
		try {
			out = new CompetitiveOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		// Set seeds for repetition of experiments.
		long seedPool = System.currentTimeMillis();				// used for the pool generators
		long seedDynamic = System.currentTimeMillis()+69149;	// used in entry times, exit times, and vertex-specific market entry
		long seedMatching = System.currentTimeMillis()+104711;	// used for randomized matching strategies

		// How many runs will we be doing in total?
		long totalNumRuns = genList.size() * 
				gammaList.size() * 
				alphaList.size() * 
				mList.size() * 
				lambdaList.size() * 
				timeLimitList.size() * 
				matchingStrategyList.size() *
				numReps;
		long currentNumRun = 0;
				
		for(PoolGenerator gen : genList) {	
			for(Double gamma : gammaList) {
				for(Double alpha : alphaList) {
					for(Double m : mList) {
						for(Double lambda : lambdaList) {
							for(Double timeLimit : timeLimitList) {
								for(int repIdx=0; repIdx < numReps; repIdx++) {
									seedDynamic+=1; 
									seedPool+=1;
									seedMatching+=1; 
									
									for(MatchingStrategy matchingStrategy : matchingStrategyList) {
						
										// Reset seeds for all the different random number generators
										rPool.setSeed(seedPool);
										rDynamic.setSeed(seedDynamic);
										rMatching.setSeed(seedMatching);

										// Record the parameters for this specific run
										out.set(Col.SEED_POOL, seedPool);
										out.set(Col.SEED_DYNAMIC, seedDynamic);
										out.set(Col.SEED_MATCHING, seedMatching);
										out.set(Col.CYCLE_CAP, cycleCap);
										out.set(Col.CHAIN_CAP, chainCap);
										out.set(Col.GENERATOR, gen);
										out.set(Col.GAMMA, gamma);
										out.set(Col.ALPHA, alpha);
										out.set(Col.M, m);
										out.set(Col.LAMBDA, lambda);
										out.set(Col.TIME_LIMIT, timeLimit);
										out.set(Col.MATCHING_STRATEGY, matchingStrategy);

										// Run the simulation for these parameters 
										CompetitiveDynamicSimulator sim = new CompetitiveDynamicSimulator(
												gamma, 
												alpha, 
												m, 
												lambda, 
												gen,
												matchingStrategy,
												rDynamic);
										logger.info("Starting run " + (currentNumRun++) + " / " + totalNumRuns + "; " + (100.0 * ((double) currentNumRun/totalNumRuns)) + "% done.");
										CompetitiveDynamicSimulatorData runData = sim.run(timeLimit);

										// Record the statistics from this one run
										out.set(Col.TOTAL_SEEN, runData.getTotalVerticesSeen());
										out.set(Col.TOTAL_MATCHED, runData.getTotalVerticesMatched());
										out.set(Col.TOTAL_GREEDY_MATCHED, runData.getTotalVerticesMatchedByGreedy());
										out.set(Col.TOTAL_PATIENT_MATCHED, runData.getTotalVerticesMatchedByPatient());
										out.set(Col.TOTAL_EXPIRED, runData.getTotalVerticesExpired());

										// Write this record as one row of data
										try {
											out.record();
										} catch(IOException e) {
											IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
											e.printStackTrace();
											System.exit(-1);
										}

									} // end of matchingStrategyList
								} // end of numReps (anything above this will have the same entry/exit times for vertices and the same generated vertices)
							} // end of timeLimitList
						} // end of lambdaList
					} // end of mList
				} // end of alphaList
			} // end of gammaList
		} // end of genList

		logger.info("*** DONE! ***");
	}
}
