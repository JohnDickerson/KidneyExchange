package edu.cmu.cs.dickerson.kpd.fairness.alg;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;

public final class FairnessUtil {
	
	private FairnessUtil() {}
	
	/**
	 * Resets all weights of edges in the pool as follows:
	 *  - dummy edges (target is altruist) have weight 0.0
	 *  - edges targeting lowly-sensitized patients have weight 1.0
	 *  - edges targeting highly-sensitized patients have weight 1.0+alphaStar
	 * @param pool kidney exchange pool
	 * @param alphaStar "bump" factor describing how much more we value a certain class of vertex
	 * @param specialV special vertices that should be valued at 1.0+alphaStar instead of 1.0
	 */
	public static void setFairnessEdgeWeights(Pool pool, double alphaStar, Set<Vertex> specialV) {
		
		for(Edge e : pool.edgeSet()) {
			
			Vertex v = pool.getEdgeTarget(e);
			if(v.isAltruist()) {
				pool.setEdgeWeight(e, 0.0);
			} else if(specialV.contains(v)) {
				pool.setEdgeWeight(e, 1.0 + alphaStar);
			} else {
				pool.setEdgeWeight(e, 1.0);
			}
		}
		
	}
	
	public static Set<VertexPair> getOnlyHighlySensitizedPairs(Set<VertexPair> allV, double CPRAthreshold) {
		Set<VertexPair> hsvSet = new HashSet<VertexPair>();
		for(VertexPair v : allV) {
			if(v.getPatientCPRA() >= CPRAthreshold) {
				hsvSet.add(v);
			}
		}
		return hsvSet;
	}
}
