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

public class CyclesThenChainsIPPacker extends Packer {

	private List<Cycle> reducedCycles;
	private CycleMembership reducedMembership;
	private Pool pool;
	private int chainSamplesPerAltruist;   // constant number of chains (UB) to sample from each altruist
	private int maxChainSize;
	private boolean usingFailureProbabilities;

	private CycleLPRelaxationPacker cyclePacker = null;
	private ChainSampler chainSampler = null;
	private boolean isInitialized = false;

	public CyclesThenChainsIPPacker(Pool pool, List<Cycle> reducedCycles, CycleMembership reducedMembership, int chainSamplesPerAltruist, boolean doInitialization, int maxChainSize, boolean usingFailureProbabilities) {
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
	}

	@Override
	public Solution pack(double upperBound) {

		if(!isInitialized) { init(); }

		long start = System.nanoTime();

		// Pack only 2- and 3-cycles into a fake matching first
		Solution solCycOnly = cyclePacker.pack(upperBound);
		Set<Vertex> matchedVerts = new HashSet<Vertex>( 
				Cycle.getConstituentVertices(solCycOnly.getMatching(), pool)
				);

		// Sample a set of chains per altruist
		Set<Cycle> sampledChains = new HashSet<Cycle>();
		for(Vertex alt : pool.getAltruists()) {

			// Can't sample any chains from isolated altruists
			if(pool.outgoingEdgesOf(alt).isEmpty()) { continue; }

			// Sample a set of chains
			for(int sampleIdx=0; sampleIdx<chainSamplesPerAltruist; sampleIdx++) {
				Cycle chain = chainSampler.sampleAChain(alt, matchedVerts, maxChainSize, usingFailureProbabilities);
				if(null != chain) {
					sampledChains.add(chain);
				}
			}
		}
		
		// Add sampled chains to 2- and 3-cycles, recompute membership
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
		
		// Done
		long end = System.nanoTime();
		long totalTime = end - start;

		sol.setSolveTime(totalTime);
		return sol;
	}

}
