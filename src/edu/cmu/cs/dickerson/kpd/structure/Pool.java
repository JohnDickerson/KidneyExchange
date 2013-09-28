package edu.cmu.cs.dickerson.kpd.structure;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class Pool extends DefaultDirectedWeightedGraph<Vertex, Edge> {

	private static final long serialVersionUID = 1L;
	
	private SortedSet<VertexPair> pairs;
	private SortedSet<VertexAltruist> altruists;
	
	public Pool(Class<? extends Edge> edgeClass) {
		super(edgeClass);
		pairs = new TreeSet<VertexPair>();
		altruists = new TreeSet<VertexAltruist>();
	}

	@Override
	public boolean addVertex(Vertex v) {
		if(v.isAltruist()) {
			return addAltruist((VertexAltruist) v);
		} else {
			return addPair((VertexPair) v);
		}
	}
	
	public boolean addPair(VertexPair pair) {
		boolean newVert = super.addVertex(pair);
		if(newVert) {
			pairs.add(pair);
		}
		return newVert;
	}
	
	public boolean addAltruist(VertexAltruist alt) {
		boolean newVert = super.addVertex(alt);
		if(newVert) {
			altruists.add(alt);
		}
		return newVert;
	}
	
	public SortedSet<VertexPair> getPairs() {
		return pairs;
	}

	public SortedSet<VertexAltruist> getAltruists() {
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
	
	public Set<VertexPair> getPairsOfType(BloodType btPatient, BloodType btDonor) {
		
		Set<VertexPair> vSet = new HashSet<VertexPair>();
		for(VertexPair pair : pairs) {
			if(pair.getBloodTypePatient().equals(btPatient) && pair.getBloodTypePatient().equals(btDonor)) {
				vSet.add(pair);
			}
		}
		
		return vSet;
	}
}
