package edu.cmu.cs.dickerson.kpd.solver;

import edu.cmu.cs.dickerson.kpd.solver.approx.Packer;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Pool;

public class GreedyPackingSolver extends Solver {

	public GreedyPackingSolver(Pool pool) {
		super(pool);
	}

	public Solution solve(int numReps, Packer packer, double upperBound) throws SolverException {
		if(numReps < 1) { throw new SolverException("Must perform at least one greedy packing (requested numReps=" + numReps + ")"); }

		Solution bestSol = null;
		long start = System.nanoTime();
		for(int solIdx=0; solIdx<numReps; solIdx++) {

			Solution newSol = packer.pack(upperBound);

			// Got any solution at all?
			if(null==newSol) { continue; }
			
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

		if(null!=bestSol) {
			bestSol.setSolveTime(totalTime);
			return bestSol;
		} else {
			return new Solution();
		}
		
	}

	public Solution solve(int numReps, Packer packer) throws SolverException {
		return solve(numReps, packer, Double.MAX_VALUE);
	}

	@Override
	public String getID() {
		return "Greedy";
	}

}
