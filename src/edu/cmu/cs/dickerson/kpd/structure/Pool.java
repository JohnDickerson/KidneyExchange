package edu.cmu.cs.dickerson.kpd.structure;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import edu.cmu.cs.dickerson.kpd.structure.real.UNOSDonor;
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
	
	/**
	 * Outputs this graph to files that can be read by the current UNOS solver
	 * This should *NOT* be used in UNOS production, since we gloss over a couple of
	 * things that are specific to the UNOS files in favor of running this on
	 * generated data.  Can be used for general studies, though.
	 * @param baseFileName
	 */
	public void writeToUNOSKPDFile(String baseFileName) {
		
		// Main graph file
		try {
			PrintWriter writer = new PrintWriter(baseFileName + ".input", "UTF-8");
			// <num-vertices> <num-edges>
			writer.println(this.vertexSet().size() + " " + this.edgeSet().size());
			// <src-vert> <sink-vert> <edge-weight> <is-dummy> <failure-prob> 
			for(Edge e : this.edgeSet()) {
				Vertex src = this.getEdgeSource(e);
				Vertex dst = this.getEdgeTarget(e);
				double weight = this.getEdgeWeight(e);
				int isDummy = dst.isAltruist() ? 1 : 0;
				double failureProb = e.getFailureProbability();
				writer.println(src.getID() + " " + dst.getID() + " " + weight + " " + isDummy + " " + failureProb);
			}
			// Legacy code signals EOF with -1 -1 -1
						writer.println("-1 -1 -1");
						writer.close();
						
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// Vertex details files for UNOS runs
		try {
			PrintWriter writer = new PrintWriter(baseFileName + "-details.input", "UTF-8");
			// <donor_id> <max_pairs_cycle> <max_pairs_chain> <home-ctr-ID>
			for(Vertex v : this.vertexSet()) {
				int maxPairsChain = Integer.MAX_VALUE-1;
				int maxPairsCycle = 3;
				if(null != v.getUnderlyingPair()) {
					for(UNOSDonor d : v.getUnderlyingPair().getDonors()) {
						maxPairsCycle = Math.min(maxPairsCycle, d.maxPairsCycle);
						maxPairsChain = Math.min(maxPairsChain, d.maxPairsChain);
					}
				}
				int homeCtrID = 0;  // ignore this for now
				writer.println(v.getID() + " " + maxPairsCycle + " " + maxPairsChain + " " + homeCtrID);
			}
			//Legacy code signals EOF with -1 -1 -1
			writer.println("-1 -1 -1");
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}
}
