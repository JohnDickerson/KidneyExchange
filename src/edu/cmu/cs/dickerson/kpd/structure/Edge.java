package edu.cmu.cs.dickerson.kpd.structure;

import org.jgrapht.graph.DefaultWeightedEdge;

public class Edge extends DefaultWeightedEdge {

	private static final long serialVersionUID = 1L;

	private double failureProbability = 0.0;

	// used for Variation solver
	private int rank = -1;
	private int[] beta = new int[]{-1};
	
	public void setFailureProbability(double failureProbability) {
		this.failureProbability = failureProbability;
	}
	
	public double getFailureProbability() {
		return this.failureProbability;
	}

	// used for Variation solver
	public void setRank(int rank) {
		this.rank = rank;
	}

	public int getRank() {
		return rank;
	}

}
