package edu.cmu.cs.dickerson.kpd.fairness;

import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.solver.CPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.Solver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;

public class Driver {

	public static void main(String args[]) {
	
		// Build a Saidman pool
		Random r = new Random(12345);
		Pool pool = new SaidmanPoolGenerator(r).generate(100, 10);
		
		// Generate all 3-cycles and somecap-chains
		CycleGenerator cg = new CycleGenerator(pool);
		List<Cycle> cycles = cg.generateCyclesAndChains(3, 5);
		
		
		// Solve the model
		Solution sol = null;
		try {
			
			Solver s = new CPLEXSolver(pool);
			sol = s.solve(cycles);
		} catch(SolverException e) {
			e.printStackTrace();
		}
		
		System.out.println(sol);
		return;
	}
}
