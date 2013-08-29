package edu.cmu.cs.dickerson.kpd.structure.alg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;

public class CycleGenerator {

	private final Pool pool; 
	public CycleGenerator(Pool pool) {
		this.pool = pool;
	}

	/**
	 * 
	 * @param pool
	 * @param maxCycleSize
	 * @param maxChainSize
	 * @return
	 */
	public List<Cycle> generateCyclesAndChains(int maxCycleSize, int maxChainSize) {

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

				// Generate all cycles or all chains starting from the vertex firstV
				path.push(startE);
				if(startV.isAltruist()) {
					generateChains(maxChainSize, generatedCycles, startV, nextV, path, pathWeight, inPath);
				} else {
					// If the target hop has a lower ID than the source, we've generated these cycles already
					if( nextV.getID() > startV.getID() ) {
						generateCycles(maxCycleSize, generatedCycles, startV, nextV, path, pathWeight, inPath);
					}
				}
				path.pop();
			}
		}

		return generatedCycles;
	}

	private void generateCycles(int maxCycleSize, Collection<Cycle> cycles, Vertex startV, Vertex lastV, Deque<Edge> path, double pathWeight, Set<Vertex> inPath) {

		if(startV.equals(lastV)) {
			// We've completed a cycle <startV, V1, V2, ..., lastV=startV>
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
					inPath
					);
				path.pop();
				
			}
			inPath.remove(lastV);
		}
	}

	private void generateChains(int maxChainSize, Collection<Cycle> cycles, Vertex startingAlt, Vertex lastV, Deque<Edge> path, double pathWeight, Set<Vertex> inPath) {

		if(inPath.contains(lastV)               // Must be a simple cycle
				|| path.size()-1 > maxChainSize // Cap chain length to maxChainSize (ignore altruist)
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
					cycles.add( Cycle.makeCycle(path, pathWeight + pool.getEdgeWeight(nextE)) );
					path.pop();
				}
				
			} else {
				// Step down one edge in the path, updating the pathWeight as well
				path.push(nextE);
				generateChains(maxChainSize,
					cycles,
					startingAlt,
					pool.getEdgeTarget(nextE),
					path,
					pathWeight + pool.getEdgeWeight(nextE),
					inPath
					);
				path.pop();
			}
			
		}
		inPath.remove(lastV);
	
	
	}
}
