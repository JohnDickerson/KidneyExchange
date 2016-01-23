package edu.cmu.cs.dickerson.kpd.solver;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;

import java.util.Date;

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
		return solve(false);  // solve the minimization IP, not the basic feasibility one
	}

	public Solution solve(final boolean isFeasibilityIP) throws SolverException {

		try {
			super.initializeCPLEX();

			// Decision variables:
			// d_ip for each V and k
			// p_ip for each V and k
			// c_ijp for each VxV and k
			// xi_ij for each VxV   <-- only for minimization IP
			IloNumVar[] x;
			if(isFeasibilityIP) {
				x = cplex.boolVarArray(k*n + k*n + k*n*n);
				assert getBitConflictIdx(n,0,0) == x.length;
			} else {
				x = cplex.boolVarArray(k*n + k*n + k*n*n + n*n);
				assert getEdgeConflictIdx(n,0) == x.length;
			}


			IOUtil.dPrintln(getClass().getSimpleName(), "Writing down " + (isFeasibilityIP ? "feasibility" : "minimization") + " bitwise IP with #decVars=" + x.length + ", MIPgap=" + super.getRelativeMipGap() + " ...");


			// Determine which edges exist and which don't (ignoring stuff like self and dummy edges)
			boolean[][] edgeExists = pool.getDenseAdjacencyMatrix();

			// Only the xi decision variables matter in the objective; for any 
			// other column, set weight to zero.  For xi columns that are not
			// between the same vertices, set weight to 1.0 (for an incorrect 
			// edge existing, or for an edge that should exist not being there)
			if(!isFeasibilityIP) {
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
			} else {
				// Unnecessary objective is to minimize the number of violated existing edges
				double[] weights = new double[x.length];
				for(int idx=0; idx<weights.length; idx++) { weights[idx] = 0.0; }
				for(int v_i=0; v_i<n; v_i++) {
					for(int v_j=0; v_j<n; v_j++) {
						if(v_i != v_j) {
							if(edgeExists[v_i][v_j]) {						
								for(int rho=0; rho<k; rho++) {
									weights[getBitConflictIdx(v_i, v_j, rho)] = -1.0;
								}
							}
						}
					}
				}

				// Objective:
				// Maximize \sum_{edges in E (i,j) \sum_rho c_ij^rho
				cplex.addMinimize(cplex.scalProd(weights, x));
			}


			// Subject to:
			// d_i^rho \geq c_{ij}^rho
			// p_j^rho \geq c_{ij}^rho
			for(int v_i=0; v_i<n; v_i++) {
				for(int v_j=0; v_j<n; v_j++) {
					if(v_i != v_j) {
						for(int rho=0; rho<k; rho++) {

							// d_i^rho - c_{ij}^rho \geq 0
							IloLinearNumExpr donorSum = cplex.linearNumExpr(); 
							donorSum.addTerm(1.0, x[getDonorIdx(v_i,rho)]);
							donorSum.addTerm(-1.0, x[getBitConflictIdx(v_i, v_j, rho)]);
							cplex.addGe(donorSum, 0.0);

							// p_j^rho - c_{ij}^rho \geq 0
							IloLinearNumExpr patientSum = cplex.linearNumExpr(); 
							patientSum.addTerm(1.0, x[getPatientIdx(v_j,rho)]);
							patientSum.addTerm(-1.0, x[getBitConflictIdx(v_i, v_j, rho)]);
							cplex.addGe(patientSum, 0.0);

						}
					}
				}
			}

			// d_i^rho + p_j^rho \leq 1 + c_{ij}^rho
			// ---->    d_i^rho + p_j^rho - c_{ij}^rho \leq 1
			for(int v_i=0; v_i<n; v_i++) {
				for(int v_j=0; v_j<n; v_j++) {
					if(v_i != v_j) {
						for(int rho=0; rho<k; rho++) {
							IloLinearNumExpr sum = cplex.linearNumExpr(); 
							sum.addTerm(1.0,  x[getDonorIdx(v_i, rho)]);
							sum.addTerm(1.0, x[getPatientIdx(v_j, rho)]);
							sum.addTerm(-1.0, x[getBitConflictIdx(v_i, v_j, rho)]);
							cplex.addLe(sum, 1.0);
						}
					}
				}
			}

			// Write the thresholding constraints
			for(int v_i=0; v_i<n; v_i++) {
				for(int v_j=0; v_j<n; v_j++) {
					if(v_i != v_j) {
						// Write constraints based on whether or not an edge is in the edge set
						if(edgeExists[v_i][v_j]) {
							// \forall (v_i,v_j) in E,    \sum_rho c_{ij}^rho \leq t + (k-t)xi_{ij} 
							IloLinearNumExpr sumUpper = cplex.linearNumExpr(); 
							for(int rho=0; rho<k; rho++) {
								sumUpper.addTerm(1.0, x[getBitConflictIdx(v_i, v_j, rho)]);
							}
							if(!isFeasibilityIP) {
								sumUpper.addTerm(-(k-threshold), x[getEdgeConflictIdx(v_i, v_j)]);
							}
							cplex.addLe(sumUpper, threshold);

							// \forall (v_i,v_j) in E,    \sum_rho c_{ij}^rho \geq (t+1)xi_{ij}
							if(!isFeasibilityIP) {
								IloLinearNumExpr sumLower = cplex.linearNumExpr(); 
								for(int rho=0; rho<k; rho++) {
									sumLower.addTerm(1.0, x[getBitConflictIdx(v_i, v_j, rho)]);
								}
								sumLower.addTerm(-(threshold+1), x[getEdgeConflictIdx(v_i, v_j)]);
								cplex.addGe(sumLower, 0.0);
							}
						} else {
							// \forall (v_i,v_j) not in E,    \sum_rho c_{ij}^rho \geq t + 1 - kxi_{ij}
							IloLinearNumExpr sumLower = cplex.linearNumExpr(); 
							for(int rho=0; rho<k; rho++) {
								sumLower.addTerm(1.0, x[getBitConflictIdx(v_i, v_j, rho)]);
							}
							if(!isFeasibilityIP) {
								sumLower.addTerm(k, x[getEdgeConflictIdx(v_i, v_j)]);
							}
							cplex.addGe(sumLower, threshold+1);

							// \forall (v_i,v_j) not in E,    \sum_rho c_{ij}^rho <= k - (k-t)xi_{ij}
							if(!isFeasibilityIP) {
								IloLinearNumExpr sumUpper = cplex.linearNumExpr(); 
								for(int rho=0; rho<k; rho++) {
									sumUpper.addTerm(1.0, x[getBitConflictIdx(v_i, v_j, rho)]);
								}
								sumUpper.addTerm(k-threshold, x[getEdgeConflictIdx(v_i, v_j)]);
								cplex.addLe(sumUpper, k);
							}
						}
					}
				}
			}

			// Add some valid inequalities


			// Solve the model
			IOUtil.dPrintln(getClass().getSimpleName(), "Calling CPLEX to solve bitwise IP at " + new Date());
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
