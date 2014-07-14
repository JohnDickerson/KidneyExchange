package edu.cmu.cs.dickerson.kpd.dynamic;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.dynamic.simulator.IRICDynamicSimulator;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.io.IRICOutput;
import edu.cmu.cs.dickerson.kpd.io.IRICOutput.Col;
import edu.cmu.cs.dickerson.kpd.ir.arrivals.ArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.ir.arrivals.UniformArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;

/**
 * Credit-based mechanism paper experiments.
 * @author John P Dickerson
 *
 */
public class DriverIRIC {

	public static void main(String[] args) {

		Random r = new Random();

		// List of generators we want to use
		List<PoolGenerator> genList = Arrays.asList(new PoolGenerator[] {
				new SaidmanPoolGenerator(r),
		});

		// list of |H|s we'll iterate over
		List<Integer> numHospitalsList = Arrays.asList(new Integer[] {
				2, 3, 4, 5, 10, 15, 20,
		});

		// arrival rate distributions (we record distribution type and mean)
		List<ArrivalDistribution> arrivalDistList = Arrays.asList(new ArrivalDistribution[] {
				new UniformArrivalDistribution(5,15),
				new UniformArrivalDistribution(15,25),
				new UniformArrivalDistribution(30,50),
		});

		// life expectancy distributions (we record distribution type and mean)
		List<ArrivalDistribution> lifeExpectancyDistList = Arrays.asList(new ArrivalDistribution[] {
				new UniformArrivalDistribution(1,1), // die after one round
				new UniformArrivalDistribution(1,11),
				new UniformArrivalDistribution(1,21),
				new UniformArrivalDistribution(1,31),
		});

		// Cycle and chain limits
		List<Integer> chainCapList = Arrays.asList(new Integer[] {
				0,
				4,
		});

		// How many time periods should the simulation have?
		int simTimePeriods = 100;
						
		// Number of repetitions for each parameter vector
		int numReps = 25; 

		
		// Store output
		String path = "iric_" + System.currentTimeMillis() + ".csv";
		IRICOutput out = null;
		try {
			out = new IRICOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		long seedMain = System.currentTimeMillis();
		long seedLife = System.currentTimeMillis()+69149;
		long seedArrival = System.currentTimeMillis()+104711;
		
		for(int numHospitals : numHospitalsList) {
			for(int chainCap : chainCapList) {
				for(PoolGenerator gen : genList) {
					for(ArrivalDistribution arrivalDist : arrivalDistList) {
						for(ArrivalDistribution lifeExpectancyDist : lifeExpectancyDistList) {


							for(int rep=0; rep<numReps; rep++) {   // must be the most internal loop

								IOUtil.dPrintln("\n*****\nGraph (|H|=" + numHospitals + ", #" + (rep+1) + "/" + numReps + "), cap: " + chainCap+ ", gen: " + gen.getClass().getSimpleName() + ", arr: " + arrivalDist.getClass().getSimpleName() + ", life: " + lifeExpectancyDist.getClass().getSimpleName() +"\n*****\n");

								// Hold seeds constant across similar runs
								seedLife++; seedArrival++; seedMain++;
								
								// Compare truthful vs. non-truthful reporting
								for(Boolean isTruthful : Arrays.asList(new Boolean[] { Boolean.TRUE, Boolean.FALSE })) {
									
									r.setSeed(seedMain);
									arrivalDist.getRandom().setSeed(seedArrival);
									lifeExpectancyDist.getRandom().setSeed(seedLife);
									
									int meanLifeExpectancy = lifeExpectancyDist.expectedDraw();

									// Create a set of |H| truthful hospitals, with urand arrival rates
									int totalExpectedPairsPerPeriod = 0;
									Set<Hospital> hospitals = new HashSet<Hospital>();
									for(int idx=0; idx<numHospitals; idx++) {
										totalExpectedPairsPerPeriod += arrivalDist.expectedDraw();
										hospitals.add( new Hospital(idx, arrivalDist, lifeExpectancyDist, isTruthful) );
									}
									totalExpectedPairsPerPeriod /= hospitals.size();
									
									// Create an altruistic donor input arrival
									double pctAlts = 0.05;
									int expectedAltsPerPeriodMin = (int) Math.rint((pctAlts - (pctAlts*0.5)) * totalExpectedPairsPerPeriod);
									int expectedAltsPerPeriodMax = (int) Math.rint((pctAlts + (pctAlts*0.5)) * totalExpectedPairsPerPeriod);
									ArrivalDistribution altArrivalDist = new UniformArrivalDistribution(expectedAltsPerPeriodMin, expectedAltsPerPeriodMax, r);


									// Create dynamic simulator
									IRICDynamicSimulator sim = new IRICDynamicSimulator(hospitals, 
											gen, 
											altArrivalDist, 
											chainCap,
											meanLifeExpectancy, 
											r);

									// Run a bunch of times
									int numMatched = sim.run(simTimePeriods);
									
									
									out.set(Col.CYCLE_CAP, 3);
									out.set(Col.CHAIN_CAP, chainCap);
									out.set(Col.GENERATOR, gen.toString());
									out.set(Col.NUM_HOSPITALS, numHospitals);
									out.set(Col.FRAC_TRUTHFUL_HOSPITALS, isTruthful ? "1.0" : "0.0");
									out.set(Col.NUM_TIME_PERIODS, simTimePeriods);
									out.set(Col.ARRIVAL_DIST, arrivalDist);
									out.set(Col.ARRIVAL_MEAN, arrivalDist.expectedDraw());
									out.set(Col.LIFE_EXPECTANCY_DIST, lifeExpectancyDist);
									out.set(Col.LIFE_EXPECTANCY_MEAN, lifeExpectancyDist.expectedDraw());
									out.set(Col.NUM_MATCHED, numMatched);
									
									// Write the  row of data
									try {
										out.record();
									} catch(IOException e) {
										IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
										e.printStackTrace();
										System.exit(-1);
									}
									
								} // end of truthful/non-truthful hospital switch
								
								
							} // numReps
						} // lifeExpectancyDistList
					} // arrivalDistList
				} // genList
			} // chainCapList
		} // numHospitalsList

	} // end of main method
	
}