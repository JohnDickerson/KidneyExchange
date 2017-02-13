package edu.cmu.cs.dickerson.kpd.io;

import java.io.IOException;

public class IRICHeteroOutput extends Output {

	public enum Col implements OutputCol {
		SEED_MAIN,
		SEED_ARRIVAL,
		SEED_LIFE,
		CYCLE_CAP,
		CHAIN_CAP,
		PCT_ALTRUISTS,
		GENERATOR,
		NUM_HOSPITALS,
		FRAC_TRUTHFUL_HOSPITALS,
		NUM_TIME_PERIODS,
		ARRIVAL_DIST1,
		ARRIVAL_MEAN1,
		ARRIVAL_DIST2,
		ARRIVAL_MEAN2,
		LIFE_EXPECTANCY_DIST,
		LIFE_EXPECTANCY_MEAN,
		NUM_MATCHED,
		NUM_EXTERNALLY_MATCHED,
		NUM_INTERNALLY_MATCHED,
		OVERALL_DOMINATED_TIME_PERIOD,
		AVG_HOSPITAL_DOMINATED_TIME_PERIOD,
		AVG_MATCHED_SMALL,
		AVG_MATCHED_LARGE
		;
		public int getColIdx() { return this.ordinal(); }
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.SEED_MAIN.getColIdx()] = "Seed (Main)";
		header[Col.SEED_ARRIVAL.getColIdx()] = "Seed (Arrival Rate)";
		header[Col.SEED_LIFE.getColIdx()] = "Seed (Life Expectancy)";
		header[Col.CYCLE_CAP.getColIdx()] = "Cycle Cap";
		header[Col.CHAIN_CAP.getColIdx()] = "Chain Cap";
		header[Col.PCT_ALTRUISTS.getColIdx()] = "Pct Altruists";
		header[Col.GENERATOR.getColIdx()] = "Generator";
		header[Col.NUM_HOSPITALS.getColIdx()] = "Num Hospitals";
		header[Col.FRAC_TRUTHFUL_HOSPITALS.getColIdx()] = "Fraction Truthful Hospitals";
		header[Col.NUM_TIME_PERIODS.getColIdx()] = "Num Time Periods";
		header[Col.ARRIVAL_DIST1.getColIdx()] = "Arrival Distribution Small";
		header[Col.ARRIVAL_DIST2.getColIdx()] = "Arrival Distribution Large";
		header[Col.ARRIVAL_MEAN1.getColIdx()] = "Arrival Mean";
		header[Col.ARRIVAL_MEAN2.getColIdx()] = "Arrival Mean";
		header[Col.LIFE_EXPECTANCY_DIST.getColIdx()] = "Life Expectancy Distribution";
		header[Col.LIFE_EXPECTANCY_MEAN.getColIdx()] = "Life Expectancy Mean";
		header[Col.NUM_MATCHED.getColIdx()] = "Num Matched";
		header[Col.NUM_EXTERNALLY_MATCHED.getColIdx()] = "Num Matched Externally";
		header[Col.NUM_INTERNALLY_MATCHED.getColIdx()] = "Num Matched Internally";
		header[Col.OVERALL_DOMINATED_TIME_PERIOD.getColIdx()] = "Overall Dominated (Time Period)";
		header[Col.AVG_HOSPITAL_DOMINATED_TIME_PERIOD.getColIdx()] = "Avg Hospital Dominatd (Time Period)";
		header[Col.AVG_MATCHED_SMALL.getColIdx()] = "Avg Matches Small Hospital";
		header[Col.AVG_MATCHED_LARGE.getColIdx()] = "Avg Matches Large Hospital";

		return header;
	}

	public IRICHeteroOutput(String path) throws IOException {
		super(path, getHeader());
	}

}
