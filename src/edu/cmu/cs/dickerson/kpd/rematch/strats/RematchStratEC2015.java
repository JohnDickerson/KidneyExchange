package edu.cmu.cs.dickerson.kpd.rematch.strats;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.rematch.RematchCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.rematch.RematchOutput;
import edu.cmu.cs.dickerson.kpd.rematch.RematchCPLEXSolver.RematchConstraintType;
import edu.cmu.cs.dickerson.kpd.rematch.RematchOutput.Col;
import edu.cmu.cs.dickerson.kpd.rematch.RematchUtil;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class RematchStratEC2015 extends RematchStrat {


	protected final Pool pool; 
	protected final int chainCap;
	protected final Map<Edge, Boolean> edgeFailedMap;
	protected final Map<Edge, Double> edgeFailureRateMap;
	protected final String generatorName;
	protected final int hardMaxPerVertex;
	protected final double failureRate;
	protected final double oracleMatchUtil;
	protected final long seed;
	
	public RematchStratEC2015(Pool pool, 
			int chainCap,
			Map<Edge, Boolean> edgeFailedMap,
			Map<Edge, Double> edgeFailureRateMap, 
			String generatorName,
			int hardMaxPerVertex, 
			double failureRate, 
			double oracleMatchUtil,
			long seed) {
		
		this.pool = pool;
		this.chainCap = chainCap;
		this.edgeFailedMap = edgeFailedMap;
		this.edgeFailureRateMap = edgeFailureRateMap;
		this.generatorName = generatorName;
		this.hardMaxPerVertex = hardMaxPerVertex;
		this.failureRate = failureRate;
		this.oracleMatchUtil = oracleMatchUtil;
		this.seed = seed;
	}

	/**
	 * Runs the experiments for the EC-15 paper
	 */
	public int runRematch(RematchOutput out,
			int maxNumRematches,
			RematchCPLEXSolver solver,
			RematchConstraintType rematchType
			) throws SolverException {

		// Get a set of edges that we should formally test (maps time period -> set of edges to test)
		CycleGenerator cg = new CycleGenerator(pool);
		List<Cycle> cycles = cg.generateCyclesAndChains(cycleCap, chainCap, true);
		Map<Integer, Set<Edge>> edgesToTestMap = solver.solve(maxNumRematches, rematchType, edgeFailedMap, maxAvgEdgesPerVertex);

		// Some of the rematchers change edge failure probabilities; reset here
		RematchUtil.resetPoolEdgeTestsToUnknown(edgeFailureRateMap);

		// Keep track of how many incoming edges to each vertex have been checked
		Map<Vertex, Set<Edge>> perVertexEdgeTested = new HashMap<Vertex, Set<Edge>>();
		for(Vertex v : pool.vertexSet()) {
			perVertexEdgeTested.put(v, new HashSet<Edge>()); 
		}

		// Get non-prescient match utilities for increasing number of allowed rematches
		Set<Edge> edgesToTestSet = new HashSet<Edge>(); // incrementally keep track of edges to test
		for(int numRematches=0; numRematches<=maxNumRematches; numRematches++) {
			// If we only want data for the last (highest) #rematches, skip there
			if(onlyPlotMaxRematch) {
				numRematches = maxNumRematches;
				// Add all #rematches' edge sets to the set of edges to test
				for(Map.Entry<Integer, Set<Edge>> reSet : edgesToTestMap.entrySet()) {
					edgesToTestSet.addAll( reSet.getValue() );
				}
			} else {
				// Add this #rematches' edge set to the total set of edges to test
				if(edgesToTestMap.containsKey(numRematches) && null!=edgesToTestMap.get(numRematches)) {
					edgesToTestSet.addAll( edgesToTestMap.get(numRematches) );
				}
			}

			// Initial bookkeeping
			out.set(Col.SEED, seed);
			out.set(Col.CYCLE_CAP, cycleCap);
			out.set(Col.CHAIN_CAP, chainCap);
			out.set(Col.NUM_PAIRS, pool.getPairs().size());
			out.set(Col.NUM_ALTRUISTS, pool.getAltruists().size());
			out.set(Col.NUM_EDGES, pool.getNumNonDummyEdges());
			out.set(Col.GENERATOR, generatorName);
			out.set(Col.MAX_AVG_EDGES_PER_VERT, maxAvgEdgesPerVertex);
			out.set(Col.HARD_MAX_EDGES_PER_VERT, hardMaxPerVertex);
			out.set(Col.REMATCH_TYPE, rematchType);
			out.set(Col.FAILURE_RATE, failureRate);
			out.set(Col.NUM_REMATCHES, numRematches);
			out.set(Col.ORACLE_MATCH_UTIL, oracleMatchUtil);

			// Update the pool with tested edges
			out.set(Col.NUM_EDGE_TESTS, edgesToTestSet.size());
			for(Edge e : edgesToTestSet) {
				Vertex dst = pool.getEdgeTarget(e);
				// If the destination vertex has remaining credits for testing edges, test this edge
				if(perVertexEdgeTested.get(dst).size() < hardMaxPerVertex) {
					e.setFailureProbability( edgeFailedMap.get(e) ? 1.0 : 0.0);
					perVertexEdgeTested.get(dst).add(e);
				}
			}

			// Do a max utility matching on this updated pool
			cycles = cg.generateCyclesAndChains(cycleCap, chainCap, true);
			Solution rematchSolution = (new CycleFormulationCPLEXSolver(
					pool, 
					cycles, 
					new CycleMembership(pool, cycles))
					).solve();

			// Now count the number of matches that actually went to transplant
			double numActualTransplants = RematchUtil.calculateNumTransplants(rematchSolution, pool, edgeFailedMap);
			out.set(Col.REMATCH_UTIL, numActualTransplants);

			cycles = null;

			// Write the  row of data
			try {
				out.record();
			} catch(IOException e) {
				IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
				e.printStackTrace();
				System.exit(-1);
			}
		} // end numRematchesList

		// Return the number of edges we tested, union'd over the entire set of rematches
		return edgesToTestSet.size();
		
	} // end of doRematchEC2015 method
}
