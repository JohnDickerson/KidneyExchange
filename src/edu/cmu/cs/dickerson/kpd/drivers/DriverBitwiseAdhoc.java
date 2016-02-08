package edu.cmu.cs.dickerson.kpd.drivers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.io.BitwiseAdhocOutput;
import edu.cmu.cs.dickerson.kpd.io.BitwiseAdhocOutput.Col;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.generator.UNOSGenerator;

public class DriverBitwiseAdhoc {


	public static void main(String args[]) {

		// How many base vertex sets should be draw?
		int numberOfBaseGraphs = 10;

		// Iterate from 0 mismatches to a threshold maximum of ...?
		int maxThreshold = 5;

		// Random base seed; we'll hold this constant for increasing threshold on the same base vertex set
		long seed = System.currentTimeMillis();

		// How big should the base graphs be?
		List<Integer> numVertsList = Arrays.asList(new Integer[] {50, 100, 150, 200, 250});

		// Possibly use different max cycle and chain sizes
		List<Integer> cycleCapList = Arrays.asList(3);
		List<Integer> chainCapList = Arrays.asList(0);//UNOS uses four

		// Generate draws from all UNOS match runs currently on the machine
		String basePath = IOUtil.getBaseUNOSFilePath();
		UNOSGenerator gen = UNOSGenerator.makeAndInitialize(basePath, ',');
		IOUtil.dPrintln("UNOS generator operating on #donors: " + gen.getDonors().size() + " and #recipients: " + gen.getRecipients().size());

		// Initialize our experimental output to .csv writer
		String path = "unos_adhoc" + System.currentTimeMillis() + ".csv";
		BitwiseAdhocOutput eOut = null;
		try {
			eOut = new BitwiseAdhocOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		// For debug output, calculate how many runs we're actually going to do
		int totalRunNums = numberOfBaseGraphs*numVertsList.size()*cycleCapList.size()*chainCapList.size()*(maxThreshold+1);
		int currRunNum = 0;
		
		for(int numVerts : numVertsList) {
			for(int baseGraphNum=0; baseGraphNum<numberOfBaseGraphs; baseGraphNum++) {
				for(int cycleCap : cycleCapList) {
					for(int chainCap : chainCapList) {

						// Bump seed to make new underlying vertex draw
						seed += 1;
						for(int threshold=0; threshold<=maxThreshold; threshold++) {

							currRunNum++;
							IOUtil.dPrintln("RUN="+currRunNum+"/"+totalRunNums+", Graph "+(baseGraphNum+1)+"/"+numberOfBaseGraphs+", |V|="+numVerts+", cycle_cap="+cycleCap+", chain_cap="+chainCap+", threshold="+threshold);
							// We want the same base graph for each of the thresholds; reset the seed
							gen.setRandom(new Random(seed));

							// Generate a new pool with a specific threshold on the edges
							gen.setThreshold(threshold);
							Pool pool = gen.generate(numVerts, 0);

							//if(2==2) { pool.writeUNOSGraphToDZN("unos_gen_"+baseGraphNum+".dzn", 10, 0); continue; }
							
							CycleGenerator cg = new CycleGenerator(pool);
							List<Cycle> cycles = cg.generateCyclesAndChains(cycleCap, chainCap, false);

							// Get stats about that pool
							int numEdges = pool.getNumNonDummyEdges();
							int numCycles = cycles.size();

							// For each vertex, get list of cycles that contain this vertex
							CycleMembership membership = new CycleMembership(pool, cycles);

							eOut.set(Col.GENERATOR, gen.toString());
							eOut.set(Col.SEED, seed);
							eOut.set(Col.CYCLE_CAP, cycleCap);
							eOut.set(Col.CHAIN_CAP, chainCap);
							eOut.set(Col.NUM_PAIRS, pool.getNumPairs());
							eOut.set(Col.NUM_ALTS, pool.getNumAltruists());
							eOut.set(Col.NUM_VERTS, pool.vertexSet().size());
							eOut.set(Col.NUM_EDGES, numEdges);
							eOut.set(Col.THRESHOLD, threshold);
							eOut.set(Col.NUM_CYCLES, numCycles);

							// Solve the model
							try {
								CycleFormulationCPLEXSolver s = new CycleFormulationCPLEXSolver(pool, cycles, membership);

								Solution sol = s.solve();
								eOut.set(Col.MATCH_SIZE, sol.getObjectiveValue());
							} catch(SolverException e) {
								e.printStackTrace();
							}

							try {
								eOut.record();
							} catch(IOException e) {
								IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
								e.printStackTrace();
								return;
							}

						}  // threshold
					} // chainCap
				} // cycleCap
			} // baseGraphNum
		} // numVerts

		// Flush and kill the CSV writer
		if(null != eOut) {
			try {
				eOut.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		IOUtil.dPrintln("*** Done! ***");
	}

}
