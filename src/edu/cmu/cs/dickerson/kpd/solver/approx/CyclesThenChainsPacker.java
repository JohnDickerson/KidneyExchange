package edu.cmu.cs.dickerson.kpd.solver.approx;

import java.util.List;

import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class CyclesThenChainsPacker extends Packer {

	private List<Cycle> cycles;
	private CycleMembership membership;
	private Pool pool;
	
	private CycleLPRelaxationPacker cyclePacker = null;
	private boolean isInitialized = false;

	public CyclesThenChainsPacker(Pool pool, List<Cycle> cycles, CycleMembership membership, boolean doInitialization) {
		this.pool = pool;
		this.cycles = cycles;
		this.membership = membership;
		if(doInitialization) { init(); }
	}
	
	/**
	 * Want to solve the LP relaxation that we use for step 1 exactly once
	 */
	private void init() {
		this.cyclePacker = new CycleLPRelaxationPacker(this.pool, this.cycles, this.membership, true);
	}
	
	@Override
	public Solution pack(double upperBound) {
		
		if(!isInitialized) { init(); }

		// Pack only 2- and 3-cycles first
		return null;
	}
	
	

}
