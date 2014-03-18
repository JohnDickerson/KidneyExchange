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

public class CycleShufflePacker extends Packer {

	private List<Cycle> cycles;
	private Pool pool;
	
	public CycleShufflePacker(Pool pool, List<Cycle> cycles) {
		this.pool = pool;
		// Create a copy of the Cycle list, since its order will be shuffled and future membership 
		// queries will be incorrect
		this.cycles = new ArrayList<Cycle>(cycles);
	}
	
	@Override
	public Solution pack(double upperBound) {
		Set<Cycle> matching = new HashSet<Cycle>();
		double objVal = 0.0;

		// Pack cycles
		long start = System.nanoTime();

		// Keep track of which vertices are in the matching so far
		Set<Vertex> matchedVerts = new HashSet<Vertex>();

		// "Sample" randomly from the set of all cycles until either we run out of
		// cycles, or we hit the parameter upperBound
		Collections.shuffle(cycles);
		for(Cycle cycle : cycles) {

			Set<Vertex> cVerts = Cycle.getConstituentVertices(cycle, pool);

			// If no vertices in this cycle are matched, it's legal to add; add it
			if(Collections.disjoint(cVerts, matchedVerts)) {
				matchedVerts.addAll(cVerts);
				objVal += cycle.getWeight();
				matching.add(cycle);
			}
			
			// If we hit the upper bound, break out
			if(objVal >= upperBound) {
				break;
			}
		}

		long end = System.nanoTime();
		long totalTime = end - start;


		// Construct formal matching, return
		Solution sol = new Solution();
		sol.setMatching(matching);
		sol.setObjectiveValue(objVal);
		sol.setSolveTime(totalTime);
		return sol;
	}

}
