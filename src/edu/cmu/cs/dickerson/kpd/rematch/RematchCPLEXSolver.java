package edu.cmu.cs.dickerson.kpd.rematch;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.CPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class RematchCPLEXSolver extends CPLEXSolver {

	private CycleMembership membership;
	protected List<Cycle> cycles;

	public enum RematchConstraintType {
		PREVENT_EXACT_MATCHING,  // Compute matching M, then add constraint saying exactly M cannot occur again
		REMOVE_MATCHED_EDGES,    // Compute matching M, then remove all edges in M (implemented as remove all cycles with at least one edge in M)
	}

	public RematchCPLEXSolver(Pool pool, List<Cycle> cycles, CycleMembership membership) {
		super(pool);
		this.cycles = cycles;
		this.membership = membership;
	}

	public Map<Integer, Set<Edge>> solve(int numRematches, RematchConstraintType rematchType) throws SolverException {

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


		try {
			super.initializeCPLEX();

			// One decision variable per cycle
			IloNumVar[] x = cplex.boolVarArray(cycles.size());

			// Decision variables multiplied by weight of corresponding cycle
			double[] weights = new double[x.length];
			int cycleIdx = 0;
			for(Cycle c : cycles) {
				weights[cycleIdx++] = c.getWeight();
			}

			// Objective:
			// Maximize \sum_{all cycles c} altWeight_c * decVar_c
			cplex.addMaximize(cplex.scalProd(weights, x));



			// Subject to: 
			// \sum_{cycles c containing v} decVar_c <=1   \forall v
			for(Vertex v : pool.vertexSet()) {

				Set<Integer> cycleColIDs = membership.getMembershipSet(v);
				if(null == cycleColIDs || cycleColIDs.isEmpty()) {
					continue;
				}

				IloLinearNumExpr sum = cplex.linearNumExpr(); 
				for(Integer cycleColID : cycleColIDs) {
					sum.addTerm(1.0, x[cycleColID]);
				}
				cplex.addLe(sum, 1.0);
			}

			// Incrementally solve, adding each solution as constraint to matrix before resolving
			Set<Integer> lastMatchCycleIdxSet = new HashSet<Integer>();
			for(int rematchIdx=1; rematchIdx <= numRematches; rematchIdx++) {

				// Add in preferred rematching constraints
				switch(rematchType) {
				case PREVENT_EXACT_MATCHING:
					// Add in the last match as a constraint ( \sum_{cycles in last match} < |size of last match| )
					if(!lastMatchCycleIdxSet.isEmpty()) {
						IloLinearNumExpr sum = cplex.linearNumExpr(); 
						for(Integer cycleColID : lastMatchCycleIdxSet) {
							sum.addTerm(1.0, x[cycleColID]);
						}
						cplex.addLe(sum, lastMatchCycleIdxSet.size()-1);
					}
					break;
				case REMOVE_MATCHED_EDGES:
					cycleIdx = 0;
					Set<Edge> edgesInLastMatch = retMap.get(rematchIdx-1);
					// Prevent any cycles that include edges from the last matching (incremental)
					if(!edgesInLastMatch.isEmpty()) {
						for(Cycle c : cycles) {
							// If any edge in this cycle was in the last match, constrain out the cycle (x_c = 0)
							for(Edge e : c.getEdges()) {
								if(edgesInLastMatch.contains(e)) {							
									IloLinearNumExpr sum = cplex.linearNumExpr(); 
									sum.addTerm(1.0, x[cycleIdx]);
									cplex.addEq(sum, 0.0);
									break;
								}
							}
							cycleIdx++;
						}
					}
					break;
				}
				// Solve, figure out which cycles and edges were included in the final solution
				super.solveCPLEX();
				double[] vals = cplex.getValues(x);
				int nCols = cplex.getNcols();

				Set<Edge> edgesInThisMatch = new HashSet<Edge>();
				lastMatchCycleIdxSet.clear();
				for(cycleIdx=0; cycleIdx<nCols; cycleIdx++) {
					if(vals[cycleIdx] > 1e-3) {
						// This is a matched cycle
						lastMatchCycleIdxSet.add(cycleIdx);
						edgesInThisMatch.addAll( cycles.get(cycleIdx).getEdges() );
					}
				}
				retMap.put(rematchIdx, edgesInThisMatch);


			} // end of rematch loop


			// 
			cplex.clearModel();
			//cplex.end();		

			return retMap;

		} catch(IloException e) {
			System.err.println("Exception thrown during CPLEX solve: " + e);
			throw new SolverException(e.toString());
		}
	}

	@Override
	public String getID() {
		return "Cycle Formulation CPLEX Solver";
	}
}
