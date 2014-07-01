package edu.cmu.cs.dickerson.kpd.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.ir.solver.IRCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;
import edu.cmu.cs.dickerson.kpd.ir.structure.HospitalInfo;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverRuntimeException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

// To start:
// Everybody dies every timestep
// No chains
// No discount

public class IRICMechanism {

	// TODO We assume that all edges are unit weight; the objective value is equal to #pairs matched

	private Set<Hospital> hospitals;
	private int cycleCap = 3;
	private int chainCap = 0;

	public IRICMechanism(Set<Hospital> hospitals) {
		this(hospitals, 3, 0);   // default to 3-cycles and 0-chains
	}

	public IRICMechanism(Set<Hospital> hospitals, int cycleCap, int chainCap) {
		this.hospitals = hospitals;	
		this.cycleCap = cycleCap;  // internal and external matching cycle limit
		this.chainCap = chainCap;  // internal and external matching chain limit
		for(Hospital hospital : hospitals) { hospital.setNumCredits(0); }   // all hospitals start out with no history
	}

	public Solution doMatching(Pool entirePool) {
		return doMatching(entirePool, new Random());
	}
	
	public Solution doMatching(Pool entirePool, Random r) {

		//
		// Initial credit balance update based on reported types
		Map<Hospital, HospitalInfo> infoMap = new HashMap<Hospital, HospitalInfo>();
		for(Hospital hospital : hospitals) {

			// Ask the hospital for its reported type
			Pool reportedInternalPool = entirePool.makeSubPool( hospital.getPublicVertexSet(entirePool, cycleCap, chainCap, false) );
			
			// Update hospital's credits based on reported type
			// c_i += 4 * k_i * ( |reported| - |expected| )
			int expectedType = hospital.getExpectedArrival();
			hospital.addCredits( 4*expectedType * (reportedInternalPool.vertexSet().size() - expectedType) );

			// Figure out a maximum utility internal match on reported type
			Solution internalMatch = null;
			try {
				internalMatch = hospital.doInternalMatching(reportedInternalPool, this.cycleCap, this.chainCap, false);
			} catch(SolverException e) {
				e.printStackTrace();
				throw new SolverRuntimeException("Unrecoverable error solving cycle packing problem on reported pool of " + hospital + "; experiments are bunk.\nOriginal Message: " + e.getMessage());
			}

			// Record details
			HospitalInfo hospitalInfo = new HospitalInfo();
			hospitalInfo.reportedInternalPool = reportedInternalPool;
			hospitalInfo.maxReportedInternalMatchSize = Cycle.getConstituentVertices(
					internalMatch.getMatching(), reportedInternalPool).size();  // recording match SIZE, not UTILITY [for now]
			hospitalInfo.minRequiredNumPairs = hospitalInfo.maxReportedInternalMatchSize;
			hospitalInfo.exactRequiredNumPairs = -1;  // tell solver to ignore equality constraints
			infoMap.put(hospital, hospitalInfo);
			System.out.println(hospitalInfo);
		}

		// We use the same global set of cycles, cycle membership for each of the subpool solves
		CycleGenerator cg = new CycleGenerator(entirePool);
		List<Cycle> allCycles = cg.generateCyclesAndChains(cycleCap, chainCap, false);
		CycleMembership cycleMembership = new CycleMembership(entirePool, allCycles);
		IRCPLEXSolver solver = new IRCPLEXSolver(entirePool, allCycles, cycleMembership, hospitals);
		
		
		// Get maximum matching subject to each hospital getting at least as many matches
		// as it could've gotten if had only matched its reported pairs alone
		Solution allIRMatching = null;
		try {
			allIRMatching = solver.solve(infoMap, 0, null, true);
			//if(!MathUtil.isInteger(allIRMatching.getObjectiveValue())) { throw new SolverException("IRICMechanism only works for unit-weight, deterministic graphs."); }
			
		} catch(SolverException e) {
			e.printStackTrace();
			throw new SolverRuntimeException("Unrecoverable error solving cycle packing problem for max s.t. only IR; experiments are bunk.\nOriginal Message: " + e.getMessage());
		}
		// Constrain future matchings to include at least as many vertices as were in the all-IR matching
		int maxMatchingNumPairs = Cycle.getConstituentVertices(allIRMatching.getMatching(), entirePool).size();
		

		// Random permutation of hospitals
		List<Hospital> shuffledHospitals = new ArrayList<Hospital>( this.hospitals );
		Collections.shuffle(shuffledHospitals, r);

		// Build constraints based on this ordering
		Solution finalSol = null;
		for(Hospital hospital : shuffledHospitals) {

			Solution solMax = null;
			Solution solMin = null;

			// Get max and min #pairs for this hospital, s.t. other constraints
			try {
				solMax = solver.solve(infoMap, maxMatchingNumPairs, hospital, true);
				solMin = solver.solve(infoMap, maxMatchingNumPairs, hospital, false);
				
				//if(!MathUtil.isInteger(solMax.getObjectiveValue()) || !MathUtil.isInteger(solMin.getObjectiveValue())) { 
				//	throw new SolverException("IRICMechanism only works for unit-weight, deterministic graphs."); 
				//}
			} catch(SolverException e) {
				e.printStackTrace();
				throw new SolverRuntimeException("Unrecoverable error solving cycle packing problem on reported pool of " + hospital + "; experiments are bunk.\nOriginal Message: " + e.getMessage());
			}
			assert(solMax != null);
			assert(solMin != null);
			
			// Update credit balance of hospital based on current credit balance and match delta
			int numPairsDiff = (int)Math.rint(solMax.getObjectiveValue()) - (int)Math.rint(solMin.getObjectiveValue());
			if(hospital.getNumCredits() >= 0) {
				hospital.removeCredits(numPairsDiff);
				finalSol = solMax;
			} else {
				hospital.addCredits(numPairsDiff);
				finalSol = solMin;
			}
			
			// Update constraints for the next iteration (hospital)
			HospitalInfo hInfo = infoMap.get(hospital);
			hInfo.minRequiredNumPairs = -1;  // tell solver to ignore >= constraint
			hInfo.exactRequiredNumPairs = (int)Math.rint(finalSol.getObjectiveValue());  // must get exactly same #verts in future matches
		}

		assert(finalSol != null);
		
		return finalSol;
	}

}
