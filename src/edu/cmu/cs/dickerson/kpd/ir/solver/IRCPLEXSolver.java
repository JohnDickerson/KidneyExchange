package edu.cmu.cs.dickerson.kpd.ir.solver;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;
import edu.cmu.cs.dickerson.kpd.ir.structure.HospitalInfo;
import edu.cmu.cs.dickerson.kpd.solver.CPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class IRCPLEXSolver extends CPLEXSolver {


	private CycleMembership membership;
	private List<Cycle> cycles;
	private Set<Hospital> hospitals;

	public IRCPLEXSolver(Pool pool, List<Cycle> cycles, CycleMembership membership, Set<Hospital> hospitals) {
		super(pool);
		this.cycles = cycles;
		this.membership = membership;
		this.hospitals = hospitals;
	}

	public Solution solve(Map<Hospital, HospitalInfo> hospInfoMap, int minTotalVerticesMatched, Hospital distinguishedHospital, boolean doMax)   throws SolverException {

		IOUtil.dPrintln(getClass().getSimpleName(), "Solving IR Problem for hospital + " + distinguishedHospital + ", max=" + doMax);

		if(null != distinguishedHospital && !hospInfoMap.containsKey(distinguishedHospital)) {
			throw new IllegalArgumentException("The set of hospital constraints does not contain distinguished hospital " + distinguishedHospital);
		}

		// Unbounded problem if no cycles, return
		if(cycles.size() == 0) {
			return new Solution(0,0,new HashSet<Cycle>());
		}

		
		try {
			super.initializeCPLEX();

			// One decision variable per cycle
			IloNumVar[] x = cplex.boolVarArray(cycles.size());

			// Decision variables multiplied by weight of corresponding cycle
			double[] weights = new double[x.length];
			int cycleIdx = 0;
			for(Cycle c : cycles) { 
				if(null == distinguishedHospital) {
					// If there is no distinguished hospital, we are doing a max #vertices matched; weights
					// for cycles are just the number of vertices (=# edges, for non-chains) in that cycle
					weights[cycleIdx++] = c.getWeight();   // ASSUMES UNIT WEIGHTS
				} else {
					// If there is a distinguished hospital, we are either max or min-ing the #vertices matched
					// FOR ONLY that hospital; cycle weights are #vertices belong to hospital
					weights[cycleIdx++] = Vertex.countPatientDonorPairs( Cycle.getConstituentVerticesInSubPool(
							c, pool, hospInfoMap.get(distinguishedHospital).reportedInternalPool) );
				}
			}

			// Objective:
			// Maximize or Minimize \sum_{all cycles c} |vertices belonging to hospital| * decVar_c
			if(doMax) {
				cplex.addMaximize(cplex.scalProd(weights, x));
			} else {
				cplex.addMinimize(cplex.scalProd(weights, x));
			}

			// Subject to: 
			// Global lower bound on #vertices matched
			if(minTotalVerticesMatched > 0) {
				cycleIdx = 0;
				IloLinearNumExpr sum = cplex.linearNumExpr(); 
				for(Cycle c : cycles) {
					sum.addTerm(c.getWeight(), x[cycleIdx++]);  // assumes unit weights (specifically 0-weight dummy edge to altruist)
				}
				cplex.addGe(sum, minTotalVerticesMatched);
			}


			// Vertices matched at most once
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


			// Individual Rationality (either equal or greater-than-equal) constraints
			for(Hospital hospital : hospitals) {
				HospitalInfo hInfo = hospInfoMap.get(hospital);

				if(hInfo.minRequiredNumPairs > 0 || hInfo.exactRequiredNumPairs > 0) {
					cycleIdx = 0;
					IloLinearNumExpr sum = cplex.linearNumExpr(); 
					for(Cycle c : cycles) {
						sum.addTerm(
								Vertex.countPatientDonorPairs( Cycle.getConstituentVerticesInSubPool(c, pool, hInfo.reportedInternalPool) )  // only patient-donor pairs count
								, x[cycleIdx++]);  // weight of cycle is #vertices belong to hospital
					}
					if(hInfo.minRequiredNumPairs > 0) {
						cplex.addGe(sum, hInfo.minRequiredNumPairs);
					}
					if(hInfo.exactRequiredNumPairs > 0) {
						cplex.addGe(sum, hInfo.exactRequiredNumPairs); // temp, change back to .addEq after testing
					}
				}
			}



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

			cplex.clearModel();
			return sol;

		} catch(IloException e) {
			System.err.println("Exception thrown during IR CPLEX solve: " + e);
			throw new SolverException(e.toString());
		}
		
	}  // end of solve

}  // end of class
