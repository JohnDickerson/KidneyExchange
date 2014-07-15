package edu.cmu.cs.dickerson.kpd.dynamic.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.dickerson.kpd.ir.solver.IRSolution;
import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;

public class IRICDynamicSimulatorData {

	private int totalNumVertsMatched;
	private int totalExternalNumVertsMatched;
	private int totalInternalNumVertsMatched;
	private List<Integer> numMatchedSoFar;
	private Map<Hospital, List<Integer>> hospNumMatchedSoFar;
	
	public IRICDynamicSimulatorData() {
		this.numMatchedSoFar = new ArrayList<Integer>();
		this.hospNumMatchedSoFar = new HashMap<Hospital, List<Integer>>();
	}

	public void registerTimePeriod(IRSolution sol) {
		for(Hospital h : sol.getHospitals()) {
			int matchedThisPeriod = sol.getNumMatchedByMechanism(h) + sol.getNumMatchedInternally(h);
			if(!hospNumMatchedSoFar.containsKey(h)) { 
				// First time we've seen this hospital; it has only matched this one time period
				hospNumMatchedSoFar.put(h, new ArrayList<Integer>()); 
				hospNumMatchedSoFar.get(h).add( matchedThisPeriod );
			} else {
				int matchedBeforeThisPeriod = hospNumMatchedSoFar.get(h).get(hospNumMatchedSoFar.get(h).size()-1);
				hospNumMatchedSoFar.get(h).add( matchedBeforeThisPeriod + matchedThisPeriod );
			}
		}
	}
	
	public Map<Hospital, List<Integer>> getHospNumMatchSoFar() {
		return hospNumMatchedSoFar;
	}

	public int getTotalNumVertsMatched() {
		return totalNumVertsMatched;
	}
	public void setTotalNumVertsMatched(int totalNumVertsMatched) {
		this.totalNumVertsMatched = totalNumVertsMatched;
	}
	public int getTotalExternalNumVertsMatched() {
		return totalExternalNumVertsMatched;
	}
	public void setTotalExternalNumVertsMatched(int totalExternalNumVertsMatched) {
		this.totalExternalNumVertsMatched = totalExternalNumVertsMatched;
	}
	public int getTotalInternalNumVertsMatched() {
		return totalInternalNumVertsMatched;
	}
	public void setTotalInternalNumVertsMatched(int totalInternalNumVertsMatched) {
		this.totalInternalNumVertsMatched = totalInternalNumVertsMatched;
	}
	public List<Integer> getNumMatchedSoFar() {
		return numMatchedSoFar;
	}
	public void setNumMatchedSoFar(List<Integer> numMatchedSoFar) {
		this.numMatchedSoFar = numMatchedSoFar;
	}
}
