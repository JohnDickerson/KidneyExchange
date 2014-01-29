package edu.cmu.cs.dickerson.kpd.solver;

import ilog.concert.IloException;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Pool;

public class EdgeFormulationCPLEXSolver extends CPLEXSolver {

	public EdgeFormulationCPLEXSolver(Pool pool) {
		super(pool);
	}

	public Solution solve() throws SolverException {

		IOUtil.dPrintln(getClass().getSimpleName(), "Solving cycle formulation IP.");

		try {
			super.initializeCPLEX();

			// TODO implement constraint generation
			
			cplex.clearModel();

			return null;

		} catch(IloException e) {
			System.err.println("Exception thrown during CPLEX solve: " + e);
			throw new SolverException(e.toString());
		}
	}
	
	@Override
	public String getID() {
		return "Edge Formulation CPLEX Solver";
	}
}
