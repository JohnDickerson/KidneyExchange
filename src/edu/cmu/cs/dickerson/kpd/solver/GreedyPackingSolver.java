package edu.cmu.cs.dickerson.kpd.solver;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class GreedyPackingSolver extends Solver {

	private CycleMembership membership;
	private List<Cycle> cycles;

	public GreedyPackingSolver(Pool pool, List<Cycle> cycles, CycleMembership membership) {
		super(pool);
		this.cycles = cycles;
		this.membership = membership;
	}

	public Solution solve(int numReps, double upperBound) throws SolverException {
		if(numReps < 1) { throw new SolverException("Must perform at least one greedy packing (requested numReps=" + numReps + ")"); }

		Solution bestSol = null;
		long start = System.nanoTime();
		for(int solIdx=0; solIdx<numReps; solIdx++) {

			Solution newSol = greedyPack(upperBound);

			// Improved incumbent?
			if(null==bestSol || newSol.getObjectiveValue() > bestSol.getObjectiveValue()) {
				bestSol = newSol;
			}

			// Found best possible solution?
			if(bestSol.getObjectiveValue() >= upperBound) {
				break;
			}
		}
		long end = System.nanoTime();
		long totalTime = end - start;

		bestSol.setSolveTime(totalTime);
		return bestSol;
	}

	public Solution solve(int numReps) throws SolverException {
		return solve(numReps, Double.MAX_VALUE);
	}

	//private Solution greedyPack() { return greedyPack(Double.MAX_VALUE); }
	private Solution greedyPack(double upperBound) {

		Set<Cycle> matching = new HashSet<Cycle>();
		double objVal = 0.0;

		pool.getPairs();

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

	@Override
	public String getID() {
		return "Greedy";
	}

}
