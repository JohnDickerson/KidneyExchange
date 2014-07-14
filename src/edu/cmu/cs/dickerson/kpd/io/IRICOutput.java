package edu.cmu.cs.dickerson.kpd.io;

import java.io.IOException;

public class IRICOutput extends Output {
	
	public enum Col implements OutputCol { 
		CYCLE_CAP,
		CHAIN_CAP,
		GENERATOR,
		NUM_HOSPITALS,
		FRAC_TRUTHFUL_HOSPITALS,
		NUM_TIME_PERIODS,
		ARRIVAL_DIST,
		ARRIVAL_MEAN,
		LIFE_EXPECTANCY_DIST,
		LIFE_EXPECTANCY_MEAN,
		NUM_MATCHED,
		NUM_EXTERNALLY_MATCHED,
		NUM_INTERNALLY_MATCHED,
		;
		public int getColIdx() { return this.ordinal(); }
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.CYCLE_CAP.getColIdx()] = "Cycle Cap";
		header[Col.CHAIN_CAP.getColIdx()] = "Chain Cap";
		header[Col.GENERATOR.getColIdx()] = "Generator";
		header[Col.NUM_HOSPITALS.getColIdx()] = "Num Hospitals";
		header[Col.FRAC_TRUTHFUL_HOSPITALS.getColIdx()] = "Fraction Truthful Hospitals";
		header[Col.NUM_TIME_PERIODS.getColIdx()] = "Num Time Periods";
		header[Col.ARRIVAL_DIST.getColIdx()] = "Arrival Distribution";
		header[Col.ARRIVAL_MEAN.getColIdx()] = "Arrival Mean";
		header[Col.LIFE_EXPECTANCY_DIST.getColIdx()] = "Life Expectancy Distribution";
		header[Col.LIFE_EXPECTANCY_MEAN.getColIdx()] = "Life Expectancy Mean";
		header[Col.NUM_MATCHED.getColIdx()] = "Num Matched";
		header[Col.NUM_EXTERNALLY_MATCHED.getColIdx()] = "Num Matched Externally";
		header[Col.NUM_INTERNALLY_MATCHED.getColIdx()] = "Num Matched Internally";
		return header;
	}
	
	public IRICOutput(String path) throws IOException {
		super(path, getHeader());
	}

}

