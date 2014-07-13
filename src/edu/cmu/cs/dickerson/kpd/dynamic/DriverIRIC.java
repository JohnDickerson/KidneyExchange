package edu.cmu.cs.dickerson.kpd.dynamic;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.dynamic.simulator.IRICDynamicSimulator;
import edu.cmu.cs.dickerson.kpd.ir.arrivals.ArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.ir.arrivals.UniformArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;

/**
 * Credit-based mechanism paper experiments.
 * @author John P Dickerson
 *
 */
public class DriverIRIC {

	public static void main(String[] args) {

		long seed = 1234;
		Random r = new Random(seed);

		// For now, assume all patients entering any hospital have same distribution of life expectancy
		ArrivalDistribution lifeExpectancyDist = new UniformArrivalDistribution(1,5,r);
		int meanLifeExpectancy = lifeExpectancyDist.expectedDraw();
		
		// Global chain length cap
		int chainCap = 0;
		
		// Create a set of 3 truthful hospitals, with urand arrival rates
		int totalExpectedPairsPerPeriod = 0;
		Set<Hospital> hospitals = new HashSet<Hospital>();
		for(int idx=0; idx<3; idx++) {
			ArrivalDistribution arrivalDist = new UniformArrivalDistribution(10,25,r);
			totalExpectedPairsPerPeriod += arrivalDist.expectedDraw();
			hospitals.add( new Hospital(idx, arrivalDist, lifeExpectancyDist, false) );
		}
		totalExpectedPairsPerPeriod /= hospitals.size();

		// Create an altruistic donor input arrival
		double pctAlts = 0.05;
		int expectedAltsPerPeriodMin = (int) Math.rint((pctAlts - (pctAlts*0.5)) * totalExpectedPairsPerPeriod);
		int expectedAltsPerPeriodMax = (int) Math.rint((pctAlts + (pctAlts*0.5)) * totalExpectedPairsPerPeriod);
		ArrivalDistribution altArrivalDist = new UniformArrivalDistribution(expectedAltsPerPeriodMin, expectedAltsPerPeriodMax, r);

		
		// Create dynamic simulator
		IRICDynamicSimulator sim = new IRICDynamicSimulator(hospitals, 
				new SaidmanPoolGenerator(r), 
				altArrivalDist, 
				chainCap,
				meanLifeExpectancy, 
				r);
		
		// Run a bunch of times
		sim.run(50);
	}
}
