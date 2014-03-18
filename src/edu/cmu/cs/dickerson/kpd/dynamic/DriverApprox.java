package edu.cmu.cs.dickerson.kpd.dynamic;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.fairness.io.ExperimentalOutput;
import edu.cmu.cs.dickerson.kpd.fairness.io.ExperimentalOutput.Col;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.GreedyPackingSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSGenerator;

public class DriverApprox {

	public static void main(String[] args) {

		// Generate draws from all UNOS match runs currently on the machine
		Random r = new Random();
		UNOSGenerator gen = UNOSGenerator.makeAndInitialize(IOUtil.getBaseUNOSFilePath(), ',', r);

		// list of |V|s we'll iterate over
		List<Integer> graphSizeList = Arrays.asList(new Integer[] {100});
		int numGraphReps = 16; 

		// Cycle and chain limits
		int cycleCap = 3;
		int chainCap = 3;

		// Number of greedy packings per solve call
		int numGreedyReps = 10;

		// Store output
		String path = "approx_" + System.currentTimeMillis() + ".csv";
		ExperimentalOutput out = null;
		try {
			out = new ExperimentalOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		for(int graphSize : graphSizeList) {
			for(int graphRep=0; graphRep<numGraphReps; graphRep++) {

				IOUtil.dPrintln("Graph (|V|=" + graphSize + ", #" + graphRep + "/" + numGraphReps + ")");

				// Bookkeeping
				out.set(Col.CHAIN_CAP, chainCap);	
				out.set(Col.CYCLE_CAP, cycleCap);	
				out.set(Col.NUM_PAIRS, graphSize);	
				
				// Generate pool
				Pool pool = gen.generatePool(graphSize);

				// Generate all cycles in pool
				CycleGenerator cg = new CycleGenerator(pool);
				List<Cycle> cycles = cg.generateCyclesAndChains(cycleCap, chainCap, false);
				CycleMembership membership = new CycleMembership(pool, cycles);

				// Get optimal match size for pool
				Solution optSol = null;
				try {
					CycleFormulationCPLEXSolver s = new CycleFormulationCPLEXSolver(pool, cycles, membership);
					optSol = s.solve();
					IOUtil.dPrintln("Optimal Value: " + optSol.getObjectiveValue());
				} catch(SolverException e) {
					e.printStackTrace();
					System.exit(-1);
				}

				// Get a greedy packing
				Solution greedySol = null;
				try {
					GreedyPackingSolver s = new GreedyPackingSolver(pool, cycles, membership);
					greedySol = s.solve(numGreedyReps);
					IOUtil.dPrintln("Greedy Value: " + greedySol.getObjectiveValue());
				} catch(SolverException e) {
					e.printStackTrace();
					System.exit(-1);
				}

				// Compare the greedy and optimal solutions
				if(null != optSol) {
					out.set(Col.UNFAIR_OBJECTIVE, optSol.getObjectiveValue());	
				}
				
				if(null != greedySol) {
					out.set(Col.FAIR_OBJECTIVE, greedySol.getObjectiveValue());		
				}
				
				// Write the  row of data
				try {
					out.record();
				} catch(IOException e) {
					IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
					e.printStackTrace();
					System.exit(-1);
				}
				
			} // graphRep : numGraphReps
		} // graphSize : graphSizeList

		// clean up CSV writer
		if(null != out) {
			try {
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		IOUtil.dPrintln("======= DONE =======");
	}

}
