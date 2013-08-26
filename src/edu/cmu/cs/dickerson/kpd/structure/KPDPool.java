package edu.cmu.cs.dickerson.kpd.structure;

import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

public class KPDPool extends DefaultDirectedWeightedGraph<KPDVertex, KPDEdge> {

	private static final long serialVersionUID = 1L;
	
	private SortedSet<KPDVertexPair> pairs;
	private SortedSet<KPDVertexAltruist> altruists;
	
	public KPDPool(Class<? extends KPDEdge> edgeClass) {
		super(edgeClass);
		pairs = new TreeSet<KPDVertexPair>();
		altruists = new TreeSet<KPDVertexAltruist>();
	}

	@Override
	public boolean addVertex(KPDVertex v) {
		throw new UnsupportedOperationException("Please use addPair(KPDVertexPair) and addAltruist(KPDVertexAltruist) instead!");
	}
	
	public boolean addPair(KPDVertexPair pair) {
		boolean newVert = super.addVertex(pair);
		if(newVert) {
			pairs.add(pair);
		}
		return newVert;
	}
	
	public boolean addAltruist(KPDVertexAltruist alt) {
		boolean newVert = super.addVertex(alt);
		if(newVert) {
			altruists.add(alt);
		}
		return newVert;
	}
	
	public SortedSet<KPDVertexPair> getPairs() {
		return pairs;
	}

	public SortedSet<KPDVertexAltruist> getAltruists() {
		return altruists;
	}

	public int getNumAltruists() {
		return altruists.size();
	}

	public int getNumPairs() {
		return pairs.size();
	}
	
	@Override
	public String toString() {
		return "< (" + getNumPairs() + ", " + getNumAltruists() + "), " + super.edgeSet().size() + " >";
	}
}
