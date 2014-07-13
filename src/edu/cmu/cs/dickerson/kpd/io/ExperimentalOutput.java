package edu.cmu.cs.dickerson.kpd.io;

import java.io.IOException;

public class ExperimentalOutput extends Output {

	public enum Col implements OutputCol { 
		START_TIME, 
		RANDOM_SEED, 
		NUM_PAIRS, 
		NUM_ALTS, 
		CYCLE_CAP, 
		CHAIN_CAP, 
		GENERATOR, 
		HIGHLY_SENSITIZED_CPRA, 
		HIGHLY_SENSITIZED_COUNT, 
		ALPHA_STAR, 
		FAIR_OBJECTIVE, 
		UNFAIR_OBJECTIVE,
		FAIR_HIGHLY_SENSITIZED_MATCHED,
		FAIR_TOTAL_CARDINALITY_MATCHED,
		UNFAIR_HIGHLY_SENSITIZED_MATCHED,
		UNFAIR_TOTAL_CARDINALITY_MATCHED,
		FAILURE_PROBABILITIES_USED,
		FAILURE_PROBABILITY_DIST,
		FAILURE_PARAMETER_1,
		FAIR_EXPECTED_HIGHLY_SENSITIZED_MATCHED,
		FAIR_EXPECTED_TOTAL_CARDINALITY_MATCHED,
		UNFAIR_EXPECTED_HIGHLY_SENSITIZED_MATCHED,
		UNFAIR_EXPECTED_TOTAL_CARDINALITY_MATCHED,
		;
		
		public int getColIdx() { return this.ordinal(); }
		};

	public ExperimentalOutput(String path) throws IOException {
		super(path, getHeader());
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.START_TIME.ordinal()] = "Start Time";
		header[Col.RANDOM_SEED.ordinal()] = "Random Seed";
		header[Col.NUM_PAIRS.ordinal()] = "Num Pairs";
		header[Col.NUM_ALTS.ordinal()] = "Num Alts";
		header[Col.CYCLE_CAP.ordinal()] = "Cycle Cap";
		header[Col.CHAIN_CAP.ordinal()] = "Chain Cap";
		header[Col.GENERATOR.ordinal()] = "Generator";
		header[Col.HIGHLY_SENSITIZED_CPRA.ordinal()] = "Highly-Sensitized CPRA";
		header[Col.HIGHLY_SENSITIZED_COUNT.ordinal()] = "Highly-Sensitized Count";
		header[Col.ALPHA_STAR.ordinal()] = "Alpha*";
		header[Col.FAIR_OBJECTIVE.ordinal()] = "Fair Objective";
		header[Col.UNFAIR_OBJECTIVE.ordinal()] = "Unfair Objective";
		header[Col.FAIR_TOTAL_CARDINALITY_MATCHED.ordinal()] = "Fair Total Matched";
		header[Col.UNFAIR_TOTAL_CARDINALITY_MATCHED.ordinal()] = "Unfair Total Matched";
		header[Col.FAIR_HIGHLY_SENSITIZED_MATCHED.ordinal()] = "Fair Highly-Sensitized Matched";
		header[Col.UNFAIR_HIGHLY_SENSITIZED_MATCHED.ordinal()] = "Unfair Highly-Sensitized Matched";
		header[Col.FAILURE_PROBABILITIES_USED.ordinal()] = "Failure Probabilities Used?";
		header[Col.FAILURE_PROBABILITY_DIST.ordinal()] = "Failure Probability Distribution";
		header[Col.FAILURE_PARAMETER_1.ordinal()] = "Failure Parameter 1";
		header[Col.FAIR_EXPECTED_HIGHLY_SENSITIZED_MATCHED.ordinal()] = "Fair Expected Highly-Sensitized Matched";
		header[Col.UNFAIR_EXPECTED_HIGHLY_SENSITIZED_MATCHED.ordinal()] = "Unfair Expected Highly-Sensitized Matched";
		header[Col.FAIR_EXPECTED_TOTAL_CARDINALITY_MATCHED.ordinal()] = "Fair Expected Total Matched";
		header[Col.UNFAIR_EXPECTED_TOTAL_CARDINALITY_MATCHED.ordinal()] = "Unfair Expected Total Matched";
		return header;
	}

}
