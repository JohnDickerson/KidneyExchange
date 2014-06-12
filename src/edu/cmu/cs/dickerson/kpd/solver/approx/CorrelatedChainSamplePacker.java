package edu.cmu.cs.dickerson.kpd.solver.approx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class CorrelatedChainSamplePacker extends Packer {

	private List<Cycle> reducedCycles;
	private Pool pool;
	private int chainSamplesPerAltruist;   // constant number of chains (UB) to sample from each altruist
	private int maxChainSize;
	private boolean usingFailureProbabilities;

	public CorrelatedChainSamplePacker(Pool pool, List<Cycle> reducedCycles, int chainSamplesPerAltruist, int maxChainSize, boolean usingFailureProbabilities) {
		this.pool = pool;
		this.reducedCycles = reducedCycles;
		this.chainSamplesPerAltruist = chainSamplesPerAltruist;
		this.maxChainSize = maxChainSize;
		this.usingFailureProbabilities = usingFailureProbabilities;
	}


	@Override
	public Solution pack(double upperBound) {

		long start = System.nanoTime();

		// K times, randomize altruists and sample a chain from each in order
		ChainSampler chainSampler = new ChainSampler(pool);
		Set<Cycle> sampledChains = new HashSet<Cycle>();
		for(int chainIdx=0; chainIdx<chainSamplesPerAltruist; chainIdx++) {
			
			// Reset the matched vertices every round 
			Set<Vertex> matchedVerts = new HashSet<Vertex>();
			
			// Choose a random ordering over altruists
			List<Vertex> shuffledAlts = new ArrayList<Vertex>(pool.getAltruists());
			Collections.shuffle(shuffledAlts);
			for(Vertex alt : shuffledAlts) {
				
				// Can't sample any chains from isolated altruists
				if(pool.outgoingEdgesOf(alt).isEmpty()) { continue; }

				// Try to sample a chain and, if successful, don't sample from its
				// constituent vertices in future rounds of altruist sampling
				Cycle chain = chainSampler.sampleAChain(alt, matchedVerts, maxChainSize, usingFailureProbabilities);
				if(null!=chain) {
					Set<Vertex> cVerts = Cycle.getConstituentVertices(chain, pool);
					matchedVerts.addAll(cVerts);	
					sampledChains.add(chain);
				}
			}
		}
		
		IOUtil.dPrintln("Sampled " + sampledChains.size() + " chains (expected " + (pool.getAltruists().size()*chainSamplesPerAltruist) + ")");
		
		// Make a new CycleMembership list for the LP solve.  This is needlessly heavyweight (we could just update the
		// the CycleMembership and list of Cycles passed in), but we're running repeated experiments so no go.  Remove in practice.
		List<Cycle> allCyclesForLP = new ArrayList<Cycle>(reducedCycles);
		allCyclesForLP.addAll(sampledChains);
		CycleMembership cycleMembershipForLP = new CycleMembership(pool, allCyclesForLP);
				
		// Now solve the LP relaxation on all 2-, 3-cycles and the sampled chains
		CycleLPRelaxationPacker cyclePacker = new CycleLPRelaxationPacker(pool, allCyclesForLP, cycleMembershipForLP, true);		
		Solution sol = cyclePacker.pack(upperBound);


		// Update solve time to include sampling
		long end = System.nanoTime();
		long totalTime = end - start;
		sol.setSolveTime(totalTime);
		return sol;
	}


}
