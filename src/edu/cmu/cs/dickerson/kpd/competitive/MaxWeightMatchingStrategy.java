package edu.cmu.cs.dickerson.kpd.competitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;

public class MaxWeightMatchingStrategy extends MatchingStrategy {

	private int maxCycleSize;
	private int maxChainSize;
	private boolean usingFailureProbabilities;
	private boolean addInfiniteTailUtility;
	private double infiniteTailFailureProb;
	private Random r;

	public MaxWeightMatchingStrategy(int maxCycleSize, int maxChainSize,
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

		// Take set of top-weighted cycles and select one of them randomly
		if(cycles.isEmpty()) {
			return null;
		} else {
			
			// Get a list of the max-weight cycles (often more than one in the max cardinality case)
			double maxWeight = Double.NEGATIVE_INFINITY;
			List<Cycle> maxWeightCycles = new ArrayList<Cycle>();
			for(Cycle cycle : cycles) {
				double cycleWeight = cycle.getWeight();
				if(cycleWeight >= maxWeight) {
					// Clear the list of old max-weighted cycles; they're not the top anymore
					if(cycleWeight > maxWeight) {
						maxWeightCycles.clear();
					}
					maxWeight = cycleWeight;
					maxWeightCycles.add(cycle);
				}
			}
			
			// Pick a random cycle from the list of max-weight cycles
			int randomIdx = r.nextInt(maxWeightCycles.size());
			return maxWeightCycles.get(randomIdx);
		}
	}
}
