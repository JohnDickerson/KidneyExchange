package edu.cmu.cs.dickerson.kpd.structure;

import org.jgrapht.graph.DefaultWeightedEdge;

public class Edge extends DefaultWeightedEdge {

	private static final long serialVersionUID = 1L;

	private double failureProbability = 0.0;
	
	public void setFailureProbability(double failureProbability) {
		this.failureProbability = failureProbability;
	}
	
	public double getFailureProbability() {
		return this.failureProbability;
	}

}
