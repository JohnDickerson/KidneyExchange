package edu.cmu.cs.dickerson.kpd.rematch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class RematchSequentialCPLEXSolver extends RematchCPLEXSolver {

	private CycleMembership membership;
	protected List<Cycle> cycles;

	public RematchSequentialCPLEXSolver(Pool pool, List<Cycle> cycles, CycleMembership membership) {
		super(pool);
		this.cycles = cycles;
		this.membership = membership;
	}
	public Map<Integer, Set<Edge>> solve(int numRematches, RematchConstraintType rematchType, Map<Edge, Boolean> edgeFailedMap) throws SolverException {
		return solve(numRematches, rematchType, edgeFailedMap, Double.MAX_VALUE);
	}

	public Map<Integer, Set<Edge>> solve(int numRematches, RematchConstraintType rematchType, Map<Edge, Boolean> edgeFailedMap, double maxAvgEdgesPerVertex) throws SolverException {

		IOUtil.dPrintln(getClass().getSimpleName(), "Solving cycle formulation, rematches=" + numRematches + ".");

		Map<Integer, Set<Edge>> retMap = new HashMap<Integer, Set<Edge>>();
		retMap.put(0, new HashSet<Edge>());  // at zero rematches, not allowed to test any edges

		// If no cycles, problem is possibly unbounded; return no edges to test for each time period
		if(cycles.size() == 0) {
			for(int rematchCt=0; rematchCt<=numRematches; rematchCt++) {
				retMap.put(0, new HashSet<Edge>());
			}
			return retMap;
		}
		// If we're not allowed to do any rematching (i.e. no testing), return empty set
		if(numRematches < 1) {
			return retMap;
		}
		
		
		return retMap;
	}

	@Override
	public String getID() {
		return "Cycle Formulation CPLEX Solver -- Rematch Sequential for Thesis";
	}
}
