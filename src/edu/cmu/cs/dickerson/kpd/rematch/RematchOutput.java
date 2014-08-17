package edu.cmu.cs.dickerson.kpd.rematch;

import java.io.IOException;

import edu.cmu.cs.dickerson.kpd.io.Output;
import edu.cmu.cs.dickerson.kpd.io.OutputCol;

public class RematchOutput extends Output {
	public enum Col implements OutputCol { 
		SEED,
		CYCLE_CAP,
		CHAIN_CAP,
		NUM_PAIRS,
		NUM_ALTRUISTS,
		NUM_EDGES,
		GENERATOR,
		MAX_AVG_EDGES_PER_VERT,
		HARD_MAX_EDGES_PER_VERT,
		REMATCH_TYPE,
		FAILURE_RATE,
		NUM_REMATCHES,
		NUM_EDGE_TESTS,
		REMATCH_UTIL,
		ORACLE_MATCH_UTIL,
		;
		public int getColIdx() { return this.ordinal(); }
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.SEED.getColIdx()] = "Seed";
		header[Col.CYCLE_CAP.getColIdx()] = "Cycle Cap";
		header[Col.CHAIN_CAP.getColIdx()] = "Chain Cap";
		header[Col.NUM_PAIRS.getColIdx()] = "Num Pairs";
		header[Col.NUM_ALTRUISTS.getColIdx()] = "Num Altruists";
		header[Col.NUM_EDGES.getColIdx()] = "Num Edges";
		header[Col.GENERATOR.getColIdx()] = "Generator";
		header[Col.MAX_AVG_EDGES_PER_VERT.getColIdx()] = "Max Avg Edges Per Vert";
		header[Col.HARD_MAX_EDGES_PER_VERT.getColIdx()] = "Hard Max Edges Per Vert";
		header[Col.REMATCH_TYPE.getColIdx()] = "Rematch Type";
		header[Col.FAILURE_RATE.getColIdx()] = "Failure Rate (constant)";
		header[Col.NUM_REMATCHES.getColIdx()] = "Num Rematches";
		header[Col.NUM_EDGE_TESTS.getColIdx()] = "Num Edge Tests";
		header[Col.REMATCH_UTIL.getColIdx()] = "Rematch Utility";
		header[Col.ORACLE_MATCH_UTIL.getColIdx()] = "Oracle Match Utility";
		return header;
	}
	
	public RematchOutput(String path) throws IOException {
		super(path, getHeader());
	}

}
