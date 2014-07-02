package edu.cmu.cs.dickerson.kpd.dynamic.simulator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.sun.istack.internal.logging.Logger;

import edu.cmu.cs.dickerson.kpd.ir.IRICMechanism;
import edu.cmu.cs.dickerson.kpd.ir.arrivals.UniformArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.ir.solver.IRSolution;
import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;

public class IRICDynamicSimulator extends DynamicSimulator {

	private static final Logger logger = Logger.getLogger(IRICDynamicSimulator.class);

	private Set<Hospital> hospitals;
	private IRICMechanism mechanism;
	private PoolGenerator poolGen;
	private Random r;

	public IRICDynamicSimulator(Set<Hospital> hospitals, PoolGenerator poolGen, Random r) {
		super();
		this.hospitals = hospitals;
		this.mechanism = new IRICMechanism(hospitals, 3, 0);
		this.poolGen = poolGen;
		this.r = r;
	}


	public void run(int timeLimit) {

		int totalExternalNumVertsMatched = 0;
		int totalInternalNumVertsMatched = 0;
		for(int timeIdx=0; timeIdx<timeLimit; timeIdx++) {
			logger.info("Time period: " + timeIdx);

			// Draw new vertex counts for each of the hospitals
			int totalArrivingVerts = 0;
			Map<Hospital, Integer> vertArrivalMap = new HashMap<Hospital, Integer>();
			for(Hospital hospital : hospitals) {
				int newVertCt = hospital.drawArrival();
				vertArrivalMap.put(hospital, newVertCt);
				totalArrivingVerts += newVertCt;
			}

			// Generate a pool and assign vertices to each hospital
			Pool pool = poolGen.generate(totalArrivingVerts, 0);   // need to change this if we starting using UNOS Gen (because altruists)
			Iterator<Vertex> vIt = pool.vertexSet().iterator();
			for(Hospital hospital : hospitals) {
				int newVertCt = vertArrivalMap.get(hospital);
				Set<Vertex> hospVerts = new HashSet<Vertex>();
				while(hospVerts.size() < newVertCt) {
					hospVerts.add(vIt.next());
				}
				hospital.setPublicAndPrivateVertices(hospVerts);
			}

			// Run the IRIC Mechanism on this pool
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
		IRSolution sol = mechanism.doMatching(pool, this.r);
		return sol;
	}

	public static void main(String[] args) {

		Random r = new Random(1234);

		// Create a set of 3 truthful hospitals, with urand arrival rates
		Set<Hospital> hospitals = new HashSet<Hospital>();
		for(int idx=0; idx<3; idx++) {
			hospitals.add( new Hospital(idx, new UniformArrivalDistribution(50,75,r), true) );
		}

		IRICDynamicSimulator sim = new IRICDynamicSimulator(hospitals, new SaidmanPoolGenerator(r), r);
		sim.run(3);
	}
}
