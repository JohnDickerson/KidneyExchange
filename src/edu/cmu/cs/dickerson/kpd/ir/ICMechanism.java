package edu.cmu.cs.dickerson.kpd.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.ir.solver.IRCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.ir.solver.IRSolution;
import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;
import edu.cmu.cs.dickerson.kpd.ir.structure.HospitalInfo;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public abstract class ICMechanism {

	// TODO We assume that all edges are unit weight; the objective value is equal to #pairs matched

	private Set<Hospital> hospitals;
	private int cycleCap = 3;
	private int chainCap = 0;
	private int meanLifeExpectancy = 1;
	private Set<Vertex> everRevealedVertices; // keep track of all vertices ever revealed to mechanism

	protected boolean enforcingIRConstraints = false;

	public ICMechanism(Set<Hospital> hospitals) {
		this(hospitals, 3, 0, 1);   // default to 3-cycles and 0-chains
	}

	public ICMechanism(Set<Hospital> hospitals, int cycleCap, int chainCap, int meanLifeExpectancy) {
		this.hospitals = hospitals;	
		this.cycleCap = cycleCap;  // internal and external matching cycle limit
		this.chainCap = chainCap;  // internal and external matching chain limit
		this.meanLifeExpectancy = meanLifeExpectancy;
		this.reset();
	}

	public void reset() {
		for(Hospital hospital : hospitals) { hospital.setNumCredits(0); }   // all hospitals start out with no history
		everRevealedVertices = new HashSet<Vertex>();
	}

	public IRSolution doMatching(Pool entirePool, Set<Vertex> dieNextRoundVertices) throws SolverException {
		return doMatching(entirePool, dieNextRoundVertices, new Random());
	}

	public boolean isEnforcingIRConstraints() {
		return enforcingIRConstraints;
	}

	public void setEnforcingIRConstraints(boolean enforcesIRConstraints) {
		this.enforcingIRConstraints = enforcesIRConstraints;
	}

	public IRSolution doMatching(Pool entirePool, Set<Vertex> dieNextRoundVertices, Random r) throws SolverException {

		//
		// Initial credit balance update based on reported types
		SortedMap<Hospital, HospitalInfo> infoMap = new TreeMap<Hospital, HospitalInfo>();
		Set<Vertex> allReportedVertices = new HashSet<Vertex>();
		for(Hospital hospital : hospitals) {

			HospitalInfo hospitalInfo = new HospitalInfo();

			// Ask the hospital for its reported type
			Set<Vertex> reportedVertices = hospital.getPublicVertexSet(hospitalInfo, entirePool, cycleCap, chainCap, false, dieNextRoundVertices);
			allReportedVertices.addAll(reportedVertices);
			Pool reportedInternalPool = entirePool.makeSubPool(reportedVertices);
			hospitalInfo.reportedInternalPool = reportedInternalPool;
			hospitalInfo.publicVertexCt = reportedVertices.size();
			hospitalInfo.privateVertexCt = hospital.getPublicAndPrivateVertices().size();

			// Update hospital's credits based on reported type
			// c_i += 4*k_i*\sum{v in reported V} \ell_v   -   4*k_i^2*\ell_m
			int expectedType = hospital.getExpectedArrival();   // "k_i"
			int creditDelta = 0;
			for(Vertex v : reportedInternalPool.vertexSet()) {
				if(everRevealedVertices.contains(v)) {
					// Hospital has previously revealed and received credits for this vertex
				} else {
					// Hospital gets credits proportional to the life expectancy of this vertex
					creditDelta += 4*expectedType*hospital.getVertexInfo().get(v).lifeExpectancy;
					everRevealedVertices.add(v);
				}
			}
			creditDelta -= 4*expectedType*expectedType*this.meanLifeExpectancy;
			hospital.addCredits( creditDelta );
			
			if(this.isEnforcingIRConstraints()) {
				// Figure out a maximum utility internal match on reported type
				Solution internalMatch = null;
				try {
					internalMatch = hospital.doInternalMatching(reportedInternalPool, this.cycleCap, this.chainCap, false);
				} catch(SolverException e) {
					e.printStackTrace();
					throw new SolverException("Unrecoverable error solving cycle packing problem on public reported pool of " + hospital + "; experiments are bunk.\nOriginal Message: " + e.getMessage());
				}
				hospitalInfo.maxReportedInternalMatchSize = Vertex.countPatientDonorPairs( Cycle.getConstituentVertices(
						internalMatch.getMatching(), reportedInternalPool) );  // recording match SIZE, not UTILITY [for now], altruists count for nothing
				hospitalInfo.minRequiredNumPairs = hospitalInfo.maxReportedInternalMatchSize;
			} else {
				// If we're not enforcing IR constraints, we don't care about reported internal match size
				hospitalInfo.maxReportedInternalMatchSize = -1;
				hospitalInfo.minRequiredNumPairs = 0;
			}

			// Figure out a maximum utility internal match on PRIVATE type (need this for experimental records only)
			Solution maxPrivateInternalMatch = null;
			try {
				Pool privatePublicPool = entirePool.makeSubPool(hospital.getPublicAndPrivateVertices());
				maxPrivateInternalMatch = hospital.doInternalMatching(privatePublicPool, this.cycleCap, this.chainCap, false);
				hospitalInfo.maxPossibleInternalMatchSize =  Vertex.countPatientDonorPairs( 
						Cycle.getConstituentVertices( maxPrivateInternalMatch.getMatching(), privatePublicPool) );  // don't count altruists
			} catch(SolverException e) {
				e.printStackTrace();
				throw new SolverException("Unrecoverable error solving cycle packing problem on public+private reported pool of " + hospital + "; experiments are bunk.\nOriginal Message: " + e.getMessage());
			}

			// Initialize remaining details and record
			hospitalInfo.exactRequiredNumPairs = -1;  // tell solver to ignore equality constraints
			infoMap.put(hospital, hospitalInfo);
			System.out.println(hospitalInfo);
		}

		// We use the same global set of cycles, cycle membership for each of the subpool solves,
		// on our new global pool consisting of all publicly reported pairs
		Pool entireReportedPool = entirePool.makeSubPool(allReportedVertices);
		CycleGenerator cg = new CycleGenerator(entireReportedPool);
		List<Cycle> allCycles = cg.generateCyclesAndChains(cycleCap, chainCap, false);
		CycleMembership cycleMembership = new CycleMembership(entireReportedPool, allCycles);
		IRCPLEXSolver solver = new IRCPLEXSolver(entireReportedPool, allCycles, cycleMembership, hospitals);


		// Get maximum matching subject to each hospital getting at least as many matches
		// as it could've gotten if had only matched its reported pairs alone
		Solution allIRMatching = null;
		try {
			allIRMatching = solver.solve(infoMap, 0, null, true);
			//if(!MathUtil.isInteger(allIRMatching.getObjectiveValue())) { throw new SolverException("IRICMechanism only works for unit-weight, deterministic graphs."); }

		} catch(SolverException e) {
			e.printStackTrace();
			throw new SolverException("Unrecoverable error solving cycle packing problem for max s.t. only IR; experiments are bunk.\nOriginal Message: " + e.getMessage());
		}
		// Constrain future matchings to include at least as many vertices as were in the all-IR matching
		int maxMatchingNumPairs = Vertex.countPatientDonorPairs(
				Cycle.getConstituentVertices(allIRMatching.getMatching(), entireReportedPool)
				);


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
				IOUtil.dPrintln(infoMap.get(hospital));
				throw new SolverException("Unrecoverable error solving cycle packing problem on reported pool of " + hospital + 
						" optimizing for " + (null==solMax ? "MAX" : "MIN") + "; experiments are bunk.\nOriginal Message: " + e.getMessage());
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

		// Convert solution to IRSolution with more information
		IRSolution finalIRSol = new IRSolution(finalSol, entireReportedPool, infoMap);

		return finalIRSol;
	}

}
