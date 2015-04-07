package edu.cmu.cs.dickerson.kpd.competitive;

import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;

public class UniformRandomMatchingStrategy extends MatchingStrategy {

	private int maxCycleSize;
	private int maxChainSize;
	private boolean usingFailureProbabilities;
	private boolean addInfiniteTailUtility;
	private double infiniteTailFailureProb;
	private Random r;

	public UniformRandomMatchingStrategy(int maxCycleSize, int maxChainSize,
			boolean usingFailureProbabilities, boolean addInfiniteTailUtility,
			double infiniteTailFailureProb, Random r) {
		super();
		this.maxCycleSize = maxCycleSize;
		this.maxChainSize = maxChainSize;
		this.usingFailureProbabilities = usingFailureProbabilities;
		this.addInfiniteTailUtility = addInfiniteTailUtility;
		this.infiniteTailFailureProb = infiniteTailFailureProb;
		this.r = r;
	}

	@Override
	public Cycle match(Vertex v, Pool pool) {
		
		// First, generate all cycles that contain this vertex
		CycleGenerator gen = new CycleGenerator(pool);
		List<Cycle> cycles = gen.generateCyclesAndChainsForOneVertex(
				v, 
				this.maxCycleSize, 
				this.maxChainSize, 
				this.usingFailureProbabilities, 
				this.addInfiniteTailUtility,
				this.infiniteTailFailureProb,
				false);

		// Randomly select one cycle from this set of cycles at random
		if(cycles.isEmpty()) {
			return null;
		} else {
			int randomIdx = r.nextInt(cycles.size());
			return cycles.get(randomIdx);
		}
	}

}
