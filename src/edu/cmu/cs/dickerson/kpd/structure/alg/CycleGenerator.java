package edu.cmu.cs.dickerson.kpd.structure.alg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;

public class CycleGenerator {

	private final Pool pool; 
	public CycleGenerator(Pool pool) {
		this.pool = pool;
	}

	public List<Cycle> generateCyclesAndChains(int maxCycleSize, int maxChainSize) {
		// By default, generate cycles without paying attention to failure probabilities
		return generateCyclesAndChains(maxCycleSize, maxChainSize, false);
	}

	/**
	 * 
	 * @param pool
	 * @param maxCycleSize
	 * @param maxChainSize
	 * @return
	 */	
	public List<Cycle> generateCyclesAndChains(int maxCycleSize, int maxChainSize, boolean usingFailureProbabilities) {
		return generateCyclesAndChains(maxCycleSize, maxChainSize, usingFailureProbabilities, false, 0.5);
	}


	/**
	 * 
	 * @param maxCycleSize
	 * @param maxChainSize
	 * @param usingFailureProbabilities
	 * @param addInfiniteTailUtility
	 * @param infiniteTailFailureProb
	 * @return
	 */
	public List<Cycle> generateCyclesAndChains(int maxCycleSize, int maxChainSize, boolean usingFailureProbabilities, boolean addInfiniteTailUtility, double infiniteTailFailureProb) {

		if(addInfiniteTailUtility && (infiniteTailFailureProb <= 0.0 || infiniteTailFailureProb >= 1.0)) { throw new IllegalArgumentException("infiniteFailureProb must be in (0,1); your value=" + infiniteTailFailureProb); }
		if(!usingFailureProbabilities && addInfiniteTailUtility) { throw new IllegalArgumentException("Infinite tail extension without failure probabilities is infinite; arguments don't make sense."); }

		IOUtil.dPrintln(getClass().getSimpleName(), "Generating all (at-most) " + maxCycleSize + "-cycles and " + maxChainSize + "-chains ...");

		if(maxCycleSize < 0 || maxChainSize < 0) {
			throw new IllegalArgumentException("Maximum (cycle, chain) length must be nonnegative.  For infinite length, please use Integer.MAX_INT.  For zero length, please use 0.");
		}

		List<Cycle> generatedCycles = new ArrayList<Cycle>();

		for(Vertex startV : pool.vertexSet()) {
			generatedCycles.addAll(generateCyclesAndChainsForOneVertex(
					startV,
					maxCycleSize,
					maxChainSize,
					usingFailureProbabilities,
					addInfiniteTailUtility,
					infiniteTailFailureProb,
					true
					));
		}

		IOUtil.dPrintln(getClass().getSimpleName(), "Generated " + generatedCycles.size() + " cycles and chains.");
		return generatedCycles;
	}

	
	/**
	 * Given a distinguished vertex startV, generates all cycles that include 
	 * startV (if startV is a pair) or all chains that start from startV (if
	 * startV is an altruistic donor), where cycles and chains are capped and
	 * valued according to the parameters passed into the method call
	 * @param startV
	 * @param maxCycleSize
	 * @param maxChainSize
	 * @param usingFailureProbabilities
	 * @param addInfiniteTailUtility
	 * @param infiniteTailFailureProb
	 * @param generatingForAllVertices if true, assumes you are calling this method repeatedly 
	 *        and will short-circuit based on vertex ID to not generate duplicate cycles. If
	 *        this is true and you are not separately looping over all vertices, this method
	 *        will NOT return all cycles containing a specific vertex
	 * @return
	 */
	public List<Cycle> generateCyclesAndChainsForOneVertex(Vertex startV, int maxCycleSize, int maxChainSize, boolean usingFailureProbabilities, boolean addInfiniteTailUtility, double infiniteTailFailureProb, boolean generatingForAllVertices) {
		
		List<Cycle> generatedCycles = new ArrayList<Cycle>();

		// Keep track of our path from the start node (startV) to the current node
		Deque<Edge> path = new ArrayDeque<Edge>();

		// Test whether vertices are in the current path (only want simple cycles)
		Set<Vertex> inPath = new HashSet<Vertex>();
		inPath.add(startV);

		for(Edge startE : pool.outgoingEdgesOf(startV)) {

			Vertex nextV = pool.getEdgeTarget(startE);

			// Initial path weight is just the single edge's weight
			double pathWeight = pool.getEdgeWeight(startE);
			// Probability of the first edge executing is 1-probability of it failing
			double pathSuccProb = (1.0 - startE.getFailureProbability());

			// Generate all cycles or all chains starting from the vertex firstV
			path.push(startE);
			if(startV.isAltruist()) {
				double discountedPathWeight = 0.0;
				generateChains(maxChainSize, generatedCycles, startV, nextV, path, pathWeight, inPath, usingFailureProbabilities, pathSuccProb, discountedPathWeight, addInfiniteTailUtility, infiniteTailFailureProb);
			} else {
				// If the target hop has a lower ID than the source, we've generated these cycles already
				// assuming we are calling this method for all vertices; otherwise, ignore this conditional
				if( !generatingForAllVertices || nextV.getID() > startV.getID() ) {
					generateCycles(
							maxCycleSize, 
							generatedCycles, 
							startV, 
							nextV, 
							path, 
							pathWeight, 
							inPath, 
							usingFailureProbabilities, 
							pathSuccProb,
							generatingForAllVertices
							);
				}
			}
			path.pop();
		}
		
		return generatedCycles;
	}


	private void generateCycles(final int maxCycleSize, Collection<Cycle> cycles, Vertex startV, Vertex lastV, Deque<Edge> path, double pathWeight, Set<Vertex> inPath, final boolean usingFailureProbabilities, double pathSuccProb, final boolean generatingForAllVertices) {

		if(startV.equals(lastV)) {
			// We've completed a cycle <startV, V1, V2, ..., lastV=startV>

			// If we're using failure probabilities, the discounted utility of a cycle is:
			// u(c) = \prod_e (1-fail(e))  *  \sum_e weight(e)
			if(usingFailureProbabilities) {
				pathWeight *= pathSuccProb;
			}

			cycles.add( Cycle.makeCycle(path, pathWeight));
		} else {

			if(inPath.contains(lastV)              // Must be a simple cycle
					|| path.size() >= maxCycleSize // Cap cycle length to maxCycleSize
					|| lastV.isAltruist()          // Only generate cycles, not chains
					|| maxCycleSize <= 0           // Sanity check
					) { 
				return;
			}

			// Explore the subtree rooted at lastV
			inPath.add(lastV);
			for(Edge nextE : pool.outgoingEdgesOf(lastV)) {

				// Target hop has a lower ID than the source, so we'll generate this cycle elsewhere
				// (conditioned on generatingAllVertices; if that's false, then we can't short circuit here)
				if(generatingForAllVertices && pool.getEdgeTarget(nextE).getID() < startV.getID()) { continue; }

				// Step down one edge in the path, updating the pathWeight as well
				path.push(nextE);
				generateCycles(maxCycleSize,
						cycles,
						startV,
						pool.getEdgeTarget(nextE),
						path,
						pathWeight + pool.getEdgeWeight(nextE),
						inPath,
						usingFailureProbabilities,
						pathSuccProb * (1.0 - nextE.getFailureProbability()),
						generatingForAllVertices
						);
				path.pop();

			}
			inPath.remove(lastV);
		}
	}

	private void generateChains(final int maxChainSize, Collection<Cycle> cycles, Vertex startingAlt, Vertex lastV, Deque<Edge> path, double rawPathWeight, Set<Vertex> inPath, final boolean usingFailureProbabilities, double pathSuccProb, double discountedPathWeight, final boolean addInfiniteTailUtility, final double infiniteTailFailureProb) {

		if(inPath.contains(lastV)               // Must be a simple cycle
				|| path.size() > maxChainSize   // Cap chain length to maxChainSize (ignore altruist)
				|| maxChainSize <= 0            // Sanity check
				) {
			return;
		}

		inPath.add(lastV);
		for(Edge nextE : pool.outgoingEdgesOf(lastV)) {


			Vertex nextV = pool.getEdgeTarget(nextE);
			if(nextV.isAltruist()) {

				// If we've bounced back to an altruist who isn't the starting altruist, ignore; else, add this chain
				if(!nextV.equals(startingAlt)) { continue; }
				else {
					path.push(nextE);
					if(!usingFailureProbabilities) {
						cycles.add( Cycle.makeCycle(path, rawPathWeight + pool.getEdgeWeight(nextE)) );
					} else {


						// Adds geometric sum to end of tail if the chain is max-length (successProb = 1.0-failureProb)
						if(addInfiniteTailUtility && path.size()==maxChainSize) {
							discountedPathWeight += ( Math.pow(1.0-infiniteTailFailureProb, maxChainSize) / (infiniteTailFailureProb) );
						}

						// We assume the dummy edge is infallible, but it might be nonzero weight, so add that
						// Also add the probability of the chain executing in its entirety (sum of weights * product of success probs)
						cycles.add( Cycle.makeCycle(path, discountedPathWeight + (rawPathWeight * pathSuccProb) + pool.getEdgeWeight(nextE)) );
					}
					path.pop();
				}

			} else {
				// Step down one edge in the path, updating the pathWeight as well
				path.push(nextE);

				// Probability of chain executing to very end (and maybe continuing)
				double newPathSuccProb = pathSuccProb * (1.0 - nextE.getFailureProbability());
				// Add discounted utility of chain goings to EXACTLY this edge and then failing (so \sum weights * \prod success * (1-failure of this edge))
				double newDiscountedPathWeight = discountedPathWeight + (rawPathWeight*pathSuccProb*nextE.getFailureProbability());

				generateChains(maxChainSize,
						cycles,
						startingAlt,
						nextV,
						path,
						rawPathWeight + pool.getEdgeWeight(nextE),    // Add this edge's weight to raw chain length
						inPath,
						usingFailureProbabilities,
						newPathSuccProb,
						newDiscountedPathWeight,   // Discounted weight + discounted utility of reaching exactly this point
						addInfiniteTailUtility,
						infiniteTailFailureProb
						);
				path.pop();
			}

		}
		inPath.remove(lastV);


	}
}
