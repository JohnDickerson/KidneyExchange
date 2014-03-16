package edu.cmu.cs.dickerson.kpd.solver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
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
