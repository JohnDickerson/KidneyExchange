package edu.cmu.cs.dickerson.kpd.structure.alg;

import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;

public final class FailureProbabilityUtil {

	private FailureProbabilityUtil() {}

	public static enum ProbabilityDistribution { NONE, CONSTANT, BIMODAL };


	/**
	 * Sets failure probabilities for each (non-dummy) edge in the graph
	 * @param pool
	 * @param dist
	 * @param r
	 */
	public static void setFailureProbability(Pool pool, ProbabilityDistribution dist, Random r) {

		if(dist.equals(ProbabilityDistribution.NONE)) {
			return;
		}
		
		for(Edge e : pool.edgeSet()) {

			if(pool.getEdgeTarget(e).isAltruist()) {
				// Dummy edges going to altruists cannot fail
				e.setFailureProbability(0.0);
			} else {
				// Real edges (real donor giving to real patient) can fail
				switch(dist) {
				case CONSTANT:
					// Constant 70% chance of failure
					e.setFailureProbability(0.7);
					break;
				case BIMODAL:
					if(r.nextDouble() < 0.25) {
						// E[10%] chance of failure
						e.setFailureProbability(0.0 + r.nextDouble()*0.2);
					} else {
						// E[90%] chance of failure
						e.setFailureProbability(0.8 + r.nextDouble()*0.2);
					}
					break;
				case NONE:
				default:
					break;
				}
			}
		}
	}
}
