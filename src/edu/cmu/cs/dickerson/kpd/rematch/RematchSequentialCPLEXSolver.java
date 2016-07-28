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
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;

public class RematchSequentialCPLEXSolver extends RematchCPLEXSolver {

	private CycleMembership membership;
	protected List<Cycle> cycles;

	private IloNumVar[] x;
	private double[] weights;
	
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


		// Begin by building the base model (objective w/ no edge tests, constraints, etc)
		try {
			super.initializeCPLEX();
			cplex.setOut(null);  // for this solver, we solve models waaay too much so no output
			super.doDebugPrint = false;
			
			// One decision variable per cycle
			x = cplex.boolVarArray(cycles.size());


			// Decision variables multiplied by weight of corresponding cycle
			weights = new double[x.length];
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

			// Track which edges we have not tested, and which we've tested
			// and exist or don't exist -- don't test dummy edges
			Set<Edge> edgeUnknownExistsSet = pool.getNonDummyEdgeSet();
			Set<Edge> edgeNoExistsSet = new HashSet<Edge>();
			Set<Edge> edgeExistsSet = new HashSet<Edge>();

			// Inner loop -- do edge testing until we're out of edges or
			// some other stopping condition is hit; keep track of which edges
			// we test at which round in the retMap {round# -> {which edge was tested}}
			int numEdgesTested = 0;
			while(!edgeUnknownExistsSet.isEmpty() && numEdgesTested < numRematches) {
				
				IOUtil.dPrintln("Edge selection " + numEdgesTested + "/" + numRematches + "...");
				
				// Get the next best edge to test (myopic lookahead)
				final Edge nextEdge = getBestEdge(edgeExistsSet, edgeNoExistsSet);
				
				// Test the edge for existence (predetermined)
				boolean nextEdgeExists = ! edgeFailedMap.get(nextEdge);
				if(nextEdgeExists) {
					edgeExistsSet.add(nextEdge);
				} else {
					edgeNoExistsSet.add(nextEdge);
				}
				
				// Record that we tested this one edge ("set of edges") this round
				numEdgesTested++;
				Set<Edge> edgesTestedThisRound = new HashSet<Edge>();
				edgesTestedThisRound.add(nextEdge);
				retMap.put(numEdgesTested, edgesTestedThisRound);
				
				edgeUnknownExistsSet.remove(nextEdge);
			} // end of edge testing loop


			// Return CPLEX memory and return map of round -> edge that was tested
			cplex.clearModel();
			return retMap;

		} catch(IloException e) {
			System.err.println("Exception thrown during CPLEX solve: " + e);
			throw new SolverException(e.toString());
		}
	}

	private Edge getBestEdge(Set<Edge> edgeExistsSet, Set<Edge> edgeNoExistsSet) throws SolverException, IloException {

		// Debug print variables, compute once
		Set<Edge> candidateEdges = this.pool.getNonDummyEdgeSet();
		int numEdgesToTest = candidateEdges.size() - edgeExistsSet.size() - edgeNoExistsSet.size();
		int numEdgesTested = 0;
		int numRoundsRunSoFar = edgeExistsSet.size() + edgeNoExistsSet.size();
		
		
		double bestObj = Double.NEGATIVE_INFINITY;
		Edge bestEdge = null;
		for(Edge currEdge : candidateEdges) {

			// Skip edges that we have already tested (and that either exist or don't)
			if(edgeNoExistsSet.contains(currEdge) || edgeExistsSet.contains(currEdge)) {
				continue;
			}
			numEdgesTested++;
			double edgeFailureProb = currEdge.getFailureProbability();

			// See what our expected utility would be with this edge existing
			edgeExistsSet.add(currEdge);
			double existsObj = getObjectiveWithAdjustedCoefficients(edgeExistsSet, edgeNoExistsSet);
			edgeExistsSet.remove(currEdge);
			currEdge.setFailureProbability(edgeFailureProb);
			
			// See what our expected utility would be without this edge existing
			edgeNoExistsSet.add(currEdge);
			double notExistsObj = getObjectiveWithAdjustedCoefficients(edgeExistsSet, edgeNoExistsSet);
			edgeNoExistsSet.remove(currEdge);
			currEdge.setFailureProbability(edgeFailureProb);
			
			// If the weighted expected utility of testing this edge is better than
			// the incumbent, store this edge and its objective
			double expectedObj = (1.0-edgeFailureProb)*existsObj + edgeFailureProb*notExistsObj;
			if(expectedObj > bestObj) {
				IOUtil.dPrintln("--- Round " + numRoundsRunSoFar + ", incumbent improved (" + numEdgesTested + "/" + numEdgesToTest + ") -- " + bestObj + " --> " + expectedObj);
				bestObj = expectedObj;
				bestEdge = currEdge;
			}
		}

		if(null==bestEdge) {
			throw new RuntimeException("Did not test any edges during getBestEdge() call.");
		}
		return bestEdge;
	}

	/**
	 * Assuming a partially-built model (i.e., initialized CPLEX, have an objective,
	 * have a constraint matrix, etc), adjusts the coefficients in the objective to
	 * reflect either successfully tested edges (success prob = 1.0) or unsuccessfully
	 * tested edges (success prob = 0.0) -- all the constraints are still valid otherwise
	 * @param edgeExistsSet set of edges that have been tested and exist (possibly empty)
	 * @param edgeNoExistsSet set of edges that have been tested and don't exist (possibly empty)
	 * @return the expected utility of the failure-aware matching conditioned on those edges
	 *         existing or not existing, using the failure probs for other edges as in EC-13
	 * @throws SolverException
	 * @throws IloException
	 */
	private double getObjectiveWithAdjustedCoefficients(Set<Edge> edgeExistsSet, Set<Edge> edgeNoExistsSet) throws SolverException, IloException {

		// Reweight the coefficients on cycle and chain variables based on edge tests
		for(Edge e : edgeExistsSet) {
			e.setFailureProbability(0.0);
		}
		for(Edge e : edgeNoExistsSet) {
			e.setFailureProbability(1.0);
		}

		// Update utilities for each of the possibly partially-tested cycles and chains
		int cycleIdx = 0;
		for(Cycle c : cycles) {
			if(Cycle.isAChain(c, pool)) {
				weights[cycleIdx++] = FailureProbabilityUtil.calculateDiscountedChainUtility(c, pool, pool.vertexSet());
			} else {
				weights[cycleIdx++] = FailureProbabilityUtil.calculateDiscountedCycleUtility(c, pool, pool.vertexSet());
			}
		}
		
		// Update CPLEX's objective function to point to these new weights
		cplex.getObjective().setExpr(cplex.scalProd(weights, x));
		
		
		
		// Solve the model and return the objective (all we care about, not the actual matching);
		super.solveCPLEX();
		return cplex.getObjValue();

	}

	@Override
	public String getID() {
		return "RematchSequentialCPLEXSolver";
	}
}
