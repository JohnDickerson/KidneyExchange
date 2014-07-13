package edu.cmu.cs.dickerson.kpd.dynamic.simulator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.sun.istack.internal.logging.Logger;

import edu.cmu.cs.dickerson.kpd.ir.IRICMechanism;
import edu.cmu.cs.dickerson.kpd.ir.arrivals.ArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.ir.arrivals.UniformArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.ir.solver.IRSolution;
import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;

public class IRICDynamicSimulator extends DynamicSimulator {

	private static final Logger logger = Logger.getLogger(IRICDynamicSimulator.class);

	private Set<Hospital> hospitals;
	private IRICMechanism mechanism;
	private PoolGenerator poolGen;
	private ArrivalDistribution altArrivalDist;
	private Random r;

	public IRICDynamicSimulator(Set<Hospital> hospitals, PoolGenerator poolGen, ArrivalDistribution altArrivalDist, Random r) {
		super();
		this.hospitals = hospitals;
		this.mechanism = new IRICMechanism(hospitals, 3, 0);
		this.poolGen = poolGen;
		this.altArrivalDist = altArrivalDist;
		this.r = r;
	}


	public void run(int timeLimit) {

		// Empty pool (add/remove vertices in the tick method)
		Pool pool = new Pool(Edge.class);
		
		for(Hospital hospital : hospitals) {
			hospital.setNumCredits(0);
		}
		
		int totalExternalNumVertsMatched = 0;
		int totalInternalNumVertsMatched = 0;
		for(int timeIdx=0; timeIdx<timeLimit; timeIdx++) {
			logger.info("Time period: " + timeIdx);

			// Draw new vertex counts for each of the hospitals
			int totalArrivingPairs = 0;
			Map<Hospital, Integer> vertArrivalMap = new HashMap<Hospital, Integer>();
			for(Hospital hospital : hospitals) {
				int newVertCt = hospital.drawArrival();
				vertArrivalMap.put(hospital, newVertCt);
				totalArrivingPairs += newVertCt;
			}

			// Draw new vertex count for altruists coming in this time period
			int totalArrivingAlts = altArrivalDist.draw();
			
			// Generate a pool and assign vertices to each hospital
			Set<Vertex> newVerts = poolGen.addVerticesToPool(pool, totalArrivingPairs, totalArrivingAlts); // need to change this if we starting using UNOS Gen (because altruists)
			Iterator<Vertex> vIt = newVerts.iterator();
			for(Hospital hospital : hospitals) {
				int newVertCt = vertArrivalMap.get(hospital);
				Set<Vertex> hospVerts = new HashSet<Vertex>();
				while(hospVerts.size() < newVertCt) {
					Vertex v = vIt.next();
					if(v.isAltruist()) {
						// All altruists are assumed to be public always, and stay forever
					} else {
						// Add pairs only to hospital, privately
						hospVerts.add(vIt.next());
					}
				}
				hospital.addPublicAndPrivateVertices(hospVerts);
			}
			
			// Evolve pool and run the IRIC Mechanism on it
			IRSolution sol = tick(pool);

			totalExternalNumVertsMatched += sol.getNumMatchedByMechanism();
			totalInternalNumVertsMatched += sol.getNumMatchedInternally();
			logger.info("Time period: " + timeIdx + ", Vertices matched: " + sol.getNumMatchedByMechanism());
		}
		logger.info("After " + timeLimit + " periods, matched:\n" 
				+ totalExternalNumVertsMatched + " external vertices,\n"
				+ totalInternalNumVertsMatched + " internal vertices,\n"
				+ (totalExternalNumVertsMatched+totalInternalNumVertsMatched) + " total vertices.");

	}

	public IRSolution tick(Pool pool) {

		// Run the mechanism on the pool, get a matching
		IRSolution sol = mechanism.doMatching(pool, this.r);
		
		return sol;
	}
	

	public static void main(String[] args) {

		long seed = 1234;
		Random r = new Random(seed);

		// Create a set of 3 truthful hospitals, with urand arrival rates
		int totalExpectedPairsPerPeriod = 0;
		Set<Hospital> hospitals = new HashSet<Hospital>();
		for(int idx=0; idx<3; idx++) {
			ArrivalDistribution arrivalDist = new UniformArrivalDistribution(50,75,r);
			ArrivalDistribution lifeExpectancyDist = new UniformArrivalDistribution(1,12,r);
			totalExpectedPairsPerPeriod += arrivalDist.expectedDraw();
			hospitals.add( new Hospital(idx, arrivalDist, lifeExpectancyDist, true) );
		}
		totalExpectedPairsPerPeriod /= hospitals.size();

		// Create an altruistic donor input arrival
		double pctAlts = 0.05;
		int expectedAltsPerPeriodMin = (int) Math.rint(pctAlts - (pctAlts*0.5) * totalExpectedPairsPerPeriod);
		int expectedAltsPerPeriodMax = (int) Math.rint(pctAlts + (pctAlts*0.5) * totalExpectedPairsPerPeriod);
		ArrivalDistribution altArrivalDist = new UniformArrivalDistribution(expectedAltsPerPeriodMin, expectedAltsPerPeriodMax, r);

		
		// D
		IRICDynamicSimulator sim = new IRICDynamicSimulator(hospitals, new SaidmanPoolGenerator(r), altArrivalDist, r);
		sim.run(3);
	}
}
