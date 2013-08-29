package edu.cmu.cs.dickerson.kpd.fairness;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SparseUNOSSaidmanPoolGenerator;

public class Driver {

	public static void main(String args[]) {
	
		// Build a Saidman pool
		Random r = new Random(12345);
		Pool pool = new SparseUNOSSaidmanPoolGenerator(r).generate(1000, 0);
		IOUtil.dPrintln(pool.edgeSet().size());
		
		// Generate all 3-cycles and somecap-chains
		CycleGenerator cg = new CycleGenerator(pool);
		List<Cycle> cycles = cg.generateCyclesAndChains(3, 3);
		
		// For each vertex, get list of cycles that contain this vertex
		CycleMembership membership = new CycleMembership(pool, cycles);
		
		// Split pairs into highly- and not highly-sensitized patients 
		double highlySensitizedThresh = 0.90;
		Set<Vertex> highV = new HashSet<Vertex>();
		for(VertexPair pair : pool.getPairs()) {
			if(pair.getPatientCPRA() >= highlySensitizedThresh) {
				highV.add(pair);
			}
		}
		
		// Solve the model
		Solution sol = null;
		try {
			
			FairnessCPLEXSolver s = new FairnessCPLEXSolver(pool, cycles, membership, highV);
			
			Solution alphaStarSol = s.solveForAlphaStar();
			sol = s.solve(alphaStarSol.getObjectiveValue());
			Solution fairSol = s.solve(0.0);
			IOUtil.dPrintln("Solved main IP with objective = " + sol.getObjectiveValue());
			IOUtil.dPrintln("Could've been = " + fairSol.getObjectiveValue());
			
			
		} catch(SolverException e) {
			e.printStackTrace();
		}
		
		return;
	}
}
