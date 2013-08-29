package edu.cmu.cs.dickerson.kpd.solver.solution;

public class Solution {
	
	// TODO include the matching (collection of cycles) here
	
	
	private long solveTime = 0;
	private double objectiveValue;
	
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

	@Override
	public String toString() {
		return "Objective: " + objectiveValue + " (solve time: " + solveTime/1000000000.0 + ")";
	}
}
