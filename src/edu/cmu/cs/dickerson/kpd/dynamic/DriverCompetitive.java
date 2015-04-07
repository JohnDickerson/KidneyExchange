package edu.cmu.cs.dickerson.kpd.dynamic;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.sun.istack.internal.logging.Logger;

import edu.cmu.cs.dickerson.kpd.dynamic.simulator.CompetitiveDynamicSimulator;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;

public class DriverCompetitive {

	private static final Logger logger = Logger.getLogger(DriverCompetitive.class);

	public static void main(String[] args) {

		Random r = new Random();

		// List of generators we want to use
		List<PoolGenerator> genList = Arrays.asList(new PoolGenerator[] {
				new SaidmanPoolGenerator(r),
				//UNOSGenerator.makeAndInitialize(IOUtil.getBaseUNOSFilePath(), ',', r),	
		});

		// List of gamma splits (vertices enter competitive pool with prob gamma, otherwise enter only one pool)
		List<Double> gammaList = Arrays.asList(new Double[] {
				0.5,
		});

		// List of alpha splits (single pool vertices enter greedy with prob alpha, otherwise enter patient pool)
		List<Double> alphaList = Arrays.asList(new Double[] {
				0.5,
		});

		// List of m parameters (for every one time period, expect m vertices to enter, Poisson process)
		List<Double> mList = Arrays.asList(new Double[] {
				2.0,
		});

		// List of lambda parameters (every vertex has lifespan of exponential clock with parameter lambda)
		List<Double> lambdaList = Arrays.asList(new Double[] {
				2.0,
		});

		// List of time limits for simulation to run
		List<Double> timeLimitList = Arrays.asList(new Double[] {
				10.0,
		});

		// Number of times to repeat experiment for all the same parameters, but new random seed
		int numReps = 1;

		// Set seeds for repetition of experiments.
		long seed = System.currentTimeMillis();  // used in entry times, exit times, and vertex-specific market entry
		
		for(PoolGenerator gen : genList) {	
			for(Double gamma : gammaList) {
				for(Double alpha : alphaList) {
					for(Double m : mList) {
						for(Double lambda : lambdaList) {
							for(Double timeLimit : timeLimitList) {
								for(int repIdx=0; repIdx < numReps; repIdx++) {
									
									seed+=1; r.setSeed(seed);
									
									CompetitiveDynamicSimulator sim = new CompetitiveDynamicSimulator(
											gamma, 
											alpha, 
											m, 
											lambda, 
											gen,
											r);
									sim.run(timeLimit);

								} // end of numReps (this should be innermost loop)
							} // end of timeLimitList
						} // end of lambdaList
					} // end of mList
				} // end of alphaList
			} // end of gammaList
		} // end of genList

		logger.info("*** DONE! ***");
	}
}
