package edu.cmu.cs.dickerson.kpd.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

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

	/**
	 * Given a cycle and a pool, returns the set of vertices in the pool that
	 * are also in the cycle
	 * @param c
	 * @param pool
	 * @return Set containing each Vertex in the Cycle
	 */
	public static Set<Vertex> getConstituentVertices(Cycle c, Pool pool) {
		Set<Vertex> vertices = new HashSet<Vertex>();
		if(null == c) { return vertices; }
		if(null == pool) { throw new RuntimeException("Need a valid pool to do vertex lookups in a cycle."); }

		for(Edge edge : c.getEdges()) {
			vertices.add( pool.getEdgeTarget(edge) );
		}
		return vertices;
	}

	/**
	 * Given a set of cycles and a pool, returns the set of vertices in the pool that
	 * are also in at least one of the cylces
	 * @param cycles
	 * @param pool
	 * @return Set containing the union of Vertex objects in all cycles
	 */
	public static Set<Vertex> getConstituentVertices(Set<Cycle> cycles, Pool pool) {
		Set<Vertex> vertices = new HashSet<Vertex>();
		if(null == cycles) { return vertices; }
		for(Cycle c : cycles) {
			vertices.addAll(Cycle.getConstituentVertices(c, pool));
		}
		return vertices;
	}


	public static Set<Vertex> getConstituentAltruists(Set<Cycle> cycles, Pool pool) {
		// Why aren't there lambdas :(.
		Set<Vertex> alts = new HashSet<Vertex>();
		for(Vertex v : Cycle.getConstituentVertices(cycles, pool)) {
			if(v.isAltruist()) { alts.add(v); }
		}
		return alts;
	}
	
	/**
	 * Given a cycle from a full pool and a reduced pool, returns the set of vertices in the cycle
	 * that are also in the reduced pool
	 * @param c
	 * @param fullPool
	 * @param reducedPool
	 * @return Set containing vertices that are in cycle and also in reduced pool
	 */
	public static Set<Vertex> getConstituentVerticesInSubPool(Cycle c, Pool fullPool, Pool reducedPool) {
		Set<Vertex> vertices = new HashSet<Vertex>();
		for(Edge edge : c.getEdges()) {
			Vertex tgtInFullVertex = fullPool.getEdgeTarget(edge);
			if(reducedPool.vertexSet().contains(tgtInFullVertex)) { vertices.add(tgtInFullVertex); }
		}
		return vertices;
	}

	/**
	 * Given a set of cycles from a full pool and a reduced pool, returns the set of vertices in
	 * at least one cycle that are also in the reduced pool
	 * @param cycles
	 * @param fullPool
	 * @param reducedPool
	 * @return Set containing vertices that are in cycle and also in reduced pool
	 */
	public static Set<Vertex> getConstituentVerticesInSubPool(Set<Cycle> cycles, Pool fullPool, Pool reducedPool) {
		Set<Vertex> vertices = new HashSet<Vertex>();
		if(null == cycles) { return vertices; }
		for(Cycle c : cycles) {
			vertices.addAll(Cycle.getConstituentVerticesInSubPool(c, fullPool, reducedPool));
		}
		return vertices;
	}
	
} // end of class

