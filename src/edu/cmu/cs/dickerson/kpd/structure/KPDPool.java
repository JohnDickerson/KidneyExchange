package edu.cmu.cs.dickerson.kpd.structure;

import java.util.SortedSet;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

public class KPDPool extends DefaultDirectedWeightedGraph<KPDVertex, KPDEdge> {

	private static final long serialVersionUID = 1L;
	
	private SortedSet<KPDVertex> pairs;
	private SortedSet<KPDVertex> altruists;
	
	public KPDPool(Class<? extends KPDEdge> edgeClass) {
		super(edgeClass);
	}

	public SortedSet<KPDVertex> getPairs() {
		return pairs;
	}

	public SortedSet<KPDVertex> getAltruists() {
		return altruists;
	}

	public int getNumAltruists() {
		return altruists.size();
	}

	public int getNumPairs() {
		return pairs.size();
	}
}
