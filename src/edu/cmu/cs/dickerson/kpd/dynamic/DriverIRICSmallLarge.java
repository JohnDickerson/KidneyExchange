package edu.cmu.cs.dickerson.kpd.dynamic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.dynamic.arrivals.ArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.dynamic.arrivals.UniformIntegerArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.dynamic.simulator.IRICDynamicSimulator;
import edu.cmu.cs.dickerson.kpd.dynamic.simulator.IRICDynamicSimulatorData;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.io.IRICHeteroOutput;
import edu.cmu.cs.dickerson.kpd.io.IRICHeteroOutput.Col;
import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;
import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital.Truthfulness;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.UNOSGenerator;

/**
 * Credit-based mechanism journal paper experiments.
 * 
 * @author Chen Hajaj
 *
 */
public class DriverIRICSmallLarge {

	public static void main(String[] args) {

		Random r = new Random();

		// List of generators we want to use
		List<PoolGenerator> genList = Arrays.asList(new PoolGenerator[] { 
				//new SaidmanPoolGenerator(r),
				UNOSGenerator.makeAndInitialize(IOUtil.getBaseUNOSFilePath(), ',', r),
			});
		
		

		// list of |H|s we'll iterate over
		List<Integer> numHospitalsList = Arrays.asList(2, 3, 4 ,5, 6/*4, 5, 6, 7,*/
		);

		// arrival rate distributions (we record distribution type and mean)
		List<ArrivalDistribution<Integer>> arrivalDistList = new ArrayList<ArrivalDistribution<Integer>>();
		//arrivalDistList.add(new UniformIntegerArrivalDistribution(1, 5));
		arrivalDistList.add(new UniformIntegerArrivalDistribution(5,15));
		arrivalDistList.add(new UniformIntegerArrivalDistribution(15, 25));

		// life expectancy distributions (we record distribution type and mean)
		List<ArrivalDistribution<Integer>> lifeExpectancyDistList = new ArrayList<ArrivalDistribution<Integer>>();
		lifeExpectancyDistList.add(new UniformIntegerArrivalDistribution(1, 1));
		// Cycle and chain limits
		List<Integer> chainCapList = Arrays.asList(4);

		// How many time periods should the simulation have?
		int simTimePeriods = 100;

		// Number of repetitions for each parameter vector
		int numReps = 100;

		// What kind of strategizing do we allow?
		Truthfulness nonTruthfulType = Truthfulness.FullyStrategic;
		// Truthfulness nonTruthfulType = Truthfulness.SemiTruthful;

		// Are we doing IR+IC+IREfficient, or IC+Efficient?
		boolean doIRICIREfficient = false;

		// Store output
		String path;
		if (doIRICIREfficient) {
			path = "iric_";
		} else {
			path = "iceff_";
		}
		path += "2_2-6_" + System.currentTimeMillis() + ".csv";

		IRICHeteroOutput out = null;
		try {
			out = new IRICHeteroOutput(path);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		long seedMain = System.currentTimeMillis();
		long seedLife = System.currentTimeMillis() + 69149;
		long seedArrival = System.currentTimeMillis() + 104711;

		for (int numHospitals : numHospitalsList) {
			for (int chainCap : chainCapList) {
				for (PoolGenerator gen : genList) {
					// for(ArrivalDistribution<Integer> arrivalDist : arrivalDistList) {
					for (ArrivalDistribution<Integer> lifeExpectancyDist : lifeExpectancyDistList) {
						ArrivalDistribution<Integer> arrivalDist1 = arrivalDistList.get(0);
						ArrivalDistribution<Integer> arrivalDist2 = arrivalDistList.get(1);

						for (int rep = 0; rep < numReps; rep++) { // must be the mos internal loop
							IOUtil.dPrintln("\n*****\nGraph (|H|=" + numHospitals + ", #" + (rep + 1) + "/" + numReps
									+ "), cap: " + chainCap + ", gen: " + gen.getClass().getSimpleName() + ", arr: "
									+ arrivalDist1.getClass().getSimpleName() + arrivalDist2.getClass().getSimpleName()
									+ ", life: " + lifeExpectancyDist.getClass().getSimpleName() + "\n*****\n");
							// Hold seeds constant across similar runs
							seedLife++; seedArrival++; seedMain++;

							int meanLifeExpectancy = lifeExpectancyDist.expectedDraw();

							// Create a set of |H| truthful hospitals, with urand arrival rates
							int totalExpectedPairsPerPeriod = 0;
							Set<Hospital> hospitals = new HashSet<Hospital>();
							for (int idx = 0; idx < numHospitals; idx++) {
								totalExpectedPairsPerPeriod += arrivalDist1.expectedDraw() + arrivalDist2.expectedDraw();
								hospitals.add(
										new Hospital(idx, arrivalDist1, lifeExpectancyDist, Truthfulness.Truthful));
							}
							for (int idx = numHospitals; idx < (numHospitals + numHospitals); idx++)
								hospitals.add(
										new Hospital(idx, arrivalDist2, lifeExpectancyDist, Truthfulness.Truthful));
							totalExpectedPairsPerPeriod /= hospitals.size();

							IRICDynamicSimulatorData truthfulRes = null; // keep track of truthful mechanism's results

							// Compare truthful vs. non-truthful reporting
							for (Boolean isTruthful : Arrays.asList(new Boolean[] { Boolean.TRUE, Boolean.FALSE })) {

								r.setSeed(seedMain);
								arrivalDist1.getRandom().setSeed(seedArrival);
								arrivalDist2.getRandom().setSeed(seedArrival);
								lifeExpectancyDist.getRandom().setSeed(seedLife);

								// Reset hospitals and set their truthfulness
								for (Hospital h : hospitals) {
									h.reset();
									if (isTruthful) {
										h.setTruthType(Truthfulness.Truthful);
									} else {
										h.setTruthType(nonTruthfulType);
									}
								}

								// Create an altruistic donor input arrival
								double pctAlts = 0.05;
								int expectedAltsPerPeriodMin = (int) Math
										.rint((pctAlts - (pctAlts * 0.5)) * totalExpectedPairsPerPeriod);
								int expectedAltsPerPeriodMax = (int) Math
										.rint((pctAlts + (pctAlts * 0.5)) * totalExpectedPairsPerPeriod);
								ArrivalDistribution<Integer> altArrivalDist = new UniformIntegerArrivalDistribution(
										expectedAltsPerPeriodMin, expectedAltsPerPeriodMax, r);

								// Create dynamic simulator
								IRICDynamicSimulator sim = new IRICDynamicSimulator(hospitals, gen, altArrivalDist,
										chainCap, meanLifeExpectancy, r, doIRICIREfficient);

								IRICDynamicSimulatorData res = null;
								try {
									// Run a bunch of times
									res = sim.run(simTimePeriods);
								} catch (SolverException e) {
									IOUtil.dPrintln("Exception: " + e);
									// System.exit(-1);
								}
								if (null == res) {
									continue;
								}
								if (isTruthful) {
									truthfulRes = res;
								}

								out.set(Col.SEED_MAIN, seedMain);
								out.set(Col.SEED_ARRIVAL, seedArrival);
								out.set(Col.SEED_LIFE, seedLife);
								out.set(Col.CYCLE_CAP, 3);
								out.set(Col.CHAIN_CAP, chainCap);
								out.set(Col.PCT_ALTRUISTS, pctAlts);
								out.set(Col.GENERATOR, gen.toString());
								out.set(Col.NUM_HOSPITALS, numHospitals);
								out.set(Col.FRAC_TRUTHFUL_HOSPITALS, isTruthful ? "1.0"
										: nonTruthfulType == Truthfulness.SemiTruthful ? "0.0" : "-1.0");
								out.set(Col.NUM_TIME_PERIODS, simTimePeriods);
								out.set(Col.ARRIVAL_DIST1, arrivalDist1);
								out.set(Col.ARRIVAL_MEAN1, arrivalDist1.expectedDraw());
								out.set(Col.ARRIVAL_DIST2, arrivalDist2);
								out.set(Col.ARRIVAL_MEAN2, arrivalDist2.expectedDraw());
								out.set(Col.LIFE_EXPECTANCY_DIST, lifeExpectancyDist);
								out.set(Col.LIFE_EXPECTANCY_MEAN, lifeExpectancyDist.expectedDraw());
								out.set(Col.NUM_MATCHED, res.getTotalNumVertsMatched());
								out.set(Col.NUM_INTERNALLY_MATCHED, res.getTotalInternalNumVertsMatched());
								out.set(Col.NUM_EXTERNALLY_MATCHED, res.getTotalExternalNumVertsMatched());

								/*// If this is the non-truthful run, figure out
								// when it was dominated by the truthful run
								if (isTruthful || null == truthfulRes) {
									out.set(Col.OVERALL_DOMINATED_TIME_PERIOD, -1);
									out.set(Col.AVG_HOSPITAL_DOMINATED_TIME_PERIOD, -1.0);
								} else {
									// Compute when the total # matches started
									// dominating
									int dominatedPeriod = DriverIRICsizes.getDominatedPeriod(
											truthfulRes.getNumMatchedSoFar(), res.getNumMatchedSoFar(), 10);
									out.set(Col.OVERALL_DOMINATED_TIME_PERIOD, dominatedPeriod);
								}*/

								// Extract matches based on hospital's size
								int hospSumS = 0;
								int hospSumL = 0;
								
								for (Hospital h : hospitals) {
									List<Integer> matches = res.getHospNumMatchSoFar().get(h);
									if (isTruthful)
										matches = truthfulRes.getHospNumMatchSoFar().get(h);
									if (h.getExpectedArrival() == arrivalDistList.get(0).expectedDraw())
										hospSumS += matches.get(99);
									else
										hospSumL += matches.get(99);

								}
								double avgHospDomTimePeriod = hospSumS / (double) numHospitals;
								out.set(Col.AVG_MATCHED_SMALL, avgHospDomTimePeriod);
								avgHospDomTimePeriod = hospSumL / (double) numHospitals;
								out.set(Col.AVG_MATCHED_LARGE, avgHospDomTimePeriod);

								// Write the row of data
								try {
									out.record();
								} catch (IOException e) {
									IOUtil.dPrintln(
											"Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
									e.printStackTrace();
									System.exit(-1);
								}

							} // end of truthful/non-truthful hospital switch

						} // numReps
					} // lifeExpectancyDistList
					// } // arrivalDistList
				} // genList
			} // chainCapList
		} // numHospitalsList

	} // end of main method

	/**
	 * Determines if one list dominates another list started at a certain time
	 * period, going on for a certain number of time periods. Returns the first
	 * index of domination (or -1).
	 * 
	 * @param dominator
	 * @param dominatee
	 * @param domMaxPeriods
	 * @return
	 */
	public static int getDominatedPeriod(List<Integer> dominator, List<Integer> dominatee, final int domMaxPeriods) {
		int dominatedCtr = 0;
		int dominatedPeriod = -1;
		for (int timeIdx = 0; timeIdx < dominator.size(); timeIdx++) {
			// If truthful>non-truthful for enough rounds, consider it dominated
			if (dominator.get(timeIdx) >= dominatee.get(timeIdx)) {
				dominatedCtr++;
			} else {
				dominatedCtr = 0;
			}
			if (dominatedCtr >= domMaxPeriods) {
				dominatedPeriod = (timeIdx - dominatedCtr + 1);
				break;
			}
		}
		return dominatedPeriod;
	}
}
