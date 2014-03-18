package edu.cmu.cs.dickerson.kpd.fairness.io;

import java.io.IOException;

public class DriverApproxOutput extends Output {
	public enum Col implements OutputCol { 
		NUM_PAIRS, 
		NUM_ALTS, 
		CYCLE_CAP, 
		CHAIN_CAP, 
		GENERATOR, 
		APPROX_REP_COUNT,
		OPT_OBJECTIVE,
		APPROX_OBJECTIVE,
		OPT_RUNTIME,
		APPROX_RUNTIME,
		;
		
		public int getColIdx() { return this.ordinal(); }
		};

	public DriverApproxOutput(String path) throws IOException {
		super(path, getHeader());
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.NUM_PAIRS.ordinal()] = "Num Pairs";
		header[Col.NUM_ALTS.ordinal()] = "Num Alts";
		header[Col.CYCLE_CAP.ordinal()] = "Cycle Cap";
		header[Col.CHAIN_CAP.ordinal()] = "Chain Cap";
		header[Col.GENERATOR.ordinal()] = "Generator";
		header[Col.APPROX_REP_COUNT.ordinal()] = "Approx Rep Count";
		header[Col.OPT_OBJECTIVE.ordinal()] = "Optimal Objective";
		header[Col.APPROX_OBJECTIVE.ordinal()] = "Approx Objective";
		header[Col.OPT_RUNTIME.ordinal()] = "Optimal Runtime";
		header[Col.APPROX_RUNTIME.ordinal()] = "Approx Runtime";
		
		
		return header;
	}
}
