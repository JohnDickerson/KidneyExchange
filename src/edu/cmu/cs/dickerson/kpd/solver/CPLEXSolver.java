package edu.cmu.cs.dickerson.kpd.solver;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import edu.cmu.cs.dickerson.kpd.fairness.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.KPDPool;

public class CPLEXSolver extends Solver {

	public CPLEXSolver(KPDPool pool) {
		super(pool);
	}

	@Override
	public Solution solve() throws SolverException {

		Solution sol = null;
		try {
			IloCplex cplex = new IloCplex();
			//cplex.setParam(IloCplex.IntParam.Threads, 4);
			//cplex.setParam(IloCplex.DoubleParam.TiLim, 3600.00);

			
			// TODO write the fairness IP!
			
			
			
			//
			// Solve the model
			long solveStartTime = System.nanoTime();
			boolean solvedOK = cplex.solve();
			long solveEndTime = System.nanoTime();
			long solveTime = solveEndTime - solveStartTime;

			if(solvedOK) {

				IOUtil.dPrintln(getID(), "Found an answer! CPLEX status: " + cplex.getStatus() + ", Time: " + ((double) solveTime / 1000000000.0));

				// The objective value is the weighted max disjoint cycle cover under fairness prefs
				double objectiveValue = cplex.getObjValue();
				IOUtil.dPrintln(getID(), "Objective value: " + objectiveValue);

				sol = new Solution();
				
				// Elapsed solve time (just solve time, not IP write time)
				sol.setSolveTime(solveTime);

			} else {
				throw new SolverException("cplex.solve() returned false.");
			}

			cplex.end();		

		} catch(IloException e) {
			System.err.println("Exception thrown during CPLEX solve: " + e);
			throw new SolverException(e.toString());
		}
			
		return sol;
	}

	@Override
	public String getID() {
		return "CPLEX Fairness Solver";
	}

}
