package edu.cmu.cs.dickerson.kpd.io;

import java.io.IOException;

public class BitwiseOutput extends Output {

	public enum Col implements OutputCol { 
		GENERATOR,
		NUM_PAIRS,
		NUM_ALTS,
		NUM_EDGES,
		HIGHLY_SENSITIZED_CPRA,
		HIGHLY_SENSITIZED_COUNT,
		kBITLENGTH,
		THRESHOLD,
		RELATIVE_MIP_GAP,
		IS_FEASIBILITY_SOLVE,
		kINDUCIBLE,
		kINDUCIBLE_ERROR,
		;
		public int getColIdx() { return this.ordinal(); }
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.GENERATOR.getColIdx()] = "Generator";
		header[Col.NUM_PAIRS.getColIdx()] = "Num Pairs";
		header[Col.NUM_ALTS.getColIdx()] = "Num Alts";
		header[Col.NUM_EDGES.getColIdx()] = "Num Edges";
		header[Col.HIGHLY_SENSITIZED_CPRA.getColIdx()] = "Highly-sensitized threshold";
		header[Col.HIGHLY_SENSITIZED_COUNT.getColIdx()] = "Highly-senitized count";
		header[Col.kBITLENGTH.getColIdx()] = "k";
		header[Col.THRESHOLD.getColIdx()] = "t";
		header[Col.kINDUCIBLE.getColIdx()] = "k-inducible?";
		header[Col.kINDUCIBLE_ERROR.getColIdx()] = "k-induced min error";
		header[Col.RELATIVE_MIP_GAP.getColIdx()] = "Relative MIP Gap";
		header[Col.IS_FEASIBILITY_SOLVE.getColIdx()] = "Feasibility only?";
		return header;
	}

	public BitwiseOutput(String path) throws IOException {
		super(path, getHeader());
	}
}