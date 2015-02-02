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
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;

public class RematchCPLEXSolver extends CPLEXSolver {

	private CycleMembership membership;
	protected List<Cycle> cycles;

	public enum RematchConstraintType {
		PREVENT_EXACT_MATCHING,  // Compute matching M, then add constraint saying exactly M cannot occur again
		REMOVE_MATCHED_EDGES,    // Compute matching M, then remove all edges in M (implemented as remove all cycles with at least one edge in M)
		REMOVE_MATCHED_CYCLES,   // Compute matching M, then remove all cycles in M
		ADAPTIVE_FULL,           // Compute matching M, test edges in M, remove 0s and keep 1s (test full cycles/chains)
		ADAPTIVE_DETERMINISTIC,  // Compute matchings without taking failure probability into account, test edges and remove those that failed
	}

	public RematchCPLEXSolver(Pool pool, List<Cycle> cycles, CycleMembership membership) {
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

		int totalEdgesSoFar = 0;
		try {
			super.initializeCPLEX();

			// One decision variable per cycle
			IloNumVar[] x = cplex.boolVarArray(cycles.size());

			// Decision variables multiplied by weight of corresponding cycle
			double[] weights = new double[x.length];
			int cycleIdx = 0;
			for(Cycle c : cycles) {
				if(rematchType.equals(RematchConstraintType.ADAPTIVE_DETERMINISTIC)) {
					// Force deterministic matching for some types of adaptive edge testing
					// Weight is just size of cycle or size of chain minus 1 (for the altruist)
					weights[cycleIdx++] = c.getEdges().size() - (Cycle.isAChain(c, pool) ? 1 : 0);   
				} else {
					weights[cycleIdx++] = c.getWeight();
				}
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


			// Update for last matching (if it exists) 
			// Incrementally solve, adding each solution as constraint to matrix before resolving
			Set<Integer> lastMatchCycleIdxSet = new HashSet<Integer>();
			for(int rematchIdx=1; rematchIdx <= numRematches; rematchIdx++) {

				// If the previous match was empty (and we're not on the first match), cap out
				// the rematch map with empty edge sets and quit (since no matching will ever be nonempty again)
				if(rematchIdx>1 && lastMatchCycleIdxSet.size() == 0) {
					retMap.put(rematchIdx, new HashSet<Edge>());
					continue;
				}
				Set<Edge> edgesInLastMatch = retMap.get(rematchIdx-1);
				
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
					// Prevent any cycles that include edges from the last matching (incremental)
					if(!edgesInLastMatch.isEmpty()) {
						cycleIdx = 0;
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
				case ADAPTIVE_FULL:
					// For each cycle/chain in this matching, test all edges
					// Set existing edges to failure_prob=0, non-existing to failure_prob=1
					if(!lastMatchCycleIdxSet.isEmpty()) {

						// Test all edges in the last matching, update failure probabilities to determinisim
						for(Edge e : edgesInLastMatch) {
							e.setFailureProbability( edgeFailedMap.get(e) ? 1.0 : 0.0 );
						}

						// Update utilities for each of the possibly partially-tested cycles and chains
						cycleIdx = 0;
						for(Cycle c : cycles) {
							if(Cycle.isAChain(c, pool)) {
								weights[cycleIdx++] = FailureProbabilityUtil.calculateDiscountedChainUtility(c, pool, pool.vertexSet());
							} else {
								weights[cycleIdx++] = FailureProbabilityUtil.calculateDiscountedCycleUtility(c, pool, pool.vertexSet());
							}
						}
						
						// Update CPLEX's objective function to point to these new weights
						cplex.getObjective().setExpr(cplex.scalProd(weights, x));
					}
					// DO NOT BREAK; want to fall through into REMOVE_MATCHED_CYCLES to constrain out cycles in this matching
				case REMOVE_MATCHED_CYCLES:
					// Disallow any previously used cycle from being reused 
					for(Integer cycleColID : lastMatchCycleIdxSet) {
						IloLinearNumExpr sum = cplex.linearNumExpr(); 
						sum.addTerm(1.0, x[cycleColID]);
						cplex.addEq(sum, 0.0);
					}
					break;
				
				case ADAPTIVE_DETERMINISTIC:
					// For any edge that was in the last matching, tested and failed, remove cycle or chain containing it
					if(!edgesInLastMatch.isEmpty()) {
						cycleIdx = 0;
						for(Cycle c : cycles) {
							// At least one failure in a cycle = that cycle disappears = same as if the failed edge didn't exist during cycle gen
							// At least one failure in a chain = that chain could execute until the failure, but we have decvars for the shorter
							//          chain so constrain out any chain that has even one failure in it
							for(Edge e : c.getEdges()) {
								if(edgesInLastMatch.contains(e) && edgeFailedMap.get(e)) {							
									IloLinearNumExpr sum = cplex.linearNumExpr(); 
									sum.addTerm(1.0, x[cycleIdx]);
									cplex.addEq(sum, 0.0);
									break;
								}
							}
							cycleIdx++;
						}
					}
					// Also constrain out any cycles we had in the last match
					/*for(Integer cycleColID : lastMatchCycleIdxSet) {
						IloLinearNumExpr sum = cplex.linearNumExpr(); 
						sum.addTerm(1.0, x[cycleColID]);
						cplex.addEq(sum, 0.0);
					}*/
					break;
				default:
					throw new SolverException("Have not implemented RematchType " + rematchType + " yet");
				}

				
				// Solve, figure out which cycles and edges were included in the final solution
				super.solveCPLEX();
				double[] vals = cplex.getValues(x);
				int nCols = cplex.getNcols();

				boolean shortCircuitDueToTooManyEdges = false;

				Set<Edge> edgesInThisMatch = new HashSet<Edge>();
				lastMatchCycleIdxSet.clear();
				for(cycleIdx=0; cycleIdx<nCols; cycleIdx++) {
					if(vals[cycleIdx] > 1e-3) {
						// This is a matched cycle; if we have extra edge tests, add it to the list, otherwise break
						totalEdgesSoFar++;
						if( totalEdgesSoFar / (double) pool.vertexSet().size() > maxAvgEdgesPerVertex) {
							shortCircuitDueToTooManyEdges = true;
							break;
						}

						lastMatchCycleIdxSet.add(cycleIdx);
						edgesInThisMatch.addAll( cycles.get(cycleIdx).getEdges() );
					}
				}
				retMap.put(rematchIdx, edgesInThisMatch);

				if(shortCircuitDueToTooManyEdges) { break; }
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
