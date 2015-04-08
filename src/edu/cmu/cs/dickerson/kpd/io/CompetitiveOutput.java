package edu.cmu.cs.dickerson.kpd.io;

import java.io.IOException;

public class CompetitiveOutput extends Output {
	
	public enum Col implements OutputCol { 
		// Parameters for the simulation
		SEED_POOL,
		SEED_DYNAMIC,
		SEED_MATCHING,
		CYCLE_CAP,
		CHAIN_CAP,
		GENERATOR,
		GAMMA,
		ALPHA,
		M,
		LAMBDA,
		TIME_LIMIT,
		MATCHING_STRATEGY,
		// Results of the simulation
		TOTAL_SEEN,
		TOTAL_MATCHED,
		TOTAL_GREEDY_MATCHED,
		TOTAL_PATIENT_MATCHED,
		TOTAL_EXPIRED,
		;
		public int getColIdx() { return this.ordinal(); }
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.SEED_POOL.getColIdx()] = "Seed (Pool)";
		header[Col.SEED_DYNAMIC.getColIdx()] = "Seed (Dynamic)";
		header[Col.SEED_MATCHING.getColIdx()] = "Seed (Matching)";
		header[Col.CYCLE_CAP.getColIdx()] = "Cycle Cap";
		header[Col.CHAIN_CAP.getColIdx()] = "Chain Cap";
		header[Col.GENERATOR.getColIdx()] = "Generator";
		header[Col.GAMMA.getColIdx()] = "Gamma";
		header[Col.ALPHA.getColIdx()] = "Alpha";
		header[Col.M.getColIdx()] = "M";
		header[Col.LAMBDA.getColIdx()] = "Lambda";
		header[Col.TIME_LIMIT.getColIdx()] = "Time Limit";
		header[Col.MATCHING_STRATEGY.getColIdx()] = "Matching Strategy";
		header[Col.TOTAL_SEEN.getColIdx()] = "Total Seen";
		header[Col.TOTAL_MATCHED.getColIdx()] = "Total Matched";
		header[Col.TOTAL_GREEDY_MATCHED.getColIdx()] = "Total Greedy Matched";
		header[Col.TOTAL_PATIENT_MATCHED.getColIdx()] = "Total Patient Matched";
		header[Col.TOTAL_EXPIRED.getColIdx()] = "Total Expired";
		return header;
	}
	
	public CompetitiveOutput(String path) throws IOException {
		super(path, getHeader());
	}

}
