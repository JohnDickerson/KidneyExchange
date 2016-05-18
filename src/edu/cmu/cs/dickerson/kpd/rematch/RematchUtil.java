package edu.cmu.cs.dickerson.kpd.rematch;

import java.util.ListIterator;
import java.util.Map;

import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;

public final class RematchUtil {

	private RematchUtil() {}
	

	/**
	 * If we previously"tested" an edge and, via that result and set its failure
	 * probability to 0.0 (=failed) or 1.0 (=succeeded), this method returns that
	 * failure probability to its originally-generated value in [0.0, 1.0]
	 * @param edgeFailureRateMap map of Edge -> original failure rate
	 */
	public static void resetPoolEdgeTestsToUnknown(Map<Edge, Double> edgeFailureRateMap) {
		// Reset the pool's edges to their failure probabilities, not failure statuses
		for(Map.Entry<Edge, Double> entry : edgeFailureRateMap.entrySet()) {
			entry.getKey().setFailureProbability(entry.getValue());
		}
	}

	
	/**
	 * Given a recommended matching, simulates that matching on the omniscient
	 * pool where we know all the edge tests and returns the successful utility
	 * gained from that matching
	 * 
	 * @param sol recommended matching
	 * @param pool underlying pool of vertices and edges
	 * @param edgeFailedMap mapping of edges -> whether they exist or not
	 * @return number of successful transplants
	 */
	public static double calculateNumTransplants(
			Solution sol,
			Pool pool,
			Map<Edge, Boolean> edgeFailedMap
			) {
		
		double numActualTransplants=0;
		for(Cycle c : sol.getMatching()) {

			if(Cycle.isAChain(c, pool)) {
				// Chains succeed incrementally (starting from altruist up to first edge failure)
				ListIterator<Edge> reverseEdgeIt = c.getEdges().listIterator(c.getEdges().size());
				int successCt = 0;
				while(reverseEdgeIt.hasPrevious()) {
					Edge e = reverseEdgeIt.previous();
					if(successCt == 0 && !pool.getEdgeSource(e).isAltruist()) {
						System.err.println("Chain generated in a different way than expected; chain check isn't going to perform correctly.");
						System.exit(-1);
					}
					if(edgeFailedMap.get(e)) {
						break;
					}
					successCt++;
				}

				if(successCt == c.getEdges().size()) {
					successCt -= 1;    // if nothing failed, don't count dummy edge going back to altruist
				}
				numActualTransplants += successCt;
			} else {
				// Cycles fail or succeed in entirety
				boolean failed = false;
				for(Edge e : c.getEdges()) {
					failed |= edgeFailedMap.get(e);  // even one edge failure -> entire cycle fails completely
				}
				if(!failed) { numActualTransplants += c.getEdges().size(); } // if cycle succeeds, count all verts in it

			}  // end of isAChain conditional
		}
		return numActualTransplants;
	}
	
}
