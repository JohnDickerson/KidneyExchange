package edu.cmu.cs.dickerson.kpd.rematch.strats;

import edu.cmu.cs.dickerson.kpd.rematch.RematchCPLEXSolver.RematchConstraintType;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;

public abstract class RematchStrat {

	// Am I initialized at least once?
	protected static boolean initialized = false;
	
	// Parameters that shouldn't change after initialization of singleton
	protected static int cycleCap = 2;
	protected static int numPairs = 0;
	protected static int numAlts = 0;
	protected static int maxNumRematchesEC2015 = 0;
	protected static double maxAvgEdgesPerVertex = 0;
	protected static RematchConstraintType rematchType = RematchConstraintType.ADAPTIVE_DETERMINISTIC;
	protected static boolean onlyPlotMaxRematch = false;
	
	public static void init(int cycleCap, int numPairs, int numAlts,
			int maxNumRematchesEC2015, double maxAvgEdgesPerVertex,
			RematchConstraintType rematchType, boolean onlyPlotMaxRematch) {
		RematchStrat.cycleCap = cycleCap;
		RematchStrat.numPairs = numPairs;
		RematchStrat.numAlts = numAlts;
		RematchStrat.maxNumRematchesEC2015 = maxNumRematchesEC2015;
		RematchStrat.maxAvgEdgesPerVertex = maxAvgEdgesPerVertex;
		RematchStrat.rematchType = rematchType;
		RematchStrat.onlyPlotMaxRematch = onlyPlotMaxRematch;
		RematchStrat.initialized = true;
	}
	
	public RematchStrat() {
		super();
		if(!initialized) {
			throw new RuntimeException("Please call RematchStrat.init(...) before creating RematchStrat objects.");
		}
	}
	
	/**
	 * Runs whatever the rematch strategy is, using the init()'d parameters
	 * @throws SolverException
	 */
	public abstract void runRematch() throws SolverException;

}
