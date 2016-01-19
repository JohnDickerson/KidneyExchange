package edu.cmu.cs.dickerson.kpd.solver;

import ilog.concert.IloException;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Pool;

public class BitwiseThresholdCPLEXSolver  extends CPLEXSolver {

	public BitwiseThresholdCPLEXSolver(Pool pool, int k, int threshold) {
		super(pool);
	}

	public Solution solve() throws SolverException {

		IOUtil.dPrintln(getClass().getSimpleName(), "Solving cycle formulation IP.");

		try {
			super.initializeCPLEX();

			// TODO
			
			
			
			Solution sol = super.solveCPLEX();
			sol.setLegalMatching(sol.getObjectiveValue()==0.0);
			
			cplex.clearModel();
			return sol;
			
		} catch(IloException e) {
			System.err.println("Exception thrown during CPLEX solve: " + e);
			throw new SolverException(e.toString());
		}

	}
}
