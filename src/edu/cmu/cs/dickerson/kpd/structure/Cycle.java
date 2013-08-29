package edu.cmu.cs.dickerson.kpd.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
}

