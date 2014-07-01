package edu.cmu.cs.dickerson.kpd.ir.solver;

import java.util.Map;

import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;
import edu.cmu.cs.dickerson.kpd.ir.structure.HospitalInfo;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;

public class IRSolution extends Solution {

	private Map<Hospital, Integer> publicMatchMap;
	private Map<Hospital, Integer> privateMatchMap;
	
	public IRSolution(Solution solution, Map<Hospital, HospitalInfo> hospInfoMap) {
		// Construct basic solution
		super(solution.getSolveTime(), solution.getObjectiveValue(), solution.getMatching());
		// Construct per-hospital solution details
		computePerHospitalStats(hospInfoMap);
	}
	
	private void computePerHospitalStats(Map<Hospital, HospitalInfo> hospInfoMap) {
		
	}
	
}
