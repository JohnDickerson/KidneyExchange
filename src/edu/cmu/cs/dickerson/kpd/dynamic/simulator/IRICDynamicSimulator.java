package edu.cmu.cs.dickerson.kpd.dynamic.simulator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import edu.cmu.cs.dickerson.kpd.dynamic.arrivals.ArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.ir.ICEfficientMechanism;
import edu.cmu.cs.dickerson.kpd.ir.ICMechanism;
import edu.cmu.cs.dickerson.kpd.ir.IREfficientmMechanism;
import edu.cmu.cs.dickerson.kpd.ir.solver.IRSolution;
import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;
import edu.cmu.cs.dickerson.kpd.ir.structure.HospitalVertexInfo;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;

public class IRICDynamicSimulator extends DynamicSimulator {

	private static final Logger logger = Logger.getLogger(IRICDynamicSimulator.class.getSimpleName());

	private Set<Hospital> hospitals;
	private ICMechanism mechanism;
	private PoolGenerator poolGen;
	private ArrivalDistribution<Integer> altArrivalDist;
	private Random r;

	public IRICDynamicSimulator(Set<Hospital> hospitals, PoolGenerator poolGen, ArrivalDistribution<Integer> altArrivalDist, int chainCap, int meanLifeExpectancy, Random r, boolean doIRICMechanism) {
		super();
		this.hospitals = hospitals;
		this.poolGen = poolGen;
		this.altArrivalDist = altArrivalDist;
		this.r = r;
		
		if(doIRICMechanism) {
			this.mechanism = new IREfficientmMechanism(hospitals, 3, chainCap, meanLifeExpectancy);
		} else {
			this.mechanism = new ICEfficientMechanism(hospitals, 3, chainCap, meanLifeExpectancy);
		}
	}


	public IRICDynamicSimulatorData run(int timeLimit) throws SolverException {

		// Empty pool (add/remove vertices in the tick method)
		Pool pool = new Pool(Edge.class);
		
		// Reset instance-specific stuff, for multiple experimental runs
		for(Hospital hospital : hospitals) {
			hospital.reset();
		}
		mechanism.reset();
		IRICDynamicSimulatorData results = new IRICDynamicSimulatorData();
		
		int totalExternalNumVertsMatched = 0;
		int totalInternalNumVertsMatched = 0;
		for(int timeIdx=0; timeIdx<timeLimit; timeIdx++) {
			logger.info("\n\n*** Time period: " + timeIdx + " ***\n\n");

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
			Set<Vertex> newVerts = poolGen.addVerticesToPool(pool, totalArrivingPairs, totalArrivingAlts); 
			
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
						hospVerts.add(v);
						HospitalVertexInfo hospVertInfo = new HospitalVertexInfo();
						hospVertInfo.entranceTime = timeIdx;  // when did vertex enter pool?
						hospVertInfo.lifeExpectancy = hospital.getLifeExpectancyDist().draw();  // how long will vertex live?
						hospital.getVertexInfo().put(v, hospVertInfo);
					}
				}
				hospital.addPublicAndPrivateVertices(hospVerts);
			}
			
			// Remove any expired vertices, track ones that are about to die
			Set<Vertex> dieNextRoundVerts = new HashSet<Vertex>();
			for(Hospital h : hospitals) {
				Iterator<Vertex> hvIt = h.getPublicAndPrivateVertices().iterator();
				while(hvIt.hasNext()) {
					Vertex v = hvIt.next();
					int vertAge = timeIdx - h.getVertexInfo().get(v).entranceTime;
					if(vertAge >= h.getVertexInfo().get(v).lifeExpectancy) {
						pool.removeVertex(v);  // delete the vertex from the full pool
						hvIt.remove();  // delete vertex from hospital's private list
					} else if(vertAge+1 == h.getVertexInfo().get(v).lifeExpectancy) {
						dieNextRoundVerts.add(v);
					}
				}
			}
			
			// Evolve pool and run the IRIC Mechanism on it
			IRSolution sol = tick(pool, dieNextRoundVerts);
			totalExternalNumVertsMatched += sol.getNumMatchedByMechanism();
			totalInternalNumVertsMatched += sol.getNumMatchedInternally();
			logger.info("Time period: " + timeIdx + ", Vertices matched: " + sol.getNumMatchedByMechanism());
			
			// Record results for experimental stuff
			results.getNumMatchedSoFar().add(totalExternalNumVertsMatched+totalInternalNumVertsMatched);
			results.registerTimePeriod(sol);
			
			// Remove any matched vertices from pool + hospitals
			Set<Vertex> toRemove = new HashSet<Vertex>(Cycle.getConstituentVertices(sol.getMatching(), pool));
			for(Hospital h : hospitals) {
				Iterator<Vertex> hvIt = h.getPublicAndPrivateVertices().iterator();
				while(hvIt.hasNext()) {
					Vertex v = hvIt.next();
					if(toRemove.contains(v)) {
						hvIt.remove();
					}
				}
			}
			pool.removeAllVertices(toRemove);  // remove matched vertex from pool
			
		}
		logger.info("After " + timeLimit + " periods, matched:\n" 
				+ totalExternalNumVertsMatched + " external vertices,\n"
				+ totalInternalNumVertsMatched + " internal vertices,\n"
				+ (totalExternalNumVertsMatched+totalInternalNumVertsMatched) + " total vertices.");
		
		results.setTotalExternalNumVertsMatched(totalExternalNumVertsMatched);
		results.setTotalInternalNumVertsMatched(totalInternalNumVertsMatched);
		results.setTotalNumVertsMatched(totalExternalNumVertsMatched+totalInternalNumVertsMatched);
		return results;
				
	}

	public IRSolution tick(Pool pool, Set<Vertex> dieNextRoundVerts) throws SolverException {

		// Run the mechanism on the pool, get a matching
		IRSolution sol = mechanism.doMatching(pool, dieNextRoundVerts, this.r);
		
		return sol;
	}
	
}
