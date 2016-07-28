package edu.cmu.cs.dickerson.kpd.rematch;

import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.solver.CPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;

public abstract class RematchCPLEXSolver extends CPLEXSolver {

	public RematchCPLEXSolver(Pool pool) {
		super(pool);
	}

	public enum RematchConstraintType {
		PREVENT_EXACT_MATCHING,  // Compute matching M, then add constraint saying exactly M cannot occur again
		REMOVE_MATCHED_EDGES,    // Compute matching M, then remove all edges in M (implemented as remove all cycles with at least one edge in M)
		REMOVE_MATCHED_CYCLES,   // Compute matching M, then remove all cycles in M
		ADAPTIVE_FULL,           // Compute matching M, test edges in M, remove 0s and keep 1s (test full cycles/chains)
		ADAPTIVE_DETERMINISTIC,  // Compute matchings without taking failure probability into account, test edges and remove those that failed
		FULLY_SEQUENTIAL,        // Test exactly one edge at a time (not part of EC paper)
	}

	public abstract Map<Integer, Set<Edge>> solve(int numRematches, 
			RematchConstraintType rematchType, 
			Map<Edge, Boolean> edgeFailedMap) throws SolverException;
	
	public abstract Map<Integer, Set<Edge>> solve(int numRematches, 
			RematchConstraintType rematchType, 
			Map<Edge, Boolean> edgeFailedMap, 
			double maxAvgEdgesPerVertex) throws SolverException;
}
