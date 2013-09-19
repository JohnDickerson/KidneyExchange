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

		IOUtil.dPrintln(getClass().getSimpleName(), "Generating all (at-most) " + maxCycleSize + "-cycles and " + maxChainSize + "-chains ...");
		
		if(maxCycleSize < 0 || maxChainSize < 0) {
			throw new IllegalArgumentException("Maximum (cycle, chain) length must be nonnegative.  For infinite length, please use Integer.MAX_INT.  For zero length, please use 0.");
		}
		
		List<Cycle> generatedCycles = new ArrayList<Cycle>();
		
		for(Vertex startV : pool.vertexSet()) {

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
					generateChains(maxChainSize, generatedCycles, startV, nextV, path, pathWeight, inPath, usingFailureProbabilities, pathSuccProb, discountedPathWeight);
				} else {
					// If the target hop has a lower ID than the source, we've generated these cycles already
					if( nextV.getID() > startV.getID() ) {
						generateCycles(maxCycleSize, generatedCycles, startV, nextV, path, pathWeight, inPath, usingFailureProbabilities, pathSuccProb);
					}
				}
				path.pop();
			}
		}

		IOUtil.dPrintln(getClass().getSimpleName(), "Generated " + generatedCycles.size() + " cycles and chains.");
		return generatedCycles;
	}

	private void generateCycles(int maxCycleSize, Collection<Cycle> cycles, Vertex startV, Vertex lastV, Deque<Edge> path, double pathWeight, Set<Vertex> inPath, boolean usingFailureProbabilities, double pathSuccProb) {

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
				if(pool.getEdgeTarget(nextE).getID() < startV.getID()) { continue; }

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
					pathSuccProb *= nextE.getFailureProbability()
					);
				path.pop();
				
			}
			inPath.remove(lastV);
		}
	}

	private void generateChains(int maxChainSize, Collection<Cycle> cycles, Vertex startingAlt, Vertex lastV, Deque<Edge> path, double rawPathWeight, Set<Vertex> inPath, boolean usingFailureProbabilities, double pathSuccProb, double discountedPathWeight) {

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
					newDiscountedPathWeight   // Discounted weight + discounted utility of reaching exactly this point
					);
				path.pop();
			}

		}
		inPath.remove(lastV);
	
	
	}
}
