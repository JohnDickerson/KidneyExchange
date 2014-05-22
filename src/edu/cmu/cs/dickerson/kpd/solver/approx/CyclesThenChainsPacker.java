package edu.cmu.cs.dickerson.kpd.solver.approx;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.solver.approx.VertexShufflePacker.ShuffleType;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class CyclesThenChainsPacker extends Packer {

	private List<Cycle> fullCycles;
	private CycleMembership fullMembership;
	private List<Cycle> reducedCycles;
	private CycleMembership reducedMembership;
	private Pool pool;
	private int maxChainSize;
	private boolean usingFailureProbabilities;

	private CycleLPRelaxationPacker cyclePacker = null;
	private VertexRandomWalkPacker chainPacker = null;
	private boolean isInitialized = false;

	public CyclesThenChainsPacker(Pool pool, List<Cycle> fullCycles, CycleMembership fullMembership, List<Cycle> reducedCycles, CycleMembership reducedMembership, boolean doInitialization, int maxChainSize, boolean usingFailureProbabilities) {
		this.pool = pool;
		this.fullCycles = fullCycles;
		this.fullMembership = fullMembership;
		this.reducedCycles = reducedCycles;
		this.reducedMembership = reducedMembership;
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
		this.chainPacker = new VertexRandomWalkPacker(pool, fullCycles, fullMembership, ShuffleType.INVERSE_PROP_CYCLE_COUNT, maxChainSize, usingFailureProbabilities);
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
		
		
		for(Vertex alt : pool.getAltruists()) {

			// Can't sample any chains from isolated altruists
			if(pool.outgoingEdgesOf(alt).isEmpty()) { continue; }

			Cycle chain = chainPacker.sampleAChain(alt, matchedVerts, maxChainSize, usingFailureProbabilities);
			
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
