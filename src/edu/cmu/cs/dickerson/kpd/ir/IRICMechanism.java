package edu.cmu.cs.dickerson.kpd.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverRuntimeException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class IRICMechanism {

	private Set<Hospital> hospitals;
	private int cycleCap = 3;
	private int chainCap = 4;
	
	public IRICMechanism(Set<Hospital> hospitals) {
		this(hospitals, 3, 4);   // default to 3-cycles and 4-chains, from UNOS KPDPP in summer 2014
	}
	
	public IRICMechanism(Set<Hospital> hospitals, int cycleCap, int chainCap) {
		this.hospitals = hospitals;	
		this.cycleCap = cycleCap;  // internal and external matching cycle limit
		this.chainCap = chainCap;  // internal and external matching chain limit
		for(Hospital hospital : hospitals) { hospital.setNumCredits(0); }   // all hospitals start out with no history
	}
	
	public Solution doMatching(Pool entirePool) {
		
		//
		// Initial credit balance update based on reported types
		Map<Hospital, HospitalInfo> infoMap = new HashMap<Hospital, HospitalInfo>();
		for(Hospital hospital : hospitals) {
			
			// Ask the hospital for its reported type
			Pool reportedInternalPool = entirePool.makeSubPool( hospital.getPublicVertexSet() );
			
			// Update hospital's credits based on reported type
			// c_i += 4 * k_i * ( |reported| - |expected| )
			int expectedType = hospital.getExpectedArrival();
			hospital.addCredits( 4*expectedType * (reportedInternalPool.vertexSet().size() - expectedType) );
			
			// Figure out a maximum utility internal match on reported type
			Solution internalMatch = null;
			try {
				CycleGenerator cg = new CycleGenerator(reportedInternalPool);
				List<Cycle> internalCycles = cg.generateCyclesAndChains(cycleCap, chainCap, false);
				internalMatch =
					(new CycleFormulationCPLEXSolver(reportedInternalPool, internalCycles, 
							new CycleMembership(reportedInternalPool, internalCycles))).solve();
				cg = null; internalCycles = null;
				System.gc();
			} catch(SolverException e) {
				throw new SolverRuntimeException("Unrecoverable error solving cycle packing problem on reported pool of " + hospital + "; experiments are bunk.");
			}
			
			// Record details
			HospitalInfo hospitalInfo = new HospitalInfo();
			hospitalInfo.reportedInternalPool = reportedInternalPool;
			hospitalInfo.maxReportedInternalMatchSize = Cycle.getConstituentVertices(
					internalMatch.getMatching(), reportedInternalPool).size();  // recording match SIZE, not UTILITY [for now]
			infoMap.put(hospital, hospitalInfo);
		}
		
		
		// Get maximum matching subject to each hospital getting at least as many matches
		// as it could've gotten if had only matched its reported pairs alone
		
		
		// Random permutation of hospitals
		List<Hospital> shuffledHospitals = new ArrayList<Hospital>( this.hospitals );
		Collections.shuffle(shuffledHospitals);
		
		// Build constraints based on this ordering
		for(Hospital hospital : shuffledHospitals) {
			
		}
		
		
		return null;
	}
	
	/**
	 * Inner class storing intermediary hospital information (reported vertex sets,
	 * size of maximum internal matching given reported vertex set, etc), so we don't
	 * have to keep asking for it.
	 */
	@SuppressWarnings("unused")
	private class HospitalInfo {
		public Pool reportedInternalPool;
		public int maxReportedInternalMatchSize;
	}
}
