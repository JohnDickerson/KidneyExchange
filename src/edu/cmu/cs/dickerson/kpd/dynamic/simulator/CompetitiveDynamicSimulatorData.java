package edu.cmu.cs.dickerson.kpd.dynamic.simulator;

public class CompetitiveDynamicSimulatorData {

	private int totalVerticesSeen 				= 0;
	private int totalVerticesMatched 			= 0;
	private int totalVerticesMatchedByGreedy 	= 0;
	private int totalVerticesMatchedByPatient 	= 0;
	private int totalVerticesExpired 			= 0;
	
	public CompetitiveDynamicSimulatorData() {
		
	}

	public int getTotalVerticesSeen() {
		return totalVerticesSeen;
	}

	public void setTotalVerticesSeen(int totalVerticesSeen) {
		this.totalVerticesSeen = totalVerticesSeen;
	}

	public int getTotalVerticesMatched() {
		return totalVerticesMatched;
	}

	public void setTotalVerticesMatched(int totalVerticesMatched) {
		this.totalVerticesMatched = totalVerticesMatched;
	}

	public int getTotalVerticesMatchedByGreedy() {
		return totalVerticesMatchedByGreedy;
	}

	public void setTotalVerticesMatchedByGreedy(int totalVerticesMatchedByGreedy) {
		this.totalVerticesMatchedByGreedy = totalVerticesMatchedByGreedy;
	}

	public int getTotalVerticesMatchedByPatient() {
		return totalVerticesMatchedByPatient;
	}

	public void setTotalVerticesMatchedByPatient(int totalVerticesMatchedByPatient) {
		this.totalVerticesMatchedByPatient = totalVerticesMatchedByPatient;
	}

	public int getTotalVerticesExpired() {
		return totalVerticesExpired;
	}

	public void setTotalVerticesExpired(int totalVerticesExpired) {
		this.totalVerticesExpired = totalVerticesExpired;
	}
	
	
	
}
