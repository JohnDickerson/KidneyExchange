package edu.cmu.cs.dickerson.kpd.dynamic;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.fairness.io.DriverApproxOutput;
import edu.cmu.cs.dickerson.kpd.fairness.io.DriverApproxOutput.Col;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.GreedyPackingSolver;
import edu.cmu.cs.dickerson.kpd.solver.approx.CycleLPRelaxationPacker;
import edu.cmu.cs.dickerson.kpd.solver.approx.CycleShufflePacker;
import edu.cmu.cs.dickerson.kpd.solver.approx.CyclesSampleChainsIPPacker;
import edu.cmu.cs.dickerson.kpd.solver.approx.CyclesThenChainsPacker;
import edu.cmu.cs.dickerson.kpd.solver.approx.VertexRandomWalkPacker;
import edu.cmu.cs.dickerson.kpd.solver.approx.VertexShufflePacker;
import edu.cmu.cs.dickerson.kpd.solver.approx.VertexShufflePacker.ShuffleType;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.UNOSGenerator;

public class DriverApprox {

	public static void main(String[] args) {

		// Set up our random generators for pools (sample from UNOS data, sample from Saidman distribution)
		Random r = new Random();
		UNOSGenerator UNOSGen = UNOSGenerator.makeAndInitialize(IOUtil.getBaseUNOSFilePath(), ',', r);
		SaidmanPoolGenerator SaidmanGen = new SaidmanPoolGenerator(r);

		// List of generators we want to use
		List<PoolGenerator> genList = Arrays.asList(new PoolGenerator[] {
				UNOSGen, 
				//SaidmanGen,
		});

		// list of |V|s we'll iterate over
		List<Integer> graphSizeList = Arrays.asList(new Integer[] {
				//50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900, 950, 1000,
				50, 60, 70, 80, 90, 100, 150, 200, 250, 300, 350, 400,
		});

		int numGraphReps = 25; 

		// Optimize w.r.t. discounted or raw utility?
		boolean usingFailureProbabilities = true;
		FailureProbabilityUtil.ProbabilityDistribution failDist = FailureProbabilityUtil.ProbabilityDistribution.CONSTANT;
		if(!usingFailureProbabilities) {
			failDist = FailureProbabilityUtil.ProbabilityDistribution.NONE;
		}

		// Cycle and chain limits
		int cycleCap = 3;
		List<Integer> chainCapList = Arrays.asList(new Integer[] {
				//0,
				4,//3,5,//10,100,
				//100,
		});
		int infiniteChainCap = Integer.MAX_VALUE - 2;

		// Number of greedy packings per solve call
		int numGreedyReps = 100;

		// Upper bound on number of chains sampled per altruist in some heuristics
		int chainSamplesPerAltruist = 256;

		// Store output
		String path = "approx_" + System.currentTimeMillis() + ".csv";
		DriverApproxOutput out = null;
		try {
			out = new DriverApproxOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		for(int graphSize : graphSizeList) {
			for(int chainCap : chainCapList) {
				for(PoolGenerator gen : genList) {
					for(int graphRep=0; graphRep<numGraphReps; graphRep++) {

						IOUtil.dPrintln("\n*****\nGraph (|V|=" + graphSize + ", #" + graphRep + "/" + numGraphReps + "), cap: " + chainCap+ ", gen: " + gen.getClass().getSimpleName() + "\n*****\n");

						// Generate pool (~5% altruists, UNOS might be different);
						int numPairs = (int)Math.round(graphSize * 0.95);
						int numAlts = graphSize - numPairs;
						Pool pool = gen.generate(numPairs, numAlts);

						// If we're setting failure probabilities, do that here:
						if(usingFailureProbabilities) {
							FailureProbabilityUtil.setFailureProbability(pool, failDist, r);
						}

						// Bookkeeping
						out.set(Col.CHAIN_CAP, chainCap);	
						out.set(Col.CYCLE_CAP, cycleCap);	
						out.set(Col.NUM_PAIRS, pool.getNumPairs());
						out.set(Col.NUM_ALTS, pool.getNumAltruists());
						out.set(Col.GENERATOR, gen.getClass().getSimpleName());
						out.set(Col.APPROX_REP_COUNT, numGreedyReps);
						out.set(Col.FAILURE_PROBABILITIES_USED, usingFailureProbabilities);
						out.set(Col.FAILURE_PROBABILITY_DIST, failDist.toString());
						out.set(Col.CYCLE_CYCCHAIN_CONSTANT, chainSamplesPerAltruist);

						// Generate all cycles in pool
						long startCycleGen = System.nanoTime();
						CycleGenerator cg = new CycleGenerator(pool);
						List<Cycle> cycles = cg.generateCyclesAndChains(cycleCap, chainCap, usingFailureProbabilities);
						CycleMembership membership = new CycleMembership(pool, cycles);
						long endCycleGen = System.nanoTime();
						out.set(Col.CYCLE_GEN_TIME, endCycleGen - startCycleGen);

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

						// Get greedy packings (upper-bounded by optimal solution's objective)
						Solution greedyCycleSol = null;
						Solution greedyCycleLPRelaxSol = null;
						Solution greedyVertexUniformSol = null;
						Solution greedyVertexInvPropSol = null;
						Solution greedyVertexRandWalkSol = null;
						Solution greedyCyclesThenChainsSol = null;
						Solution greedyCyclesSampleChainsIPSol = null;
						// Upper bound from IP might be below absolute upper bound because we do not include very long chains
						double upperBoundFromReducedIP = (null==optSol) ? Double.MAX_VALUE : optSol.getObjectiveValue();
						double upperBound = Double.MAX_VALUE;
						try {
							GreedyPackingSolver s = new GreedyPackingSolver(pool);

							greedyCycleSol = s.solve(numGreedyReps, new CycleShufflePacker(pool, cycles), upperBoundFromReducedIP);
							IOUtil.dPrintln("Greedy Cycle [UNIFORM] Value: " + greedyCycleSol.getObjectiveValue());

							greedyCycleLPRelaxSol = s.solve(numGreedyReps, new CycleLPRelaxationPacker(pool, cycles, membership, false), upperBoundFromReducedIP);
							IOUtil.dPrintln("Greedy Cycle [LPRELAX] Value: " + greedyCycleLPRelaxSol.getObjectiveValue());

							greedyVertexUniformSol = s.solve(numGreedyReps, new VertexShufflePacker(pool, cycles, membership, ShuffleType.UNIFORM_RANDOM), upperBoundFromReducedIP);
							IOUtil.dPrintln("Greedy Vertex [UNIFORM] Value: " + greedyVertexUniformSol.getObjectiveValue());

							greedyVertexInvPropSol = s.solve(numGreedyReps, new VertexShufflePacker(pool, cycles, membership, ShuffleType.INVERSE_PROP_CYCLE_COUNT), upperBoundFromReducedIP);
							IOUtil.dPrintln("Greedy Vertex [INVPROP] Value: " + greedyVertexInvPropSol.getObjectiveValue());

							greedyVertexRandWalkSol = s.solve(numGreedyReps, new VertexRandomWalkPacker(pool, cycles, membership, ShuffleType.INVERSE_PROP_CYCLE_COUNT, infiniteChainCap, usingFailureProbabilities), upperBoundFromReducedIP);
							IOUtil.dPrintln("Greedy Vertex [RANDWALK] Value: " + greedyVertexRandWalkSol.getObjectiveValue());


							// Generate only 2- and 3-cycles for the cycles-then-chain packing heuristics
							long startReducedCycleGen = System.nanoTime();
							List<Cycle> reducedCycles = (new CycleGenerator(pool)).generateCyclesAndChains(3, 0, usingFailureProbabilities);
							CycleMembership reducedMembership = new CycleMembership(pool, reducedCycles);
							long endReducedCycleGen = System.nanoTime();
							out.set(Col.CYCLE_REDUCED_GEN_TIME, endReducedCycleGen - startReducedCycleGen);

							greedyCyclesThenChainsSol = s.solve(numGreedyReps, new CyclesThenChainsPacker(pool, reducedCycles, reducedMembership, chainSamplesPerAltruist, false, infiniteChainCap, usingFailureProbabilities), upperBound);
							IOUtil.dPrintln("Greedy Cycle [CYCCHAIN] Value: " + greedyCyclesThenChainsSol.getObjectiveValue());

							greedyCyclesSampleChainsIPSol = s.solve(numGreedyReps, new CyclesSampleChainsIPPacker(pool, reducedCycles, chainSamplesPerAltruist, infiniteChainCap, usingFailureProbabilities), upperBound);
							IOUtil.dPrintln("Greedy Cycle [IPSAMPLE] Value: " + greedyCyclesSampleChainsIPSol.getObjectiveValue());

						} catch(SolverException e) {
							e.printStackTrace();
							System.exit(-1);
						}

						// Compare the greedy and optimal solutions
						if(null != optSol) {
							out.set(Col.OPT_OBJECTIVE, optSol.getObjectiveValue());	
							out.set(Col.OPT_RUNTIME, optSol.getSolveTime());
						}

						if(null != greedyCycleSol) {
							out.set(Col.APPROX_CYCLE_UNIFORM_OBJECTIVE, greedyCycleSol.getObjectiveValue());		
							out.set(Col.APPROX_CYCLE_UNIFORM_RUNTIME, greedyCycleSol.getSolveTime());
						}

						if(null != greedyCycleLPRelaxSol) {
							out.set(Col.APPROX_CYCLE_LPRELAX_OBJECTIVE, greedyCycleLPRelaxSol.getObjectiveValue());		
							out.set(Col.APPROX_CYCLE_LPRELAX_RUNTIME, greedyCycleLPRelaxSol.getSolveTime());
						}

						if(null != greedyVertexUniformSol) {
							out.set(Col.APPROX_VERTEX_UNIFORM_OBJECTIVE, greedyVertexUniformSol.getObjectiveValue());		
							out.set(Col.APPROX_VERTEX_UNIFORM_RUNTIME, greedyVertexUniformSol.getSolveTime());
						}

						if(null != greedyVertexInvPropSol) {
							out.set(Col.APPROX_VERTEX_INVPROP_OBJECTIVE, greedyVertexInvPropSol.getObjectiveValue());		
							out.set(Col.APPROX_VERTEX_INVPROP_RUNTIME, greedyVertexInvPropSol.getSolveTime());
						}

						if(null != greedyVertexRandWalkSol) {
							out.set(Col.APPROX_VERTEX_RANDWALK_OBJECTIVE, greedyVertexRandWalkSol.getObjectiveValue());		
							out.set(Col.APPROX_VERTEX_RANDWALK_RUNTIME, greedyVertexRandWalkSol.getSolveTime());
						}

						if(null != greedyCyclesThenChainsSol) {
							out.set(Col.APPROX_CYCLE_CYCCHAIN_OBJECTIVE, greedyCyclesThenChainsSol.getObjectiveValue());		
							out.set(Col.APPROX_CYCLE_CYCCHAIN_RUNTIME, greedyCyclesThenChainsSol.getSolveTime());
						}

						if(null != greedyCyclesSampleChainsIPSol) {
							out.set(Col.APPROX_CYCLE_IPSAMPLE_OBJECTIVE, greedyCyclesSampleChainsIPSol.getObjectiveValue());		
							out.set(Col.APPROX_CYCLE_IPSAMPLE_RUNTIME, greedyCyclesSampleChainsIPSol.getSolveTime());
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
				} // gen : genList
			} // chainCap : chainCapList
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
