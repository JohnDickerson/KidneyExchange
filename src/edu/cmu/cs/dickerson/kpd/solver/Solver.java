package edu.cmu.cs.dickerson.kpd.solver;

import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Pool;

public abstract class Solver {

	protected final Pool pool;
	public Solver(Pool pool) {
		this.pool = pool;
	}
	
	public abstract Solution solve() throws SolverException;

	public abstract String getID();
}
