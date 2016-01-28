
package edu.cmu.cs.dickerson.kpd.io;

import java.io.IOException;

public class BitwiseAdhocOutput extends Output {

	public enum Col implements OutputCol { 
		GENERATOR,
		SEED,
		CYCLE_CAP,
		CHAIN_CAP,
		NUM_PAIRS,
		NUM_ALTS,
		NUM_VERTS,
		NUM_EDGES,
		THRESHOLD,
		NUM_CYCLES,
		MATCH_SIZE,
		;
		public int getColIdx() { return this.ordinal(); }
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.GENERATOR.getColIdx()] = "Generator";
		header[Col.SEED.getColIdx()] = "Seed";
		header[Col.CYCLE_CAP.getColIdx()] = "Cycle Cap";
		header[Col.CHAIN_CAP.getColIdx()] = "Chain Cap";
		header[Col.NUM_PAIRS.getColIdx()] = "Num Pairs";
		header[Col.NUM_ALTS.getColIdx()] = "Num Alts";
		header[Col.NUM_VERTS.getColIdx()] = "Num Verts";
		header[Col.NUM_EDGES.getColIdx()] = "Num Edges";
		header[Col.THRESHOLD.getColIdx()] = "t";
		header[Col.NUM_CYCLES.getColIdx()] = "Num Cycles";
		header[Col.MATCH_SIZE.getColIdx()] = "Match Size";
		return header;
	}

	public BitwiseAdhocOutput(String path) throws IOException {
		super(path, getHeader());
	}
}