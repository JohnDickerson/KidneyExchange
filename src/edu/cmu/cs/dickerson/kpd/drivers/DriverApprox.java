package edu.cmu.cs.dickerson.kpd.drivers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.io.DriverApproxOutput;
import edu.cmu.cs.dickerson.kpd.io.DriverApproxOutput.Col;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationLPRelaxCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.GreedyPackingSolver;
import edu.cmu.cs.dickerson.kpd.solver.approx.CorrelatedChainSamplePacker;
import edu.cmu.cs.dickerson.kpd.solver.approx.CycleLPRelaxationPacker;
import edu.cmu.cs.dickerson.kpd.solver.approx.CycleShufflePacker;
import edu.cmu.cs.dickerson.kpd.solver.approx.CyclesSampleChainsIPPacker;
import edu.cmu.cs.dickerson.kpd.solver.approx.CyclesThenChainsIPPacker;
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

		boolean doOptIP = true;  // Do optimal IP solve on C(3,4)
		boolean doOptLP = true;  // Do optimal LP solve on C(3,4)
		boolean doOptIPUB2 = true;  // Do optimal IP solve on C(3,4) but using infinite chain extension on 4-chains
		boolean doCycle = false;  // Uniform sample cycles packing
		boolean doCycleLPRelax = false;  // Take LP relaxation on C(3,4), sample based on weights, pack
		boolean doVertexUniform = false;  // Sample vertices uniformly, then cycles containing vertex, then pack
		boolean doVertexInvProp = false;  // Weighted sample vertices inversely prop to #cycles containing, pack
		boolean doVertexRandWalk = false;  // Pack random walk chains/cycles from random sample vertex
		boolean doCyclesThenChains = false;  // LP relax pack on C(3,0), random sample altruist random walk pack best path
		boolean doCyclesSampleChainsIP = false;  // Sample a bunch of chains, solve IP on those chains + C(3,0)
		boolean doCyclesThenChainsIP = false;  // Solve IP on C(3,0), sample chains, solve IP on C(3,0) + those chains
		boolean doCorrelatedChainSample = true;  // Sample chains in a reasonable way, solve LP on C(3,0) + those chains
		
		// Set up our random generators for pools (sample from UNOS data, sample from Saidman distribution)
		Random r = new Random();
		//UNOSGenerator UNOSGen = UNOSGenerator.makeAndInitialize(IOUtil.getBaseUNOSFilePath(), ',', r);
		SaidmanPoolGenerator SaidmanGen = new SaidmanPoolGenerator(r);

		// List of generators we want to use
		List<PoolGenerator> genList = Arrays.asList(new PoolGenerator[] {
				//UNOSGen, 
				SaidmanGen,
		});

		// list of |V|s we'll iterate over
		List<Integer> graphSizeList = Arrays.asList(new Integer[] {
				//50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900, 950, 1000,
				50, 100, 150, 200, 250, 300, 
		//		400, 500, 600, 700, 800, 900, 1000, 1250, 1500,
		});

		// Number of randomly generated graphs per parameter vector instantiation
		int numGraphReps = 25; 

		// Optimize w.r.t. discounted or raw utility?
		boolean usingFailureProbabilities = true;
		FailureProbabilityUtil.ProbabilityDistribution failDist = FailureProbabilityUtil.ProbabilityDistribution.CONSTANT;
		double failure_param1 = 0.7;  // e.g., constant failure rate of 70%
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
		int numGreedyReps = 25;

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
						GreedyPackingSolver s = new GreedyPackingSolver(pool);

						// If we're setting failure probabilities, do that here:
						if(usingFailureProbabilities) {
							FailureProbabilityUtil.setFailureProbability(pool, failDist, r, failure_param1);
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
						out.set(Col.FAILURE_PARAMETER_1, failure_param1);
						out.set(Col.CYCLE_CYCCHAIN_CONSTANT, chainSamplesPerAltruist);

						// All solutions (optimal, greedy packings, etc)
						Solution optSolIP = null;
						Solution optSolLP = null;
						Solution optSolIPUB2 = null;
						Solution greedyCycleSol = null;
						Solution greedyCycleLPRelaxSol = null;
						Solution greedyVertexUniformSol = null;
						Solution greedyVertexInvPropSol = null;
						Solution greedyVertexRandWalkSol = null;
						Solution greedyCyclesThenChainsSol = null;
						Solution greedyCyclesSampleChainsIPSol = null;
						Solution greedyCyclesThenChainsIPSol = null;
						Solution greedyCorrelatedChainSampleSol = null;
						
						// Upper bound from IP might be below absolute upper bound because we do not include very long chains
						double upperBoundFromReducedIP = Double.MAX_VALUE;
						double upperBound = Double.MAX_VALUE;

						List<Cycle> cycles = null;
						CycleMembership membership = null;
						if(graphSize <= 250) {//Integer.MAX_VALUE) {
							// Generate all cycles in pool
							long startCycleGen = System.nanoTime();
							CycleGenerator cg = new CycleGenerator(pool);
							cycles = cg.generateCyclesAndChains(cycleCap, chainCap, usingFailureProbabilities);
							membership = new CycleMembership(pool, cycles);
							long endCycleGen = System.nanoTime();
							out.set(Col.CYCLE_GEN_TIME, endCycleGen - startCycleGen);


							try {
								if(doOptIP) {
									// Get optimal match size for pool on C(3,some non-infinite chain cap)
									CycleFormulationCPLEXSolver optIPS = new CycleFormulationCPLEXSolver(pool, cycles, membership);
									optSolIP = optIPS.solve();
									IOUtil.dPrintln("'Optimal IP' (chain-capped) Value: " + optSolIP.getObjectiveValue());
								}
								if(doOptLP) {
									// Get LP relaxation match size for pool on C(3,some non-infinite chain cap)
									CycleFormulationLPRelaxCPLEXSolver optLPS = new CycleFormulationLPRelaxCPLEXSolver(pool, cycles, membership);
									optSolLP = optLPS.solve().getLeft();
									IOUtil.dPrintln("'Optimal LP' (chain-capped) Value: " + optSolLP.getObjectiveValue());
								}
								if(doOptIPUB2) {
									List<Cycle> cyclesUB2 = cg.generateCyclesAndChains(cycleCap, chainCap, usingFailureProbabilities, true, failure_param1);
									CycleMembership membershipUB2 = new CycleMembership(pool, cyclesUB2);;
									// Get optimal match size for pool on C(3,some non-infinite chain cap + infinite extension)
									CycleFormulationCPLEXSolver optIPUB2S = new CycleFormulationCPLEXSolver(pool, cyclesUB2, membershipUB2);
									optSolIPUB2 = optIPUB2S.solve();
									IOUtil.dPrintln("'Optimal IP' (UB extension) Value: " + optSolIPUB2.getObjectiveValue());
									// Try to GC
									cyclesUB2 = null; membershipUB2 = null;
									System.gc();
								}
								
								// We can only compute upper bounds if we have optimal solves on the reduced IP or LP relaxation
								if(doOptIP || doOptLP) {
									// Compare against reduced IP value if available, otherwise settle for reduced LP relaxation
									double lowerObjValue = null!=optSolIP ? optSolIP.getObjectiveValue() : optSolLP.getObjectiveValue();
									// Get upper bound on optimal match for C(3,infinite chains)
									double trueOptimalUB = Double.MAX_VALUE;
									if(!usingFailureProbabilities) {
										// With deterministic matching, our UB technique is worthless ( :-( )
									} else if(FailureProbabilityUtil.ProbabilityDistribution.CONSTANT == failDist) {
										// Given success probability q = (1-failure_probability), an upper bound is
										// |Altruists| * q^(chain_cap) * (q / (1-q))
										// Recall: our chain cap includes the altruist (so a->v1->v2 is a 3-chain); subtract 1
										double q = 1.0 - failure_param1;
										if(q != 1.0) {
											double perChainUB = Math.pow(q, chainCap-1) * (q / (1.0-q));
											trueOptimalUB = lowerObjValue + (pool.getNumAltruists() * perChainUB);
										}
									} else {
										// We can bound using other distributions, but not implemented yet
									}
									out.set(Col.OPT_UB1_OBJECTIVE, trueOptimalUB);
									IOUtil.dPrintln("UB1 on true Optimal Value: " + trueOptimalUB);
								}

								if(doCycleLPRelax) {
									// LP relaxation based on full cycle and chain set
									upperBoundFromReducedIP = (null==optSolIP) ? Double.MAX_VALUE : optSolIP.getObjectiveValue();
									greedyCycleLPRelaxSol = s.solve(numGreedyReps, new CycleLPRelaxationPacker(pool, cycles, membership, false), upperBoundFromReducedIP);
									IOUtil.dPrintln("Greedy Cycle [LPRELAX] Value: " + greedyCycleLPRelaxSol.getObjectiveValue());
								}

							} catch(SolverException e) {
								e.printStackTrace();
								System.exit(-1);
							}
						}

						try {

							// Some of these heuristics take way too long and are bad, so only run on small graphs
							if(null != cycles && null != membership && graphSize <= 200) {
								if(doCycle) {
									greedyCycleSol = s.solve(numGreedyReps, new CycleShufflePacker(pool, cycles), upperBoundFromReducedIP);
									IOUtil.dPrintln("Greedy Cycle [UNIFORM] Value: " + greedyCycleSol.getObjectiveValue());
								}

								if(doVertexUniform) {
									greedyVertexUniformSol = s.solve(numGreedyReps, new VertexShufflePacker(pool, cycles, membership, ShuffleType.UNIFORM_RANDOM), upperBoundFromReducedIP);
									IOUtil.dPrintln("Greedy Vertex [UNIFORM] Value: " + greedyVertexUniformSol.getObjectiveValue());
								}

								if(doVertexInvProp) {
									greedyVertexInvPropSol = s.solve(numGreedyReps, new VertexShufflePacker(pool, cycles, membership, ShuffleType.INVERSE_PROP_CYCLE_COUNT), upperBoundFromReducedIP);
									IOUtil.dPrintln("Greedy Vertex [INVPROP] Value: " + greedyVertexInvPropSol.getObjectiveValue());
								}

								if(doVertexRandWalk) {
									greedyVertexRandWalkSol = s.solve(numGreedyReps, new VertexRandomWalkPacker(pool, cycles, membership, ShuffleType.INVERSE_PROP_CYCLE_COUNT, infiniteChainCap, usingFailureProbabilities), upperBoundFromReducedIP);
									IOUtil.dPrintln("Greedy Vertex [RANDWALK] Value: " + greedyVertexRandWalkSol.getObjectiveValue());
								}
							}

							// Try to kill off the full cycle/chain enumeration; hope against hope ...
							cycles=null;
							membership=null;
							System.gc();
							
							// Generate only 2- and 3-cycles for the cycles-then-chain packing heuristics
							long startReducedCycleGen = System.nanoTime();
							List<Cycle> reducedCycles = (new CycleGenerator(pool)).generateCyclesAndChains(3, 0, usingFailureProbabilities);
							CycleMembership reducedMembership = new CycleMembership(pool, reducedCycles);
							long endReducedCycleGen = System.nanoTime();
							out.set(Col.CYCLE_REDUCED_GEN_TIME, endReducedCycleGen - startReducedCycleGen);

							if(doCyclesThenChains) {
								greedyCyclesThenChainsSol = s.solve(numGreedyReps, new CyclesThenChainsPacker(pool, reducedCycles, reducedMembership, chainSamplesPerAltruist, false, infiniteChainCap, usingFailureProbabilities), upperBound);
								IOUtil.dPrintln("Greedy Cycle [CYCCHAIN] Value: " + greedyCyclesThenChainsSol.getObjectiveValue());
							}

							if(doCyclesSampleChainsIP) {
								greedyCyclesSampleChainsIPSol = s.solve(numGreedyReps, new CyclesSampleChainsIPPacker(pool, reducedCycles, chainSamplesPerAltruist, infiniteChainCap, usingFailureProbabilities), upperBound);
								IOUtil.dPrintln("Greedy Cycle [IPSAMPLE] Value: " + greedyCyclesSampleChainsIPSol.getObjectiveValue());
							}

							if(doCyclesThenChainsIP) {
								greedyCyclesThenChainsIPSol = s.solve(numGreedyReps, new CyclesThenChainsIPPacker(pool, reducedCycles, reducedMembership, chainSamplesPerAltruist, false, infiniteChainCap, usingFailureProbabilities), upperBound);
								IOUtil.dPrintln("Greedy Cycle [CYCCHAIN-IP] Value: " + greedyCyclesThenChainsIPSol.getObjectiveValue());
							}
							
							if(doCorrelatedChainSample) {
								greedyCorrelatedChainSampleSol = s.solve(numGreedyReps, new CorrelatedChainSamplePacker(pool, reducedCycles, chainSamplesPerAltruist, infiniteChainCap, usingFailureProbabilities), upperBound);
								IOUtil.dPrintln("Greedy Cycle [CHAIN-CYCLE] Value: " + greedyCorrelatedChainSampleSol.getObjectiveValue());	
							}
						} catch(SolverException e) {
							e.printStackTrace();
							System.exit(-1);
						}


						// Compare the greedy and optimal solutions
						if(null != optSolIP) {
							out.set(Col.OPT_IP_OBJECTIVE, optSolIP.getObjectiveValue());	
							out.set(Col.OPT_IP_RUNTIME, optSolIP.getSolveTime());
						}

						if(null != optSolLP) {
							out.set(Col.OPT_LP_OBJECTIVE, optSolLP.getObjectiveValue());	
							out.set(Col.OPT_LP_RUNTIME, optSolLP.getSolveTime());
						}
						
						if(null != optSolIPUB2) {
							out.set(Col.OPT_UB2_OBJECTIVE, optSolIPUB2.getObjectiveValue());	
							out.set(Col.OPT_UB2_RUNTIME, optSolIPUB2.getSolveTime());
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

						if(null != greedyCyclesThenChainsIPSol) {
							out.set(Col.APPROX_CYCLE_CYCCHAIN_IP_OBJECTIVE, greedyCyclesThenChainsIPSol.getObjectiveValue());		
							out.set(Col.APPROX_CYCLE_CYCCHAIN_IP_RUNTIME, greedyCyclesThenChainsIPSol.getSolveTime());
						}

						if(null != greedyCorrelatedChainSampleSol) {
							out.set(Col.APPROX_CORRELATED_CHAINS_OBJECTIVE, greedyCorrelatedChainSampleSol.getObjectiveValue());		
							out.set(Col.APPROX_CORRELATED_CHAINS_RUNTIME, greedyCorrelatedChainSampleSol.getSolveTime());
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
