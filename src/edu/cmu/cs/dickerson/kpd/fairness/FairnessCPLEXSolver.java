package edu.cmu.cs.dickerson.kpd.fairness;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;

import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.CPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class FairnessCPLEXSolver extends CPLEXSolver {

	private double[] altWeights;
	private Set<Vertex> specialV;
	private CycleMembership membership;
	
	public FairnessCPLEXSolver(Pool pool, List<Cycle> cycles, CycleMembership membership, Set<Vertex> specialV) {
		super(pool, cycles);
		this.membership = membership;
		this.specialV = specialV;
		
		// Calculate highly-sensitized weights for each cycle
		calcSpecialWeights();
	}

	/**
	 * Computes h(c), the weight of a cycle where only transplants to 
	 * a vertex in the "special vertex" (e.g., highly-sensitized) set count
	 */
	private void calcSpecialWeights() {
		
		// Each cycle will have a new, adjusted weight
		altWeights = new double[cycles.size()];
		int cycleIdx = 0;

		// For each cycle, create a new weight that only takes special vertices' successful transplants into account
		for(Cycle c : cycles) {
			double altWeight = 0.0;
			for(Edge e : c.getEdges()) {
				Vertex recipient = pool.getEdgeTarget(e);
				if(specialV.contains(recipient)) {
					altWeight += pool.getEdgeWeight(e);
				}
			}
			altWeights[cycleIdx++] = altWeight;
		}
	}
	
	
	/**
	 * Given a set of distinguished vertices and adjusted weights for all cycles based
	 * on these distinguished vertices, finds the weighted maximum % of these vertices that
	 * can be included in any matching in the pool
	 * @return a* = max_{m \in M} | { v  |  v \in m  ^  v \in special } / |special|
	 * @throws SolverException if CPLEX fails to solve to optimality or otherwise messes up
	 */
	public Solution solveForAlphaStar() throws SolverException {
		
		IOUtil.dPrintln(getClass().getSimpleName(), "Solving for a* with |special| = " + specialV.size());
		
		try {
			super.initializeCPLEX();
			
			// One decision variable per cycle (although many cycles might have special weight 0)
			IloNumVar[] x = cplex.boolVarArray(cycles.size());

			// Objective:
			// Maximize \sum_{all cycles c} altWeight_c * decVar_c
			cplex.addMaximize(cplex.scalProd(altWeights, x));
		
			// Subject to: 
			// \sum_{cycles c containing v} decVar_c <=1   \forall v
			for(Vertex v : pool.vertexSet()) {
				
				Set<Integer> cycleColIDs = membership.getMemberList(v);
				if(null == cycleColIDs || cycleColIDs.isEmpty()) {
					continue;
				}
				
				IloLinearNumExpr sum = cplex.linearNumExpr(); 
				for(Integer cycleColID : cycleColIDs) {
					sum.addTerm(1.0, x[cycleColID]);
				}
				cplex.addLe(sum, 1.0);
			}
			
			
			// Solve the model, get base statistics (solve time, objective value, etc)
			Solution sol = super.solveCPLEX();
			
			// We're interested in a* = #matched / |special|
			if(specialV.size() > 0) {
				sol.setObjectiveValue(sol.getObjectiveValue() / specialV.size());
			}
			IOUtil.dPrintln(getClass().getSimpleName(), "Found a* = " + sol.getObjectiveValue());
			
			cplex.end();		
			return sol;
			
		} catch(IloException e) {
			System.err.println("Exception thrown during CPLEX solve: " + e);
			throw new SolverException(e.toString());
		}
	}
	
	public Solution solve(double alpha) throws SolverException {
		
		IOUtil.dPrintln(getClass().getSimpleName(), "Solving main fairness IP with a = " + alpha);
		
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
				
				Set<Integer> cycleColIDs = membership.getMemberList(v);
				if(null == cycleColIDs || cycleColIDs.isEmpty()) {
					continue;
				}
				
				IloLinearNumExpr sum = cplex.linearNumExpr(); 
				for(Integer cycleColID : cycleColIDs) {
					sum.addTerm(1.0, x[cycleColID]);
				}
				cplex.addLe(sum, 1.0);
			}
			
			
			// \sum_c altWeight_c * decVar_c >= alpha*|special|
			cplex.addGe(cplex.scalProd(altWeights, x), alpha * specialV.size());
			
			

			// Solve the model, get base statistics (solve time, objective value, etc)
			Solution sol = super.solveCPLEX();
		
			// Figure out which cycles were included in the final solution
			//double[] vals = cplex.getValues();
			
			// We're interested in a* = #matched / |special|
			IOUtil.dPrintln(getClass().getSimpleName(), "Solved IP!  Objective value: " + sol.getObjectiveValue());
			
			cplex.end();		
			return sol;
			
		} catch(IloException e) {
			System.err.println("Exception thrown during CPLEX solve: " + e);
			throw new SolverException(e.toString());
		}
	}

	
	@Override
	public String getID() {
		return "Fairness CPLEX Solver";
	}

	
}
