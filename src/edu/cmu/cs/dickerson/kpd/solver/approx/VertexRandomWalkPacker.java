package edu.cmu.cs.dickerson.kpd.solver.approx;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.WeightedRandomSample;
import edu.cmu.cs.dickerson.kpd.solver.approx.VertexShufflePacker.ShuffleType;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class VertexRandomWalkPacker extends Packer {

	private Pool pool;
	private CycleMembership membership;
	private List<Cycle> cycles;

	private ShuffleType shuffleType;
	private int maxChainSize;
	private boolean usingFailureProbabilities;

	public VertexRandomWalkPacker(Pool pool, List<Cycle> cycles, CycleMembership membership, ShuffleType shuffleType, int maxChainSize, boolean usingFailureProbabilities) {
		this.pool = pool;
		this.cycles = cycles;
		this.membership = membership;
		this.shuffleType = shuffleType;
		this.maxChainSize = maxChainSize;
		this.usingFailureProbabilities = usingFailureProbabilities;
	}

	@Override
	public Solution pack(double upperBound) {

		Set<Cycle> matching = new HashSet<Cycle>();
		double objVal = 0.0;
		Set<Vertex> matchedVerts = new HashSet<Vertex>();

		long start = System.nanoTime();

		// First, use every altruist by packing chains
		if(maxChainSize > 1) {
			for(Vertex alt : pool.getAltruists()) {

				// Can't sample any chains from isolated altruists
				if(pool.outgoingEdgesOf(alt).isEmpty()) { continue; }

				Cycle chain = sampleAChain(alt, matchedVerts, maxChainSize, usingFailureProbabilities);
				
				// Couldn't find a legal path from this altruist
				if(null==chain) { continue; }
				
				// We check legality of the chain during generation, so add all verts and chain to matching
				Set<Vertex> cVerts = Cycle.getConstituentVertices(chain, pool);
				matchedVerts.addAll(cVerts);
				objVal += chain.getWeight();
				matching.add(chain);

				// If we hit the upper bound, break out
				if(objVal >= upperBound) {
					break;
				}
			}
		}

		// Second, pack remaining vertices in cycles (using a VertexShufflePacker)
		VertexShufflePacker cyclePacker = new VertexShufflePacker(this.pool, this.cycles, this.membership, this.shuffleType, matchedVerts);
		Solution cyclesOnly = cyclePacker.pack(upperBound - objVal);

		// Add these packed cycles to our full matching
		matching.addAll(cyclesOnly.getMatching());
		objVal += cyclesOnly.getObjectiveValue();

		long end = System.nanoTime();
		long totalTime = end - start;

		// Construct formal matching, return
		Solution sol = new Solution();
		sol.setMatching(matching);
		sol.setObjectiveValue(objVal);
		sol.setSolveTime(totalTime);
		return sol;
	}


	/**
	 * Random walks a chain from altruistic Vertex alt, sampling hops based on our weighting scheme.
	 * 
	 * @param alt Starting altruist for the chain
	 * @param matchedVerts Set of off-limits vertices for this chain
	 * @param maxChainSize Chain cap (will sample a chain <= maxChainSize)
	 * @param usingFailureProbabilities True if the chain's weight should be discounted, false if raw
	 * @return A chain starting at alt of size <= maxChainSize that random walks from alt through the legal
	 *   remaining vertices in the pool, sampling neighbors inversely proportional to the number of cycles
	 *   containing those neighbors.  Weight is discounted (usingFailureProbabilities=True) or raw (=False)
	 */
	protected Cycle sampleAChain(Vertex alt, Set<Vertex> matchedVerts, int maxChainSize, boolean usingFailureProbabilities) {

		if(null==alt) { throw new IllegalArgumentException("Altruist cannot be null."); }
		if(null==matchedVerts) { throw new IllegalArgumentException("Set of matched vertices cannot be null."); }
		if(maxChainSize < 2) { throw new IllegalArgumentException("Cannot sample chains if maxChainSize<2 (maxChainSize=" + maxChainSize); }
		if(pool.outgoingEdgesOf(alt).size() < 1) { throw new IllegalArgumentException("Altruist " + alt + " has no outgoing edges.  Cannot call sampleAChain."); }

		// Accumulate our chain's edges
		Deque<Edge> path = new ArrayDeque<Edge>();
		Set<Vertex> inPath = new HashSet<Vertex>();

		double pathSuccProb = 1.0;
		double discountedPathWeight = 0.0;
		double rawPathWeight = 0.0;

		Vertex currentV = alt;
		do {

			// Want to choose next hop inversely proportional to #cycles/chains containing it
			WeightedRandomSample<Edge> neighborSet = new WeightedRandomSample<Edge>();
			for(Edge edge : pool.outgoingEdgesOf(currentV)) {

				Vertex candidateV = pool.getEdgeTarget(edge);

				// If this neighbor has already been matched (or is in our chain), skip
				if(matchedVerts.contains(candidateV) || inPath.contains(candidateV)) { continue; }
				// If this neighbor is an altruist who isn't the starting altruist, skip
				if(candidateV.isAltruist() && !candidateV.equals(alt)) { continue; }

				// Never want to sample vertices that are not in any cycles (no chance of matching)
				double cycleCount = membership.getMembershipSet(candidateV).size();
				if(cycleCount == 0) { continue; }

				// Not worrying about overflow for now, since we won't be using this on big |cycle| counts
				double weight = (double) cycles.size() / cycleCount;
				neighborSet.add(weight, edge);

			}

			// Get our next hop in the chain, based on the weights computed above
			Edge nextE = null;
			if(path.size() >= maxChainSize - 1 || neighborSet.size() < 1) {
				
				// If we're at the last step of the chain due to a chain cap, or if no vertices
				// are both neighbors of this vertex AND unmatched, then try to hop back to the
				// starting altruist
				nextE = pool.getEdge(currentV, alt);
				if(null==nextE) {
					return null;
					//throw new RuntimeException("Starting with altruist " + alt + ", found a vertex that did not connect (dummy edge or otherwise) back to the altruist.\n" +
					//		"Vertex: " + currentV + ", neighbors: " + pool.outgoingEdgesOf(currentV));
				}
			} else {
				nextE = neighborSet.sampleWithoutReplacement();
			}


			path.push(nextE);
			Vertex nextV = pool.getEdgeTarget(nextE);		
			inPath.add(nextV);

			// If we're ending the chain, make a formal Cycle and return
			if(nextV.isAltruist() && nextV.equals(alt)) {

				// Add the discounted weight from this chain executing in its entirety
				discountedPathWeight += ((1.0-nextE.getFailureProbability())*pathSuccProb*rawPathWeight);
				break;

			} else {
				// Add discounted utility of chain goings to EXACTLY this edge and then failing (so \sum weights * \prod success * (1-failure of this edge))
				discountedPathWeight += (rawPathWeight*pathSuccProb*nextE.getFailureProbability());
				// Probability of chain executing to very end (and maybe continuing)
				pathSuccProb *= (1.0 - nextE.getFailureProbability());
				// We assume the chain gets this far, add edge to raw weight
				rawPathWeight += pool.getEdgeWeight(nextE);

				// We've hopped!
				currentV = nextV;
			}

		} while(true);

		// Construct a formal Cycle from the sampled path, and weight it accordingly
		if(!usingFailureProbabilities) {
			return Cycle.makeCycle(path, rawPathWeight);
		} else {
			return Cycle.makeCycle(path, discountedPathWeight);
		}		
	}
}
