package edu.cmu.cs.dickerson.kpd.solver.approx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

/**
 * <p>Given the set of all 2- and 3-cycles, samples some small set of chains and feeds
 * them into the cycle formulation IP solver.</p>
 * 
 * @author John P. Dickerson
 * @since 1.0
 * @see Packer
 * @see CycleFormulationCPLEXSolver
 */
public class CyclesSampleChainsIPPacker extends Packer {

	private List<Cycle> reducedCycles;
	private Pool pool;
	private int chainSamplesPerAltruist;   // constant number of chains (UB) to sample from each altruist
	private int maxChainSize;
	private boolean usingFailureProbabilities;

	public CyclesSampleChainsIPPacker(Pool pool, List<Cycle> reducedCycles, int chainSamplesPerAltruist, int maxChainSize, boolean usingFailureProbabilities) {
		this.pool = pool;
		this.reducedCycles = reducedCycles;
		this.chainSamplesPerAltruist = chainSamplesPerAltruist;
		this.maxChainSize = maxChainSize;
		this.usingFailureProbabilities = usingFailureProbabilities;
	}
	
	@Override
	public Solution pack(double upperBound) {
		
		long start = System.nanoTime();

		// We have the entire set of cycles precomputed already, so just sample chains before IP solve
		ChainSampler chainSampler = new ChainSampler(pool);
		Set<Vertex> matchedVerts = new HashSet<Vertex>();   // all chains totally independent, let the IP sort it out; no matched vertices
		
		// Sample at most K chains for each altruist (ordering of alts doesn't matter)
		Set<Cycle> sampledChains = new HashSet<Cycle>();
		for(Vertex alt : pool.getAltruists()) {

			// Can't sample any chains from isolated altruists
			if(pool.outgoingEdgesOf(alt).isEmpty()) { continue; }

			// Sample at most K chains from altruist, save the longest/highest weight
			for(int sampleIdx=0; sampleIdx<chainSamplesPerAltruist; sampleIdx++) {
				Cycle chain = chainSampler.sampleAChain(alt, matchedVerts, maxChainSize, usingFailureProbabilities);
				if(null!=chain) {
					sampledChains.add(chain);
				}
			}
		}
		
		// Make a new CycleMembership list for the IP solve.  This is needlessly heavyweight (we could just update the
		// the CycleMembership and list of Cycles passed in), but we're running repeated experiments so no go.  Remove in practice.
		List<Cycle> allCyclesForIP = new ArrayList<Cycle>(reducedCycles);
		allCyclesForIP.addAll(sampledChains);
		CycleMembership cycleMembershipForIP = new CycleMembership(pool, allCyclesForIP);
		
		// IP solve on reduced cycle+chain set
		Solution sol = null;
		try {
			CycleFormulationCPLEXSolver s = new CycleFormulationCPLEXSolver(pool, allCyclesForIP, cycleMembershipForIP);
			sol = s.solve();
		} catch(SolverException e) {
			e.printStackTrace();
			return null;
		}
		
		long end = System.nanoTime();
		long totalTime = end - start;
		sol.setSolveTime(totalTime);  // want to include chain generation + IP solve time, not just IP solve time
		
		return sol;
	}

}
