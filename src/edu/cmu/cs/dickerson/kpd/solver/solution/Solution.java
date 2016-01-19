package edu.cmu.cs.dickerson.kpd.solver.solution;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.structure.Cycle;

public class Solution {
	
	private long solveTime = 0;
	private double objectiveValue = 0.0;
	private Set<Cycle> matching;
	private boolean isLegalMatching;
	
	public Solution(long solveTime, double objectiveValue, Set<Cycle> matching) {
		this(solveTime, objectiveValue, matching, true);
	}
	
	public Solution(long solveTime, double objectiveValue, Set<Cycle> matching, boolean isLegalMatching) {
		this.solveTime = solveTime;
		this.objectiveValue = objectiveValue;
		this.matching = matching;
		this.isLegalMatching = isLegalMatching;
	}
	
	public Solution() {
		matching = new HashSet<Cycle>();
	}
	
	public boolean addMatchedCycle(Cycle cycle) {
		return matching.add(cycle);
	}
	
	
	public Set<Cycle> getMatching() {
		return matching;
	}

	public void setMatching(Set<Cycle> matching) {
		this.matching = matching;
	}

	public long getSolveTime() {
		return solveTime;
	}

	public void setSolveTime(long totalSolveTime) {
		this.solveTime = totalSolveTime;
	}
	
	public double getObjectiveValue() {
		return objectiveValue;
	}

	public void setObjectiveValue(double objectiveValue) {
		this.objectiveValue = objectiveValue;
	}

	public boolean isLegalMatching() {
		return isLegalMatching;
	}

	public void setLegalMatching(boolean isLegalMatching) {
		this.isLegalMatching = isLegalMatching;
	}

	@Override
	public String toString() {
		return "Objective: " + objectiveValue + " (solve time: " + solveTime/1000000000.0 + ")";
	}
}
