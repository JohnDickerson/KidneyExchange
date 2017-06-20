package edu.cmu.cs.dickerson.kpd.Ethics;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.HashSet;
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

public class EthicalCPLEXSolver extends CPLEXSolver {

	private double[] altWeights;
	private CycleMembership membership;
	protected List<Cycle> cycles;
	
	public EthicalCPLEXSolver(Pool pool, List<Cycle> cycles, CycleMembership membership) {
		// Use the normal fairness solver without any failure probabilities
		this(pool, cycles, membership, false);
	}
	
	public EthicalCPLEXSolver(Pool pool, List<Cycle> cycles, CycleMembership membership, boolean usingFailureProbabilities) {
		super(pool);
		this.cycles = cycles;
		this.membership = membership;
		
		if(usingFailureProbabilities) {
			throw new UnsupportedOperationException("Only implemented for deterministic matching so far.");
		}
		createCardinalityWeightVector();
	}

	/**
	 * Computes a vector of cycle cardinalities and stores in altWeights.
	 */
	private void createCardinalityWeightVector() {
		
		// Each cycle will have a new, adjusted weight
		altWeights = new double[cycles.size()];
		int cycleIdx = 0;

		// For each cycle, create a new weight that takes special "ethical" weights into account
		for(Cycle c : cycles) {
			double altWeight = 0.0;
			for(Edge e : c.getEdges()) {
				//Special weight is stored in EthicalVertexPair
				if( !pool.getEdgeTarget(e).isAltruist() ) {
					altWeight += 1.0;
				}
			}
			altWeights[cycleIdx++] = altWeight;
		}
	}
	
	
	
	//Solve with cardinality constraint
	public Solution solve(double min_cardinality) throws SolverException {
		
		IOUtil.dPrintln(getClass().getSimpleName(), "Solving 'ethical' IP");
		
		try {
			super.initializeCPLEX();
			
			//-1 is stochastic parallel (faster), 1 is deterministic parallel (slower)
			cplex.setParam(IloCplex.IntParam.ParallelMode, 1);
			
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
			
			//Subect to: 
			//total cardinality >= min cardinality
			if (min_cardinality > 0) {
				IOUtil.dPrintln(getClass().getSimpleName(), "Enforcing ethics; solution must be " + (min_cardinality-0.0001) );
			}
			cplex.addGe(cplex.scalProd(altWeights, x), min_cardinality - 0.0001);
			
			
			// Solve the model, get base statistics (solve time, objective value, etc)
			Solution sol = super.solveCPLEX();
		
			// Figure out which cycles were included in the final solution
			double[] vals = cplex.getValues(x);
			int nCols = cplex.getNcols();
			for(cycleIdx=0; cycleIdx<nCols; cycleIdx++) {
				if(vals[cycleIdx] > 1e-3) {
					sol.addMatchedCycle(cycles.get(cycleIdx));
				}
			}
			
			IOUtil.dPrintln(getClass().getSimpleName(), "Solved IP!  Objective value: " + sol.getObjectiveValue());
			IOUtil.dPrintln(getClass().getSimpleName(), "Number of cycles in matching: " + sol.getMatching().size());
			
			// move to a JUnit test
			// Sanity check to make sure the matching is vertex disjoint
			Set<Vertex> seenVerts = new HashSet<Vertex>();
			for(Cycle c : sol.getMatching()) {
				for(Edge e : c.getEdges()) {
					Vertex v = pool.getEdgeSource(e);
					if(seenVerts.contains(v)) {
						IOUtil.dPrintln(getClass().getSimpleName(), "A vertex (" + v + ") was in more than one matched cycle; aborting.");
					}
					seenVerts.add(v);
				}
			}
			
			cplex.clearModel();
			//cplex.end();		
			
			return sol;
			
		} catch(IloException e) {
			System.err.println("Exception thrown during CPLEX solve: " + e);
			throw new SolverException(e.toString());
		}
	}

	
	@Override
	public String getID() {
		return "Ethical CPLEX Solver";
	}
	
}
