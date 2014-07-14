package edu.cmu.cs.dickerson.kpd.solver;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Pool;

public abstract class CPLEXSolver extends Solver {

	protected static IloCplex cplex = null;

	public CPLEXSolver(Pool pool) {
		super(pool);
	}


	protected void initializeCPLEX() throws IloException {

		// Either initialize CPLEX or clear out any old data
		if(null == cplex) {
			cplex = new IloCplex();
		} else {
			cplex.clearModel();
		}

		if(getMaxCPUThreads() > 0) {
			cplex.setParam(IloCplex.IntParam.Threads, super.getMaxCPUThreads());
		}
		if(getMaxSolveSeconds() > 0) {
			cplex.setParam(IloCplex.DoubleParam.TiLim, super.getMaxSolveSeconds());
		}
	}

	protected Solution solveCPLEX() throws IloException, SolverException {

		//
		// Solve the model
		long solveStartTime = System.nanoTime();
		boolean solvedOK = cplex.solve();
		long solveEndTime = System.nanoTime();
		long solveTime = solveEndTime - solveStartTime;

		if(solvedOK) {

			IOUtil.dPrintln(getID(), "Found an answer! Obj: " + cplex.getObjValue() + ", CPLEX status: " + cplex.getStatus() + ", Time: " + ((double) solveTime / 1000000000.0));
			Solution sol = new Solution();

			// The objective value is the Dodgson score for a*
			double objectiveValue = cplex.getObjValue();
			sol.setObjectiveValue(objectiveValue);

			// Elapsed solve time (just solve time, not IP write time)
			sol.setSolveTime(solveTime);

			return sol;

		} else {
			throw new SolverException("cplex.solve() returned false.\n"+cplex.getCplexStatus());
		}
	}

	@Override
	public String getID() {
		return "CPLEX Solver";
	}

}
