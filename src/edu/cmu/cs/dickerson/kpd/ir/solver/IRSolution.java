package edu.cmu.cs.dickerson.kpd.ir.solver;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;
import edu.cmu.cs.dickerson.kpd.ir.structure.HospitalInfo;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;

public class IRSolution extends Solution {

	private SortedMap<Hospital, HospitalInfo> hospInfoMap;
	private Pool fullPublicPool;

	protected IRSolution() {};

	public IRSolution(Solution solution, Pool fullPublicPool, SortedMap<Hospital, HospitalInfo> hospInfoMap) {
		// Construct basic solution
		super(solution.getSolveTime(), solution.getObjectiveValue(), solution.getMatching());
		this.fullPublicPool = fullPublicPool;
		this.hospInfoMap = hospInfoMap;
		// Construct per-hospital solution details
		computePerHospitalStats();
	}

	/**
	 * Fills in per-hospital details that are only known after the IRIC Mechanism completes
	 */
	private void computePerHospitalStats() {
		for(Map.Entry<Hospital, HospitalInfo> e : hospInfoMap.entrySet()) {
			Hospital h = e.getKey();
			HospitalInfo hInfo = e.getValue();

			int numHospVertsMatched = Cycle.getConstituentVerticesInSubPool(super.getMatching(), fullPublicPool, hInfo.reportedInternalPool).size();
			hInfo.actualMechanismMatchSize = numHospVertsMatched; // actually matched in final solution
		}
	}

	/**
	 * @return total number of vertices matched publicly by mechanism
	 */
	public int getNumMatchedByMechanism() {
		return Cycle.getConstituentVertices(super.getMatching(), fullPublicPool).size();
	}
	
	/**
	 * @return total number of vertices belonging to hospital matched publicly by mechanism
	 */
	public int getNumMatchedByMechanism(Hospital hospital) {
		return hospInfoMap.get(hospital).actualMechanismMatchSize;
	}

	/**
	 * @return number of vertices hospital could have matched internally, selfishly
	 */
	public int getNumCouldHaveInternallyMatched(Hospital hospital) {
		return hospInfoMap.get(hospital).maxPossibleInternalMatchSize;
	}

	/**
	 * @return size of the set of vertices reported publicly by hospital
	 */
	public int getReportedVertexCt(Hospital hospital) {
		return hospInfoMap.get(hospital).publicVertexCt;
	}

	/**
	 * @return size of the set of vertices hospital has (privately and publicly)
	 */
	public int getTrueVertexCt(Hospital hospital) {
		return hospInfoMap.get(hospital).privateVertexCt;
	}

}
