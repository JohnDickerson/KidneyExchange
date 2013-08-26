package edu.cmu.cs.dickerson.kpd.solver;

import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.KPDPool;

public abstract class Solver {

	protected final KPDPool pool;
	public Solver(KPDPool pool) {
		this.pool = pool;
	}
	
	public abstract Solution solve() throws SolverException;

	public abstract String getID();
}
