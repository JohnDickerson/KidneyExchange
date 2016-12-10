package edu.cmu.cs.dickerson.kpd.io;

import java.io.IOException;

public class FailureSensitivityOutput extends Output {

	public enum Col implements OutputCol { 
		SEED,
		CYCLE_CAP,
		CHAIN_CAP,
		GENERATOR,
		TRUE_FAILURE_PROB,
		ASSUMED_FAILURE_PROB,
		EXPECTED_TRANSPLANTS,
		REALIZED_TRANSPLANTS,
		;
		public int getColIdx() { return this.ordinal(); }
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.SEED.getColIdx()] = "Seed";
		header[Col.CYCLE_CAP.getColIdx()] = "Cycle Cap";
		header[Col.CHAIN_CAP.getColIdx()] = "Chain Cap";
		header[Col.GENERATOR.getColIdx()] = "Generator";
		header[Col.TRUE_FAILURE_PROB.getColIdx()] = "True Failure Prob";
		header[Col.ASSUMED_FAILURE_PROB.getColIdx()] = "Assumed Failure Prob";
		header[Col.EXPECTED_TRANSPLANTS.getColIdx()] = "Expected Transplants";
		header[Col.REALIZED_TRANSPLANTS.getColIdx()] = "Realized Transplants";
		
		return header;
	}
	
	public FailureSensitivityOutput(String path) throws IOException {
		super(path, getHeader());
	}
}
