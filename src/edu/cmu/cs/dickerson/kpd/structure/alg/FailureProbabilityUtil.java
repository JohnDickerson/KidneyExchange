package edu.cmu.cs.dickerson.kpd.structure.alg;

import java.util.ListIterator;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;

public final class FailureProbabilityUtil {

	private FailureProbabilityUtil() {}

	public static enum ProbabilityDistribution { NONE, CONSTANT, BIMODAL };


	/**
	 * Sets failure probabilities for each (non-dummy) edge in the graph
	 * @param pool KPD Pool
	 * @param dist either NONE (ignore failure probs, used for testing), CONSTANT (0.7 failure for all non-dummy edges), or BIMODAL (two modes, ~E[fail]=0.1 and ~E[fail=0.9] such that the overall E[fail] = 0.7)
	 * @param r Random instance
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
	
	
	public static double calculateDiscountedCycleUtility(Cycle c, Pool pool, Set<Vertex> specialV) {
		
		double utilSum = 0.0;
		double succProb = 1.0;
		for(Edge e : c.getEdges()) {
			Vertex recipient = pool.getEdgeTarget(e);
			if(specialV.contains(recipient)) {
				utilSum += pool.getEdgeWeight(e);
			}
			succProb *= (1.0 - e.getFailureProbability());
		}
		
		return utilSum * succProb;
	}
	
	
	
	public static double calculateDiscountedChainUtility(Cycle c, Pool pool, Set<Vertex> specialV) {
		
		int edgeIdx = 0;
		double pathSuccProb = 1.0;
		double rawPathWeight = 0.0;
		double discountedPathWeight = 0.0;
		
		// Iterate from last to first in the list of edges, since altruists start at the end
		ListIterator<Edge> reverseEdgeIt = c.getEdges().listIterator(c.getEdges().size());
		while(reverseEdgeIt.hasPrevious()) {
			
			Edge e = reverseEdgeIt.previous();
			
			// We're looking at the (infallible) dummy edge heading back to the altruist
			// We're also assuming that the altruist is not in specialV (or if she is, it doesn't matter)
			if(edgeIdx == c.getEdges().size() - 1) {
				discountedPathWeight += rawPathWeight*pathSuccProb;
				break;
			}
			
			if(edgeIdx == 0 && !pool.getEdgeSource(e).isAltruist()) {
				throw new IllegalArgumentException("Our generator generates chains with altruists sourcing the first edge.  I haven't coded up the discounted utility code for non-0-index altruists.");
			} else {
				discountedPathWeight += rawPathWeight*pathSuccProb*e.getFailureProbability();
			}

			pathSuccProb *= (1.0 - e.getFailureProbability());

			if(specialV.contains(pool.getEdgeTarget(e))) {
				rawPathWeight += pool.getEdgeWeight(e);
			}
			edgeIdx++;
		}
		
		return discountedPathWeight;
	}
	
}
