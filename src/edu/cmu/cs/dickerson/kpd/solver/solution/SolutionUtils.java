package edu.cmu.cs.dickerson.kpd.solver.solution;

import java.util.Set;

import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;

public final class SolutionUtils {

	private SolutionUtils() {};
	
	/**
	 * Counts the number of distinguished vertices in a given solution's matching
	 * @param pool
	 * @param solution
	 * @param vertices
	 * @return
	 */
	public static int countVertsInMatching(Pool pool, Solution solution, Set<Vertex> vertices, boolean includeAltruists) {
		
		if(null == solution || null == vertices) { return 0; }
		
		int count = 0;
		for(Cycle matchedCycle : solution.getMatching()) {
			for(Edge edge : matchedCycle.getEdges()) {
				Vertex matchedVertex = pool.getEdgeTarget(edge);
				if( (includeAltruists || !matchedVertex.isAltruist()) && vertices.contains(matchedVertex)) {
					count++;
				}
			}
		}
		
		return count;
	}
	
	public static double countExpectedTransplantsInMatching(Pool pool, Solution solution, Set<Vertex> specialV) {
		
		if(null == solution || null == specialV) { return 0; }
		
		double expectation = 0.0;
		for(Cycle matchedCycle : solution.getMatching()) {
			if(Cycle.isAChain(matchedCycle, pool)) {
				expectation += FailureProbabilityUtil.calculateDiscountedChainUtility(matchedCycle, pool, specialV, true);
			} else {
				expectation += FailureProbabilityUtil.calculateDiscountedCycleUtility(matchedCycle, pool, specialV, true);
			}
		}
		
		return expectation;
	}
}
