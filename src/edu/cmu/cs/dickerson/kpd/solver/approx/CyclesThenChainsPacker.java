package edu.cmu.cs.dickerson.kpd.solver.approx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class CyclesThenChainsPacker extends Packer {

	private List<Cycle> reducedCycles;
	private CycleMembership reducedMembership;
	private Pool pool;
	private int chainSamplesPerAltruist;   // constant number of chains (UB) to sample from each altruist
	private int maxChainSize;
	private boolean usingFailureProbabilities;

	private CycleLPRelaxationPacker cyclePacker = null;
	private ChainSampler chainSampler = null;
	private boolean isInitialized = false;

	public CyclesThenChainsPacker(Pool pool, List<Cycle> reducedCycles, CycleMembership reducedMembership, int chainSamplesPerAltruist, boolean doInitialization, int maxChainSize, boolean usingFailureProbabilities) {
		this.pool = pool;
		this.reducedCycles = reducedCycles;
		this.reducedMembership = reducedMembership;
		this.chainSamplesPerAltruist = chainSamplesPerAltruist;
		this.maxChainSize = maxChainSize;
		this.usingFailureProbabilities = usingFailureProbabilities;
		if(doInitialization) { init(); }
	}

	/**
	 * Want to solve the LP relaxation that we use for step 1 exactly once (then do random packs
	 * based off that initial LP relax solve quickly many times in the pack method)
	 */
	private void init() {
		this.cyclePacker = new CycleLPRelaxationPacker(pool, reducedCycles, reducedMembership, true);
		this.chainSampler = new ChainSampler(pool);
		this.isInitialized = true;
	}

	@Override
	public Solution pack(double upperBound) {

		if(!isInitialized) { init(); }

		double objVal = 0.0;
		long start = System.nanoTime();

		// Pack only 2- and 3-cycles first
		Solution solCycOnly = cyclePacker.pack(upperBound);
		objVal += solCycOnly.getObjectiveValue();
		Set<Cycle> matching = solCycOnly.getMatching();
		Set<Vertex> matchedVerts = new HashSet<Vertex>( 
				Cycle.getConstituentVertices(solCycOnly.getMatching(), pool)
				);


		List<Vertex> shuffledAlts = new ArrayList<Vertex>(pool.getAltruists());
		Collections.shuffle(shuffledAlts);
		for(Vertex alt : shuffledAlts) {

			// Can't sample any chains from isolated altruists
			if(pool.outgoingEdgesOf(alt).isEmpty()) { continue; }

			// Sample at most K chains from altruist, save the longest/highest weight
			Cycle longestChain = null;
			for(int sampleIdx=0; sampleIdx<chainSamplesPerAltruist; sampleIdx++) {
				Cycle chain = chainSampler.sampleAChain(alt, matchedVerts, maxChainSize, usingFailureProbabilities);
				if(null==longestChain ||    // any chain (including null) is at least as good as null
						(null!=chain && longestChain.getWeight() < chain.getWeight()) // sampled chain better than current best
						) {
					longestChain = chain;
				}
			}

			// Couldn't find a legal path from this altruist
			if(null==longestChain) { continue; }

			// We check legality of the chain during generation, so add all verts and chain to matching
			Set<Vertex> cVerts = Cycle.getConstituentVertices(longestChain, pool);
			matchedVerts.addAll(cVerts);
			objVal += longestChain.getWeight();
			matching.add(longestChain);

			// If we hit the upper bound, break out
			if(objVal >= upperBound) {
				break;
			}
		}


		long end = System.nanoTime();
		long totalTime = end - start;

		// Construct union of cycles-only and chains-only matchings
		Solution sol = new Solution();
		sol.setMatching(matching);
		sol.setObjectiveValue(objVal);
		sol.setSolveTime(totalTime);
		return sol;
	}



}
