package edu.cmu.cs.dickerson.kpd.structure;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import edu.cmu.cs.dickerson.kpd.dynamic.DriverKDD;
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

	@Override
	public boolean removeVertex(Vertex v) {
		if(v.isAltruist()) { altruists.remove(v); }
		else {	pairs.remove(v); }
		return super.removeVertex(v);
	}
	
	@Override
	public boolean removeAllVertices(Collection<? extends Vertex> vertices) {
		boolean changed = false;
		for(Vertex v : vertices) {
			changed |= this.removeVertex(v);
		}
		return changed;
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

	public Pool makeSubPool(Set<Vertex> subsetV) {
		
		// TODO Look into subgraph-ing in JGraphT library; not sure about backing sets, so playing it safe now
		//DirectedWeightedSubgraph<Vertex, Edge> subGraph = new DirectedWeightedSubgraph<Vertex, Edge>(this, subsetV, null);
		
		// Add all legal vertices to new pool
		Pool subPool = new Pool(Edge.class);
		for(Vertex v : subsetV) {
			if(this.containsVertex(v)) {
				subPool.addVertex(v);
			}
		}
		
		// Add all legal edges to new pool 
		for(Vertex src : subPool.vertexSet()) {
			for(Vertex sink : subPool.vertexSet()) {
				if(this.containsEdge(src, sink)) {
					subPool.addEdge(src, sink, this.getEdge(src, sink));
				}
			}
		}
		
		return subPool;
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

			// Legacy code requires vertices in sorted order by ID
			List<Vertex> allVertsSorted = new ArrayList<Vertex>(this.vertexSet());
			Collections.sort(allVertsSorted);

			// <num-vertices> <num-edges>
			writer.println(this.vertexSet().size() + " " + this.edgeSet().size());
			// <src-vert> <sink-vert> <edge-weight> <is-dummy> <failure-prob> 

			for(Vertex src : allVertsSorted) {
				for(Edge e : this.outgoingEdgesOf(src)) {

					Vertex dst = this.getEdgeTarget(e);
					double weight = this.getEdgeWeight(e);
					int isDummy = dst.isAltruist() ? 1 : 0;
					double failureProb = e.getFailureProbability();
					writer.println(src.getID() + " " + dst.getID() + " " + weight + " " + isDummy + " " + failureProb);
				}
			}
			// Legacy code signals EOF with -1 -1 -1
			writer.println("-1 -1 -1");
			writer.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// Vertex preferences and details files for UNOS runs
		try {
			PrintWriter writerPrefs = new PrintWriter(baseFileName + "-vertex-prefs.input", "UTF-8");

			// These are used for our simulator, to track ABO and sensitized patients
			PrintWriter writerDetails = new PrintWriter(baseFileName + "-vertex-details.input", "UTF-8");
			writerDetails.println("ID ABO-Patient ABO-Donor Wife-Patient? PRA In-Degree Out-degree Is-altruist? Marginalized?");
			Set<Vertex> marginalizedVerts = DriverKDD.getMarginalizedVertices(this);
			//Set<Vertex> marginalizedVerts = new HashSet<Vertex>();
			
			for(Vertex v : this.vertexSet()) {
				// <donor_id> <max_pairs_cycle> <max_pairs_chain> <home-ctr-ID>

				int maxPairsChain = Integer.MAX_VALUE-1;
				int maxPairsCycle = 3;
				if(null != v.getUnderlyingPair()) {
					for(UNOSDonor d : v.getUnderlyingPair().getDonors()) {
						maxPairsCycle = Math.min(maxPairsCycle, d.maxPairsCycle);
						maxPairsChain = Math.min(maxPairsChain, d.maxPairsChain);
					}
				}
				int homeCtrID = 0;  // ignore this for now
				writerPrefs.println(v.getID() + " " + maxPairsCycle + " " + maxPairsChain + " " + homeCtrID);

				// TODO fix when we deal with more than one donor correctly
				String donorBlood = "O";
				if(v.getUnderlyingPair().getDonors().size() > 0) {
					donorBlood = v.getUnderlyingPair().getDonors().iterator().next().abo.toString();
				}
				
				writerDetails.println(v.getID() + " " +
						(v.isAltruist() ? "Unk" : v.getUnderlyingPair().getRecipient().abo) + " " + 
						donorBlood + " " +
						"0" + " " +
						(v.isAltruist() ? "0" : v.getUnderlyingPair().getRecipient().cpra) + " " +
						this.inDegreeOf(v) + " " +
						this.outDegreeOf(v) + " " +
						(v.isAltruist() ? "1" : "0") + " " +
						(marginalizedVerts.contains(v) ? "1" : "0") + " " 
						);
			}
			//Legacy code signals EOF with -1 -1 -1
			writerPrefs.println("-1 -1 -1");
			writerPrefs.close();
			writerDetails.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}
	
	
	/**
	 * Writes the underlying pool to a NetworkX viz-friendly file format
	 * @param baseFileName
	 */
	public void writeToVizFile(String baseFileName) {
		try {
			PrintWriter writer = new PrintWriter(baseFileName + ".edges", "UTF-8");

			// Legacy code requires vertices in sorted order by ID
			List<Vertex> allVertsSorted = new ArrayList<Vertex>(this.vertexSet());
			Collections.sort(allVertsSorted);

			for(Vertex src : allVertsSorted) {
				for(Edge e : this.outgoingEdgesOf(src)) {

					Vertex dst = this.getEdgeTarget(e);
					double weight = this.getEdgeWeight(e);
					int isDummy = dst.isAltruist() ? 1 : 0;
					// <src-vert> <sink-vert> <edge-weight> <is-dummy>
					writer.println(src.getID() + "," + dst.getID() + "," + weight + "," + isDummy);
				}
			}
			writer.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// Vertex preferences and details files for UNOS runs
		try {
			PrintWriter writer = new PrintWriter(baseFileName + ".verts", "UTF-8");

			for(Vertex v : this.vertexSet()) {
	
				// <vert> <patient-ABO> <donor-ABO>
				if(v.isAltruist()) {
					writer.println(v.getID() + "," + "-" + "," + ((VertexAltruist)v).getBloodTypeDonor() );
				} else {
					writer.println(v.getID() + "," + ((VertexPair)v).getBloodTypePatient() + "," + ((VertexPair)v).getBloodTypeDonor() );
				}
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
