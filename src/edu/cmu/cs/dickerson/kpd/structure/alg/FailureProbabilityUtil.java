package edu.cmu.cs.dickerson.kpd.structure.alg;

import java.util.ListIterator;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.Ethics.EthicalVertexPair;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;

public final class FailureProbabilityUtil {

	private FailureProbabilityUtil() {}

	public static enum ProbabilityDistribution { NONE, 
		CONSTANT, 
		BIMODAL_RANDOM,
		BIMODAL_CORRELATED,
		BIMODAL_CORRELATED_UNOS, 
		BIMODAL_CORRELATED_APD, 
		BIMODAL_CORRELATED_NKR 
	};


	/**
	 * Legacy for old AAMAS-2014 experiments, where constant failure probability of 0.7 assumed
	 * @param pool
	 * @param dist
	 * @param r
	 */
	public static void setFailureProbability(Pool pool, ProbabilityDistribution dist, Random r) {
		FailureProbabilityUtil.setFailureProbability(pool, dist, r, 0.7);
	}

	/**
	 * Sets failure probabilities for each (non-dummy) edge in the graph
	 * @param pool KPD Pool
	 * @param dist either NONE (ignore failure probs, used for testing), CONSTANT (0.7 failure for all non-dummy edges),
	 *             BIMODAL_RANDOM (two modes, ~E[fail]=0.1 and ~E[fail=0.9] such that the overall E[fail] = 0.7), or
	 *             BIMODAL_CORRELATED (two modes, highly-sensitized patients ~E[fail]=0.9, lowly-sensitized ~E[fail]=0.1
	 *             BIMODAL_CORRELATED_{UNOS,APD,NKR} (same as BIMODAL_CORRELATED but fed aggregate data from real exchanges)
	 * @param r Random instance
	 */
	public static void setFailureProbability(Pool pool, ProbabilityDistribution dist, Random r, double param1) {

		if(dist.equals(ProbabilityDistribution.NONE)) {
			return;
		}

		for(Edge e : pool.edgeSet()) {

			if(pool.getEdgeTarget(e).isAltruist()) {
				// Dummy edges going to altruists cannot fail
				e.setFailureProbability(0.0);
			} else {
				// Real edges (real donor giving to real patient) can fail
				// Safe cast; earlier if statement checks for Altruist
				double patient_cpra = ((VertexPair) pool.getEdgeTarget(e)).getPatientCPRA();

				switch(dist) {
				case CONSTANT:
					// Constant 70% chance of failure
					e.setFailureProbability(param1);
					break;
				case BIMODAL_RANDOM:
					if(r.nextDouble() < 0.25) {
						// E[10%] chance of failure
						e.setFailureProbability(0.0 + r.nextDouble()*0.2);
					} else {
						// E[90%] chance of failure
						e.setFailureProbability(0.8 + r.nextDouble()*0.2);
					}
					break;
				case BIMODAL_CORRELATED:
				case BIMODAL_CORRELATED_UNOS:
					if(patient_cpra < 0.8) {   // CPRA<80 = UNOS lowly-sensitized
						// E[10%] chance of failure
						//e.setFailureProbability(0.0 + r.nextDouble()*0.2);
						e.setFailureProbability(0.1);
					} else {
						// E[90%] chance of failure
						//e.setFailureProbability(0.8 + r.nextDouble()*0.2);
						e.setFailureProbability(0.9);
					}
					break;
				case BIMODAL_CORRELATED_APD:
					if(patient_cpra < 0.75) {   // CPRA<75 = APD highest sensitization level (from Ashlagi et al. "Nonsimultaneous Chains and Dominos in Kidney Paired Donation--Revisited")
						// E[28%] chance of failure   :  20% crossmatch failure, 8% exogenous failure rate
						//e.setFailureProbability(0.18 + r.nextDouble()*0.2);
						e.setFailureProbability(0.28);
					} else {
						// E[58%] chance of failure
						//e.setFailureProbability(0.48 + r.nextDouble()*0.2);
						e.setFailureProbability(0.58);
					}
					break;
				case BIMODAL_CORRELATED_NKR:
					throw new UnsupportedOperationException("Haven't implemented the NKR correlated bimodal failure distribution");
					//break;
				case NONE:
				default:
					break;
				}
			}
		}
	}

	/**
	 * Given a set of assumed-to-be-disjoint cycles, calculates the total discounted utility 
	 * of the matching (i.e., if forceCardinality=1, then this is the expected number of transplants)
	 * @param cycles set of vertex-disjoint cycles
	 * @param pool full pool in which these cycles exist
	 * @param forceCardinality should we count edges as weight=1 or weight=assigned weight?
	 * @return 
	 */
	public static double calculateDiscountedMatchUtility(Set<Cycle> cycles, Pool pool, boolean forceCardinality) {

		if(null == cycles || null == pool) { return 0.0; }

		double utilSum = 0.0;
		for(Cycle c : cycles) {
			if(Cycle.isAChain(c, pool)) {
				utilSum += FailureProbabilityUtil.calculateDiscountedChainUtility(c, pool, pool.vertexSet(), forceCardinality);
			} else {
				utilSum += FailureProbabilityUtil.calculateDiscountedCycleUtility(c, pool, pool.vertexSet(), forceCardinality);
			}
		}
		return utilSum;
	}


	public static double calculateDiscountedCycleUtility(Cycle c, Pool pool, Set<Vertex> specialV) {
		return calculateDiscountedCycleUtility(c, pool, specialV, false);
	}

	public static double calculateDiscountedCycleUtility(Cycle c, Pool pool, Set<Vertex> specialV, boolean forceCardinality) {

		double utilSum = 0.0;
		double succProb = 1.0;
		for(Edge e : c.getEdges()) {
			Vertex recipient = pool.getEdgeTarget(e);
			if(specialV.contains(recipient)) {
				utilSum += ( forceCardinality ? 1.0 : pool.getEdgeWeight(e) );
			}
			succProb *= (1.0 - e.getFailureProbability());
		}

		return utilSum * succProb;
	}
	
	public static double calculateDiscountedChainUtility(Cycle c, Pool pool, Set<Vertex> specialV) {
		return calculateDiscountedChainUtility(c, pool, specialV, false);
	}

	public static double calculateDiscountedChainUtility(Cycle c, Pool pool, Set<Vertex> specialV, boolean forceCardinality) {

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
				rawPathWeight += ( forceCardinality ? 1.0 : pool.getEdgeWeight(e) );
			}
			edgeIdx++;
		}

		return discountedPathWeight;
	}
	
	/*
	 * Overloads for use by EthicalCPLEXSolver below this point.
	 * All vertices in pools must be EthicalVertexPairs.
	 * Vertex weights are determined by "ethical" characteristics of recipient.
	 */
	
	public static double calculateDiscountedCycleUtility(Cycle c, Pool pool) {
		return calculateDiscountedCycleUtility(c, pool, false);
	}

	public static double calculateDiscountedCycleUtility(Cycle c, Pool pool, boolean forceCardinality) {

		double utilSum = 0.0;
		double succProb = 1.0;
		for(Edge e : c.getEdges()) {
			EthicalVertexPair recipient = (EthicalVertexPair) pool.getEdgeTarget(e);
			utilSum += ( forceCardinality ? 1.0 : recipient.getWeight() );
			succProb *= (1.0 - e.getFailureProbability());
		}

		return utilSum * succProb;
	}
	
	public static double calculateDiscountedChainUtility(Cycle c, Pool pool) {
		return calculateDiscountedChainUtility(c, pool, false);
	}

	public static double calculateDiscountedChainUtility(Cycle c, Pool pool, boolean forceCardinality) {

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
			
			EthicalVertexPair recipient = (EthicalVertexPair) pool.getEdgeTarget(e);
			rawPathWeight += ( forceCardinality ? 1.0 : recipient.getWeight() );

			edgeIdx++;
		}

		return discountedPathWeight;
	}

}
