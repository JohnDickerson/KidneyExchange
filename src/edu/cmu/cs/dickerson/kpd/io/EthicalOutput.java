package edu.cmu.cs.dickerson.kpd.io;

import java.io.IOException;

public class EthicalOutput extends Output {

	public enum Col implements OutputCol { 
		VERSION,
		SEED,
		NUM_ITERATIONS,
		ARRIVAL_PAIRS,
		ARRIVAL_ALTS,
		SEEN_PAIRS,
		SEEN_ALTS,
		DEPARTED_PAIRS,
		ALG_TYPE,
		SEEN_TYPE1,
		SEEN_TYPE2,
		SEEN_TYPE3,
		SEEN_TYPE4,
		SEEN_TYPE5,
		SEEN_TYPE6,
		SEEN_TYPE7,
		SEEN_TYPE8,
		MATCHED_TYPE1,
		MATCHED_TYPE2,
		MATCHED_TYPE3,
		MATCHED_TYPE4,
		MATCHED_TYPE5,
		MATCHED_TYPE6,
		MATCHED_TYPE7,
		MATCHED_TYPE8,
		;
		public int getColIdx() { return this.ordinal(); }
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.VERSION.getColIdx()] = "Weights Version";
		header[Col.SEED.getColIdx()] = "Seed";
		header[Col.NUM_ITERATIONS.getColIdx()] = "Number of Iterations";
		header[Col.ARRIVAL_PAIRS.getColIdx()] = "Expected Arrival Rate (Pairs)";
		header[Col.ARRIVAL_ALTS.getColIdx()] = "Expected Arrival Rate (Altruists)";
		header[Col.SEEN_PAIRS.getColIdx()] = "Total Pairs Seen";
		header[Col.SEEN_ALTS.getColIdx()] = "Total Altruists Seen";
		header[Col.DEPARTED_PAIRS.getColIdx()] = "Total Departed Pairs";
		header[Col.ALG_TYPE.getColIdx()] = "Algorithm Type";
		header[Col.SEEN_TYPE1.getColIdx()] = "Seen (Type 1)";
		header[Col.SEEN_TYPE2.getColIdx()] = "Seen (Type 2)";
		header[Col.SEEN_TYPE3.getColIdx()] = "Seen (Type 3)";
		header[Col.SEEN_TYPE4.getColIdx()] = "Seen (Type 4)";
		header[Col.SEEN_TYPE5.getColIdx()] = "Seen (Type 5)";
		header[Col.SEEN_TYPE6.getColIdx()] = "Seen (Type 6)";
		header[Col.SEEN_TYPE7.getColIdx()] = "Seen (Type 7)";
		header[Col.SEEN_TYPE8.getColIdx()] = "Seen (Type 8)";
		header[Col.MATCHED_TYPE1.getColIdx()] = "Matched (Type 1)";
		header[Col.MATCHED_TYPE2.getColIdx()] = "Matched (Type 2)";
		header[Col.MATCHED_TYPE3.getColIdx()] = "Matched (Type 3)";
		header[Col.MATCHED_TYPE4.getColIdx()] = "Matched (Type 4)";
		header[Col.MATCHED_TYPE5.getColIdx()] = "Matched (Type 5)";
		header[Col.MATCHED_TYPE6.getColIdx()] = "Matched (Type 6)";
		header[Col.MATCHED_TYPE7.getColIdx()] = "Matched (Type 7)";
		header[Col.MATCHED_TYPE8.getColIdx()] = "Matched (Type 8)";
		return header;
	}

	public EthicalOutput(String path) throws IOException {
		super(path, getHeader());
	}
}
