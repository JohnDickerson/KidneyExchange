package edu.cmu.cs.dickerson.kpd.structure;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import edu.cmu.cs.dickerson.kpd.drivers.DriverKDD;
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

	public int getNumNonDummyEdges() {
		int edgeCt = 0;
		for(Edge e : this.edgeSet()) {
			if(this.getEdgeWeight(e) != 0.0) {
				edgeCt++;
			}
		}
		return edgeCt;
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
	
	
	/**
	 * Dumps UNOS graph to a dense adjacency matrix for Alex, comma-delimited, n rows
	 * each with n 1s or 0s for edge exists or not
	 * @param path
	 */
	public void writeUNOSGraphToDenseAdjacencyMatrix(String path) {
		int n=this.vertexSet().size();
		boolean[][] edgeExists = this.getDenseAdjacencyMatrix();
		try {
			PrintWriter writer = new PrintWriter(path, "UTF-8");
			for(int v_i=0; v_i<n; v_i++) { 
				StringBuilder sb = new StringBuilder();
				for(int v_j=0; v_j<n; v_j++) {
					sb.append(edgeExists[v_i][v_j] ? "1," : "0,");
				}
				writer.println(sb.toString());
			}
			
			writer.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		return;
	}
	
	/**
	 * Dumps UNOS graph to a DZN input file for Minizinc's CP solver (for bitwise project)
	 * @param path output filepath for DZN file
	 * @param k number of bits per patient, donor vector
	 * @param t threshold
	 */
	public void writeUNOSGraphToDZN(String path, int k, int t) {
		int n=this.vertexSet().size();
		boolean[][] edgeExists = this.getDenseAdjacencyMatrix();
		try {
			PrintWriter writer = new PrintWriter(path, "UTF-8");
			writer.println("v = " + this.vertexSet().size() + ";");
			writer.println("k = " + k + ";");
			writer.println("t = " + t + ";");
			for(int v_i=0; v_i<n; v_i++) { 
				StringBuilder sb = new StringBuilder();
				if(v_i==0) {
					sb.append("e = [| ");
				} else {
					sb.append("     | ");
				}
				for(int v_j=0; v_j<n; v_j++) {
					sb.append(edgeExists[v_i][v_j] ? "1" : "0");
					if(v_j != n-1) {
						sb.append(", ");
					}
				}
				if(v_i==n-1) {
					sb.append(" |];");
				}
				writer.println(sb.toString());
			}
			
			writer.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		return;
	}
	
	
	
	/**
	 * Writes the UNOS graph to a k-implementable CNF SAT file at path
	 * @param k
	 * @param path
	 */
	public void writeUNOSGraphToBitwiseCNF(final int k, String path) {
		int n=this.vertexSet().size();
		boolean[][] edgeExists = this.getDenseAdjacencyMatrix();
		try {
			PrintWriter writer = new PrintWriter(path, "UTF-8");
			writer.println("c " + path);
			writer.println("c " + new Date());
			int numVariables = 
					n*k +      // p_i^\rho    \forall v_i \in V, \rho \in [k]
					n*k +      // d_i^\rho    \forall v_i \in V, \rho \in [k]
					n*n*k;     // z_{ij}^\rho  \forall (v_i, v_j) \not\in E, \rho in [k]  (overestimate)
			int numClauses = 0;
			StringBuilder sb = new StringBuilder();
			for(int v_i=0; v_i<n; v_i++) { 
				for(int v_j=0; v_j<n; v_j++) {
					if(v_i != v_j) {
						if(edgeExists[v_i][v_j]) {
							// \bigwedge\limits_{\rho \in [k]} (\neg d_i^\rho \lor \neg p_j^\rho)   & \forall (v_i, v_j) \in E
							for(int rho=0; rho<k; rho++) {
								sb.append("-" + getCNFDonorIdx(n, k, v_i, rho) + " -" + getCNFPatientIdx(n, k, v_j, rho) + " 0\n");
								numClauses++;
							}
						} else {
							// (z^1_{ij} \lor z^2_{ij} \lor \ldots \lor z^k_{ij}) \land   ....
							StringBuilder conflictSB = new StringBuilder();
							for(int rho=0; rho<k; rho++) {
								conflictSB.append(getCNFConflictForceIdx(n, k, v_i, v_j, rho) + " ");
							}
							conflictSB.append("0\n");
							sb.append(conflictSB.toString());
							numClauses++;
							
							// ... \bigwedge\limits_{\rho \in [k]}\left[ 
							//            (\neg z^\rho_{ij} \lor d_i^\rho) \land (\neg z^\rho_{ij} \lor p_j^\rho) 
							//        \right]  & \forall (v_i, v_j) \not\in E
							for(int rho=0; rho<k; rho++) {
								sb.append("-" + getCNFConflictForceIdx(n, k, v_i, v_j, rho) + " " + getCNFDonorIdx(n, k, v_i, rho) + " 0\n");
								numClauses++;
								sb.append("-" + getCNFConflictForceIdx(n, k, v_i, v_j, rho) + " " + getCNFPatientIdx(n, k, v_j, rho) + " 0\n");
								numClauses++;
							}
						}
						
					}
				}
			}
			writer.println("p cnf " + numVariables + " " + numClauses);
			writer.println(sb.toString());
			writer.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		return;
	}
	
	private int getCNFDonorIdx(final int n, final int k, final int v_i, final int rho) {
		return 1 + v_i*k + rho;
	}
	
	private int getCNFPatientIdx(final int n, final int k, final int v_j, final int rho) {
		return 1 + k*n + k*v_j + rho;
	}
	
	private int getCNFConflictForceIdx(final int n, final int k, final int v_i, final int v_j, final int rho) {
		return 1 + k*n + k*n + k*n*v_i + k*v_j + rho;
	}
	
	
	/**
	 * Returns the |V| x |V| adjacency matrix, dense, for this pool
	 * @return
	 */
	public boolean[][] getDenseAdjacencyMatrix() {
		int n=this.vertexSet().size();
		boolean[][] edgeExists = new boolean[n][n];
		for(int v_i=0; v_i<n; v_i++) { for(int v_j=0; v_j<n; v_j++) { edgeExists[v_i][v_j] = false; }}
		for(Edge e : this.edgeSet()) {
			// Don't include the dummy edges going back to altruists (since they're a byproduct of the cycle formulation)
			if(this.getEdgeTarget(e).isAltruist()) { continue; }
			// Otherwise, set (v_i, v_j) to True in our existence array
			edgeExists[this.getEdgeSource(e).getID()][this.getEdgeTarget(e).getID()] = true;
		}
		return edgeExists;
	}
}
