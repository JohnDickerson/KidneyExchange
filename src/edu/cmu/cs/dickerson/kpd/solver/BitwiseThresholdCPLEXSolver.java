package edu.cmu.cs.dickerson.kpd.solver;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Pool;

public class BitwiseThresholdCPLEXSolver  extends CPLEXSolver {

	private final int n;
	private final int k;
	private final int threshold;
	
	public BitwiseThresholdCPLEXSolver(Pool pool, int k, int threshold) {
		super(pool);
		this.n = pool.vertexSet().size();
		this.k = k;
		this.threshold = threshold;
	}
	

	public Solution solve() throws SolverException {

		IOUtil.dPrintln(getClass().getSimpleName(), "Solving cycle formulation IP.");

		try {
			super.initializeCPLEX();

			// Decision variables:
			// d_ip for each V and k
			// p_ip for each V and k
			// c_ijp for each VxV and k
			// xi_ij for each VxV
			IloNumVar[] x = cplex.boolVarArray(k*n + k*n + k*n*n + n*n);

			// Only the xi decision variables matter in the objective; for any 
			// other column, set weight to zero.  For xi columns that are not
			// between the same vertices, set weight to 1.0 (for an incorrect 
			// edge existing, or for an edge that should exist not being there)
			double[] weights = new double[x.length];
			for(int idx=0; idx<weights.length; idx++) { weights[idx] = 0.0; }
			for(int v_i=0; v_i<n; v_i++) {
				for(int v_j=0; v_j<n; v_j++) {
					if(v_i != v_j) {
						weights[getEdgeConflictIdx(v_i, v_j)] = 1.0;
					}
				}
			}

			// Objective:
			// Maximize \sum_{possible edges (i,j)} xi_{ij}
			cplex.addMinimize(cplex.scalProd(weights, x));


			// Subject to:
			// 
			
			
			Solution sol = super.solveCPLEX();
			sol.setLegalMatching(sol.getObjectiveValue()==0.0);
			
			cplex.clearModel();
			return sol;
			
		} catch(IloException e) {
			System.err.println("Exception thrown during CPLEX solve: " + e);
			throw new SolverException(e.toString());
		}
	}
	
	// Returns column index of d_i^rho
	private int getDonorIdx(int v_i, int rho) {
		return (k*v_i) + rho;
	}
	
	// Returns column index of p_i^rho
	private int getPatientIdx(int v_i, int rho) {
		return (k*n) + (k*v_i) + rho;
	}
	 
	// Returns column index of c_{ij}^rho
	private int getBitConflictIdx(int v_i, int v_j, int rho) {
		return (k*n) + (k*n) + (k*n*v_i) + (k*v_j) + rho;
	}
	
	// Returns column index of \xi_{ij}
	private int getEdgeConflictIdx(int v_i, int v_j) {
		return (k*n) + (k*n) + (k*n*n) + n*v_i + v_j;
	}
}
