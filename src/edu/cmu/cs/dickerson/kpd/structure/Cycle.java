package edu.cmu.cs.dickerson.kpd.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public class Cycle {

	private List<Edge> edges;
	private double weight;

	private Cycle(List<Edge> edges, double weight) {
		this.edges = edges;
		this.weight = weight;
	}

	public static Cycle makeCycle(Collection<Edge> edges, double weight) {
		List<Edge> edgesCopy = new ArrayList<Edge>(edges);
		return new Cycle(edgesCopy, weight);
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public List<Edge> getEdges() {
		return edges;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("< ");
		for(Edge edge : edges) {
			sb.append(edge.toString() + " ");
		}
		sb.append("> @ " + weight);
		return sb.toString();
	}


	public static boolean isAChain(Cycle c, Pool pool) {

		// Utilities for chains and cycles are computed differently.  Our chains always end with an altruist (when
		// they're generated, we push onto a queue starting with an altruist), and our cycles are small, so 
		// hopefully this will short-circuit quickly
		boolean isChain = false;
		ListIterator<Edge> reverseEdgeIt = c.getEdges().listIterator(c.getEdges().size());
		while(reverseEdgeIt.hasPrevious()) {
			if(pool.getEdgeSource(reverseEdgeIt.previous()).isAltruist()) {
				isChain = true;
				break;
			}
		}
		return isChain;
	}
}

