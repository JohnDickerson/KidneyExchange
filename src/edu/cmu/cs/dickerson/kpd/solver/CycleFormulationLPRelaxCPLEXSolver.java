package edu.cmu.cs.dickerson.kpd.solver;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.helper.Pair;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class CycleFormulationLPRelaxCPLEXSolver extends CPLEXSolver {
	
	private CycleMembership membership;
	protected List<Cycle> cycles;
	
	public CycleFormulationLPRelaxCPLEXSolver(Pool pool, List<Cycle> cycles, CycleMembership membership) {
		super(pool);
		this.cycles = cycles;
		this.membership = membership;
	}
	
	/**
	 * Solves the root LP relaxation of the cycle formulation kidney exchange IP
	 * @return Basic solution and a Map cycle index -> respective value in LP solve
	 * @throws SolverException CPLEX goes bad.
	 */
	public Pair<Solution, Map<Integer, Double>> solve() throws SolverException {

		IOUtil.dPrintln(getClass().getSimpleName(), "Solving cycle formulation LP.");

		try {
			super.initializeCPLEX();

			// Numeric decvars per cycle range from [0.0, 1.0]
			IloNumVar[] x = cplex.numVarArray(cycles.size(), 0.0, 1.0);

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


			// Solve the model, get base statistics (solve time, objective value, etc)
			Solution sol = super.solveCPLEX();
			IOUtil.dPrintln(getClass().getSimpleName(), "Solved LP!  Objective value: " + sol.getObjectiveValue());

			double[] vals = cplex.getValues(x);
			Map<Integer, Double> cycleMap = new HashMap<Integer, Double>();
			for(int cIdx=0; cIdx<vals.length; cIdx++) {
				cycleMap.put(cIdx, vals[cIdx]);
			}
			
			// 
			cplex.clearModel();
			//cplex.end();		

			return new Pair<Solution, Map<Integer, Double>>(sol, cycleMap);

		} catch(IloException e) {
			System.err.println("Exception thrown during CPLEX solve: " + e);
			throw new SolverException(e.toString());
		}
	}

	@Override
	public String getID() {
		return "Cycle LP Relax CPLEX Solver";
	}

}
