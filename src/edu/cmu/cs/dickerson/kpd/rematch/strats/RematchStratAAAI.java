package edu.cmu.cs.dickerson.kpd.rematch.strats;

import java.util.Map;

import edu.cmu.cs.dickerson.kpd.rematch.RematchOutput;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;

public class RematchStratAAAI extends RematchStrat {


	protected final RematchOutput out;
	protected final Pool pool; 
	protected final int chainCap;
	protected final Map<Edge, Boolean> edgeFailedMap;
	protected final Map<Edge, Double> edgeFailureRateMap;
	protected final String generatorName;
	protected final int hardMaxPerVertex;
	protected final double failureRate;
	protected final double oracleMatchUtil;
	protected final long seed;
	
	public RematchStratAAAI(RematchOutput out, 
			Pool pool, 
			int chainCap,
			Map<Edge, Boolean> edgeFailedMap,
			Map<Edge, Double> edgeFailureRateMap, 
			String generatorName,
			int hardMaxPerVertex, 
			double failureRate, 
			double oracleMatchUtil,
			long seed) {
		
		this.out = out;
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
	 * Runs the experiments for the AAAI submission
	 */
	public void runRematch() throws SolverException {
		
		
	}
	
}
