package edu.cmu.cs.dickerson.kpd.io;

import java.io.IOException;

public class EthicalBloodOutput extends Output {

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
		
		// SEEN (enumerate all 128 blood-profile types)
		SEEN_1_O_O, SEEN_1_A_O, SEEN_1_B_O, SEEN_1_AB_O,
		SEEN_1_O_A, SEEN_1_A_A, SEEN_1_B_A, SEEN_1_AB_A,
		SEEN_1_O_B, SEEN_1_A_B, SEEN_1_B_B, SEEN_1_AB_B,
		SEEN_1_O_AB, SEEN_1_A_AB, SEEN_1_B_AB, SEEN_1_AB_AB,
		
		SEEN_2_O_O, SEEN_2_A_O, SEEN_2_B_O, SEEN_2_AB_O,
		SEEN_2_O_A, SEEN_2_A_A, SEEN_2_B_A, SEEN_2_AB_A,
		SEEN_2_O_B, SEEN_2_A_B, SEEN_2_B_B, SEEN_2_AB_B,
		SEEN_2_O_AB, SEEN_2_A_AB, SEEN_2_B_AB, SEEN_2_AB_AB,	
		
		SEEN_3_O_O, SEEN_3_A_O, SEEN_3_B_O, SEEN_3_AB_O,
		SEEN_3_O_A, SEEN_3_A_A, SEEN_3_B_A, SEEN_3_AB_A,
		SEEN_3_O_B, SEEN_3_A_B, SEEN_3_B_B, SEEN_3_AB_B,
		SEEN_3_O_AB, SEEN_3_A_AB, SEEN_3_B_AB, SEEN_3_AB_AB,
		
		SEEN_4_O_O, SEEN_4_A_O, SEEN_4_B_O, SEEN_4_AB_O,
		SEEN_4_O_A, SEEN_4_A_A, SEEN_4_B_A, SEEN_4_AB_A,
		SEEN_4_O_B, SEEN_4_A_B, SEEN_4_B_B, SEEN_4_AB_B,
		SEEN_4_O_AB, SEEN_4_A_AB, SEEN_4_B_AB, SEEN_4_AB_AB,
		
		SEEN_5_O_O, SEEN_5_A_O, SEEN_5_B_O, SEEN_5_AB_O,
		SEEN_5_O_A, SEEN_5_A_A, SEEN_5_B_A, SEEN_5_AB_A,
		SEEN_5_O_B, SEEN_5_A_B, SEEN_5_B_B, SEEN_5_AB_B,
		SEEN_5_O_AB, SEEN_5_A_AB, SEEN_5_B_AB, SEEN_5_AB_AB,
		
		SEEN_6_O_O, SEEN_6_A_O, SEEN_6_B_O, SEEN_6_AB_O,
		SEEN_6_O_A, SEEN_6_A_A, SEEN_6_B_A, SEEN_6_AB_A,
		SEEN_6_O_B, SEEN_6_A_B, SEEN_6_B_B, SEEN_6_AB_B,
		SEEN_6_O_AB, SEEN_6_A_AB, SEEN_6_B_AB, SEEN_6_AB_AB,	
		
		SEEN_7_O_O, SEEN_7_A_O, SEEN_7_B_O, SEEN_7_AB_O,
		SEEN_7_O_A, SEEN_7_A_A, SEEN_7_B_A, SEEN_7_AB_A,
		SEEN_7_O_B, SEEN_7_A_B, SEEN_7_B_B, SEEN_7_AB_B,
		SEEN_7_O_AB, SEEN_7_A_AB, SEEN_7_B_AB, SEEN_7_AB_AB,
		
		SEEN_8_O_O, SEEN_8_A_O, SEEN_8_B_O, SEEN_8_AB_O,
		SEEN_8_O_A, SEEN_8_A_A, SEEN_8_B_A, SEEN_8_AB_A,
		SEEN_8_O_B, SEEN_8_A_B, SEEN_8_B_B, SEEN_8_AB_B,
		SEEN_8_O_AB, SEEN_8_A_AB, SEEN_8_B_AB, SEEN_8_AB_AB,
		
		// MATCH (enumerate all 128 blood-profile types)
		MATCH_1_O_O, MATCH_1_A_O, MATCH_1_B_O, MATCH_1_AB_O,
		MATCH_1_O_A, MATCH_1_A_A, MATCH_1_B_A, MATCH_1_AB_A,
		MATCH_1_O_B, MATCH_1_A_B, MATCH_1_B_B, MATCH_1_AB_B,
		MATCH_1_O_AB, MATCH_1_A_AB, MATCH_1_B_AB, MATCH_1_AB_AB,
		
		MATCH_2_O_O, MATCH_2_A_O, MATCH_2_B_O, MATCH_2_AB_O,
		MATCH_2_O_A, MATCH_2_A_A, MATCH_2_B_A, MATCH_2_AB_A,
		MATCH_2_O_B, MATCH_2_A_B, MATCH_2_B_B, MATCH_2_AB_B,
		MATCH_2_O_AB, MATCH_2_A_AB, MATCH_2_B_AB, MATCH_2_AB_AB,	
		
		MATCH_3_O_O, MATCH_3_A_O, MATCH_3_B_O, MATCH_3_AB_O,
		MATCH_3_O_A, MATCH_3_A_A, MATCH_3_B_A, MATCH_3_AB_A,
		MATCH_3_O_B, MATCH_3_A_B, MATCH_3_B_B, MATCH_3_AB_B,
		MATCH_3_O_AB, MATCH_3_A_AB, MATCH_3_B_AB, MATCH_3_AB_AB,
		
		MATCH_4_O_O, MATCH_4_A_O, MATCH_4_B_O, MATCH_4_AB_O,
		MATCH_4_O_A, MATCH_4_A_A, MATCH_4_B_A, MATCH_4_AB_A,
		MATCH_4_O_B, MATCH_4_A_B, MATCH_4_B_B, MATCH_4_AB_B,
		MATCH_4_O_AB, MATCH_4_A_AB, MATCH_4_B_AB, MATCH_4_AB_AB,
		
		MATCH_5_O_O, MATCH_5_A_O, MATCH_5_B_O, MATCH_5_AB_O,
		MATCH_5_O_A, MATCH_5_A_A, MATCH_5_B_A, MATCH_5_AB_A,
		MATCH_5_O_B, MATCH_5_A_B, MATCH_5_B_B, MATCH_5_AB_B,
		MATCH_5_O_AB, MATCH_5_A_AB, MATCH_5_B_AB, MATCH_5_AB_AB,
		
		MATCH_6_O_O, MATCH_6_A_O, MATCH_6_B_O, MATCH_6_AB_O,
		MATCH_6_O_A, MATCH_6_A_A, MATCH_6_B_A, MATCH_6_AB_A,
		MATCH_6_O_B, MATCH_6_A_B, MATCH_6_B_B, MATCH_6_AB_B,
		MATCH_6_O_AB, MATCH_6_A_AB, MATCH_6_B_AB, MATCH_6_AB_AB,	
		
		MATCH_7_O_O, MATCH_7_A_O, MATCH_7_B_O, MATCH_7_AB_O,
		MATCH_7_O_A, MATCH_7_A_A, MATCH_7_B_A, MATCH_7_AB_A,
		MATCH_7_O_B, MATCH_7_A_B, MATCH_7_B_B, MATCH_7_AB_B,
		MATCH_7_O_AB, MATCH_7_A_AB, MATCH_7_B_AB, MATCH_7_AB_AB,
		
		MATCH_8_O_O, MATCH_8_A_O, MATCH_8_B_O, MATCH_8_AB_O,
		MATCH_8_O_A, MATCH_8_A_A, MATCH_8_B_A, MATCH_8_AB_A,
		MATCH_8_O_B, MATCH_8_A_B, MATCH_8_B_B, MATCH_8_AB_B,
		MATCH_8_O_AB, MATCH_8_A_AB, MATCH_8_B_AB, MATCH_8_AB_AB,
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
		
		// Quantity seen of all 128 profile x blood combinations
		header[Col.SEEN_1_O_O.getColIdx()] = "SEEN_1_O_O";
		header[Col.SEEN_1_A_O.getColIdx()] = "SEEN_1_A_O";
		header[Col.SEEN_1_B_O.getColIdx()] = "SEEN_1_B_O";
		header[Col.SEEN_1_AB_O.getColIdx()] = "SEEN_1_AB_O";
		header[Col.SEEN_1_O_A.getColIdx()] = "SEEN_1_O_A";
		header[Col.SEEN_1_A_A.getColIdx()] = "SEEN_1_A_A";
		header[Col.SEEN_1_B_A.getColIdx()] = "SEEN_1_B_A";
		header[Col.SEEN_1_AB_A.getColIdx()] = "SEEN_1_AB_A";
		header[Col.SEEN_1_O_B.getColIdx()] = "SEEN_1_O_B";
		header[Col.SEEN_1_A_B.getColIdx()] = "SEEN_1_A_B";
		header[Col.SEEN_1_B_B.getColIdx()] = "SEEN_1_B_B";
		header[Col.SEEN_1_AB_B.getColIdx()] = "SEEN_1_AB_B";
		header[Col.SEEN_1_O_AB.getColIdx()] = "SEEN_1_O_AB";
		header[Col.SEEN_1_A_AB.getColIdx()] = "SEEN_1_A_AB";
		header[Col.SEEN_1_B_AB.getColIdx()] = "SEEN_1_B_AB";
		header[Col.SEEN_1_AB_AB.getColIdx()] = "SEEN_1_AB_AB";
		header[Col.SEEN_2_O_O.getColIdx()] = "SEEN_2_O_O";
		header[Col.SEEN_2_A_O.getColIdx()] = "SEEN_2_A_O";
		header[Col.SEEN_2_B_O.getColIdx()] = "SEEN_2_B_O";
		header[Col.SEEN_2_AB_O.getColIdx()] = "SEEN_2_AB_O";
		header[Col.SEEN_2_O_A.getColIdx()] = "SEEN_2_O_A";
		header[Col.SEEN_2_A_A.getColIdx()] = "SEEN_2_A_A";
		header[Col.SEEN_2_B_A.getColIdx()] = "SEEN_2_B_A";
		header[Col.SEEN_2_AB_A.getColIdx()] = "SEEN_2_AB_A";
		header[Col.SEEN_2_O_B.getColIdx()] = "SEEN_2_O_B";
		header[Col.SEEN_2_A_B.getColIdx()] = "SEEN_2_A_B";
		header[Col.SEEN_2_B_B.getColIdx()] = "SEEN_2_B_B";
		header[Col.SEEN_2_AB_B.getColIdx()] = "SEEN_2_AB_B";
		header[Col.SEEN_2_O_AB.getColIdx()] = "SEEN_2_O_AB";
		header[Col.SEEN_2_A_AB.getColIdx()] = "SEEN_2_A_AB";
		header[Col.SEEN_2_B_AB.getColIdx()] = "SEEN_2_B_AB";
		header[Col.SEEN_2_AB_AB.getColIdx()] = "SEEN_2_AB_AB";
		header[Col.SEEN_3_O_O.getColIdx()] = "SEEN_3_O_O";
		header[Col.SEEN_3_A_O.getColIdx()] = "SEEN_3_A_O";
		header[Col.SEEN_3_B_O.getColIdx()] = "SEEN_3_B_O";
		header[Col.SEEN_3_AB_O.getColIdx()] = "SEEN_3_AB_O";
		header[Col.SEEN_3_O_A.getColIdx()] = "SEEN_3_O_A";
		header[Col.SEEN_3_A_A.getColIdx()] = "SEEN_3_A_A";
		header[Col.SEEN_3_B_A.getColIdx()] = "SEEN_3_B_A";
		header[Col.SEEN_3_AB_A.getColIdx()] = "SEEN_3_AB_A";
		header[Col.SEEN_3_O_B.getColIdx()] = "SEEN_3_O_B";
		header[Col.SEEN_3_A_B.getColIdx()] = "SEEN_3_A_B";
		header[Col.SEEN_3_B_B.getColIdx()] = "SEEN_3_B_B";
		header[Col.SEEN_3_AB_B.getColIdx()] = "SEEN_3_AB_B";
		header[Col.SEEN_3_O_AB.getColIdx()] = "SEEN_3_O_AB";
		header[Col.SEEN_3_A_AB.getColIdx()] = "SEEN_3_A_AB";
		header[Col.SEEN_3_B_AB.getColIdx()] = "SEEN_3_B_AB";
		header[Col.SEEN_3_AB_AB.getColIdx()] = "SEEN_3_AB_AB";
		header[Col.SEEN_4_O_O.getColIdx()] = "SEEN_4_O_O";
		header[Col.SEEN_4_A_O.getColIdx()] = "SEEN_4_A_O";
		header[Col.SEEN_4_B_O.getColIdx()] = "SEEN_4_B_O";
		header[Col.SEEN_4_AB_O.getColIdx()] = "SEEN_4_AB_O";
		header[Col.SEEN_4_O_A.getColIdx()] = "SEEN_4_O_A";
		header[Col.SEEN_4_A_A.getColIdx()] = "SEEN_4_A_A";
		header[Col.SEEN_4_B_A.getColIdx()] = "SEEN_4_B_A";
		header[Col.SEEN_4_AB_A.getColIdx()] = "SEEN_4_AB_A";
		header[Col.SEEN_4_O_B.getColIdx()] = "SEEN_4_O_B";
		header[Col.SEEN_4_A_B.getColIdx()] = "SEEN_4_A_B";
		header[Col.SEEN_4_B_B.getColIdx()] = "SEEN_4_B_B";
		header[Col.SEEN_4_AB_B.getColIdx()] = "SEEN_4_AB_B";
		header[Col.SEEN_4_O_AB.getColIdx()] = "SEEN_4_O_AB";
		header[Col.SEEN_4_A_AB.getColIdx()] = "SEEN_4_A_AB";
		header[Col.SEEN_4_B_AB.getColIdx()] = "SEEN_4_B_AB";
		header[Col.SEEN_4_AB_AB.getColIdx()] = "SEEN_4_AB_AB";
		header[Col.SEEN_5_O_O.getColIdx()] = "SEEN_5_O_O";
		header[Col.SEEN_5_A_O.getColIdx()] = "SEEN_5_A_O";
		header[Col.SEEN_5_B_O.getColIdx()] = "SEEN_5_B_O";
		header[Col.SEEN_5_AB_O.getColIdx()] = "SEEN_5_AB_O";
		header[Col.SEEN_5_O_A.getColIdx()] = "SEEN_5_O_A";
		header[Col.SEEN_5_A_A.getColIdx()] = "SEEN_5_A_A";
		header[Col.SEEN_5_B_A.getColIdx()] = "SEEN_5_B_A";
		header[Col.SEEN_5_AB_A.getColIdx()] = "SEEN_5_AB_A";
		header[Col.SEEN_5_O_B.getColIdx()] = "SEEN_5_O_B";
		header[Col.SEEN_5_A_B.getColIdx()] = "SEEN_5_A_B";
		header[Col.SEEN_5_B_B.getColIdx()] = "SEEN_5_B_B";
		header[Col.SEEN_5_AB_B.getColIdx()] = "SEEN_5_AB_B";
		header[Col.SEEN_5_O_AB.getColIdx()] = "SEEN_5_O_AB";
		header[Col.SEEN_5_A_AB.getColIdx()] = "SEEN_5_A_AB";
		header[Col.SEEN_5_B_AB.getColIdx()] = "SEEN_5_B_AB";
		header[Col.SEEN_5_AB_AB.getColIdx()] = "SEEN_5_AB_AB";
		header[Col.SEEN_6_O_O.getColIdx()] = "SEEN_6_O_O";
		header[Col.SEEN_6_A_O.getColIdx()] = "SEEN_6_A_O";
		header[Col.SEEN_6_B_O.getColIdx()] = "SEEN_6_B_O";
		header[Col.SEEN_6_AB_O.getColIdx()] = "SEEN_6_AB_O";
		header[Col.SEEN_6_O_A.getColIdx()] = "SEEN_6_O_A";
		header[Col.SEEN_6_A_A.getColIdx()] = "SEEN_6_A_A";
		header[Col.SEEN_6_B_A.getColIdx()] = "SEEN_6_B_A";
		header[Col.SEEN_6_AB_A.getColIdx()] = "SEEN_6_AB_A";
		header[Col.SEEN_6_O_B.getColIdx()] = "SEEN_6_O_B";
		header[Col.SEEN_6_A_B.getColIdx()] = "SEEN_6_A_B";
		header[Col.SEEN_6_B_B.getColIdx()] = "SEEN_6_B_B";
		header[Col.SEEN_6_AB_B.getColIdx()] = "SEEN_6_AB_B";
		header[Col.SEEN_6_O_AB.getColIdx()] = "SEEN_6_O_AB";
		header[Col.SEEN_6_A_AB.getColIdx()] = "SEEN_6_A_AB";
		header[Col.SEEN_6_B_AB.getColIdx()] = "SEEN_6_B_AB";
		header[Col.SEEN_6_AB_AB.getColIdx()] = "SEEN_6_AB_AB";
		header[Col.SEEN_7_O_O.getColIdx()] = "SEEN_7_O_O";
		header[Col.SEEN_7_A_O.getColIdx()] = "SEEN_7_A_O";
		header[Col.SEEN_7_B_O.getColIdx()] = "SEEN_7_B_O";
		header[Col.SEEN_7_AB_O.getColIdx()] = "SEEN_7_AB_O";
		header[Col.SEEN_7_O_A.getColIdx()] = "SEEN_7_O_A";
		header[Col.SEEN_7_A_A.getColIdx()] = "SEEN_7_A_A";
		header[Col.SEEN_7_B_A.getColIdx()] = "SEEN_7_B_A";
		header[Col.SEEN_7_AB_A.getColIdx()] = "SEEN_7_AB_A";
		header[Col.SEEN_7_O_B.getColIdx()] = "SEEN_7_O_B";
		header[Col.SEEN_7_A_B.getColIdx()] = "SEEN_7_A_B";
		header[Col.SEEN_7_B_B.getColIdx()] = "SEEN_7_B_B";
		header[Col.SEEN_7_AB_B.getColIdx()] = "SEEN_7_AB_B";
		header[Col.SEEN_7_O_AB.getColIdx()] = "SEEN_7_O_AB";
		header[Col.SEEN_7_A_AB.getColIdx()] = "SEEN_7_A_AB";
		header[Col.SEEN_7_B_AB.getColIdx()] = "SEEN_7_B_AB";
		header[Col.SEEN_7_AB_AB.getColIdx()] = "SEEN_7_AB_AB";
		header[Col.SEEN_8_O_O.getColIdx()] = "SEEN_8_O_O";
		header[Col.SEEN_8_A_O.getColIdx()] = "SEEN_8_A_O";
		header[Col.SEEN_8_B_O.getColIdx()] = "SEEN_8_B_O";
		header[Col.SEEN_8_AB_O.getColIdx()] = "SEEN_8_AB_O";
		header[Col.SEEN_8_O_A.getColIdx()] = "SEEN_8_O_A";
		header[Col.SEEN_8_A_A.getColIdx()] = "SEEN_8_A_A";
		header[Col.SEEN_8_B_A.getColIdx()] = "SEEN_8_B_A";
		header[Col.SEEN_8_AB_A.getColIdx()] = "SEEN_8_AB_A";
		header[Col.SEEN_8_O_B.getColIdx()] = "SEEN_8_O_B";
		header[Col.SEEN_8_A_B.getColIdx()] = "SEEN_8_A_B";
		header[Col.SEEN_8_B_B.getColIdx()] = "SEEN_8_B_B";
		header[Col.SEEN_8_AB_B.getColIdx()] = "SEEN_8_AB_B";
		header[Col.SEEN_8_O_AB.getColIdx()] = "SEEN_8_O_AB";
		header[Col.SEEN_8_A_AB.getColIdx()] = "SEEN_8_A_AB";
		header[Col.SEEN_8_B_AB.getColIdx()] = "SEEN_8_B_AB";
		header[Col.SEEN_8_AB_AB.getColIdx()] = "SEEN_8_AB_AB";
		
		// Quantity matched of all 128 profile x blood combinations
		header[Col.MATCH_1_O_O.getColIdx()] = "MATCH_1_O_O";
		header[Col.MATCH_1_A_O.getColIdx()] = "MATCH_1_A_O";
		header[Col.MATCH_1_B_O.getColIdx()] = "MATCH_1_B_O";
		header[Col.MATCH_1_AB_O.getColIdx()] = "MATCH_1_AB_O";
		header[Col.MATCH_1_O_A.getColIdx()] = "MATCH_1_O_A";
		header[Col.MATCH_1_A_A.getColIdx()] = "MATCH_1_A_A";
		header[Col.MATCH_1_B_A.getColIdx()] = "MATCH_1_B_A";
		header[Col.MATCH_1_AB_A.getColIdx()] = "MATCH_1_AB_A";
		header[Col.MATCH_1_O_B.getColIdx()] = "MATCH_1_O_B";
		header[Col.MATCH_1_A_B.getColIdx()] = "MATCH_1_A_B";
		header[Col.MATCH_1_B_B.getColIdx()] = "MATCH_1_B_B";
		header[Col.MATCH_1_AB_B.getColIdx()] = "MATCH_1_AB_B";
		header[Col.MATCH_1_O_AB.getColIdx()] = "MATCH_1_O_AB";
		header[Col.MATCH_1_A_AB.getColIdx()] = "MATCH_1_A_AB";
		header[Col.MATCH_1_B_AB.getColIdx()] = "MATCH_1_B_AB";
		header[Col.MATCH_1_AB_AB.getColIdx()] = "MATCH_1_AB_AB";
		header[Col.MATCH_2_O_O.getColIdx()] = "MATCH_2_O_O";
		header[Col.MATCH_2_A_O.getColIdx()] = "MATCH_2_A_O";
		header[Col.MATCH_2_B_O.getColIdx()] = "MATCH_2_B_O";
		header[Col.MATCH_2_AB_O.getColIdx()] = "MATCH_2_AB_O";
		header[Col.MATCH_2_O_A.getColIdx()] = "MATCH_2_O_A";
		header[Col.MATCH_2_A_A.getColIdx()] = "MATCH_2_A_A";
		header[Col.MATCH_2_B_A.getColIdx()] = "MATCH_2_B_A";
		header[Col.MATCH_2_AB_A.getColIdx()] = "MATCH_2_AB_A";
		header[Col.MATCH_2_O_B.getColIdx()] = "MATCH_2_O_B";
		header[Col.MATCH_2_A_B.getColIdx()] = "MATCH_2_A_B";
		header[Col.MATCH_2_B_B.getColIdx()] = "MATCH_2_B_B";
		header[Col.MATCH_2_AB_B.getColIdx()] = "MATCH_2_AB_B";
		header[Col.MATCH_2_O_AB.getColIdx()] = "MATCH_2_O_AB";
		header[Col.MATCH_2_A_AB.getColIdx()] = "MATCH_2_A_AB";
		header[Col.MATCH_2_B_AB.getColIdx()] = "MATCH_2_B_AB";
		header[Col.MATCH_2_AB_AB.getColIdx()] = "MATCH_2_AB_AB";
		header[Col.MATCH_3_O_O.getColIdx()] = "MATCH_3_O_O";
		header[Col.MATCH_3_A_O.getColIdx()] = "MATCH_3_A_O";
		header[Col.MATCH_3_B_O.getColIdx()] = "MATCH_3_B_O";
		header[Col.MATCH_3_AB_O.getColIdx()] = "MATCH_3_AB_O";
		header[Col.MATCH_3_O_A.getColIdx()] = "MATCH_3_O_A";
		header[Col.MATCH_3_A_A.getColIdx()] = "MATCH_3_A_A";
		header[Col.MATCH_3_B_A.getColIdx()] = "MATCH_3_B_A";
		header[Col.MATCH_3_AB_A.getColIdx()] = "MATCH_3_AB_A";
		header[Col.MATCH_3_O_B.getColIdx()] = "MATCH_3_O_B";
		header[Col.MATCH_3_A_B.getColIdx()] = "MATCH_3_A_B";
		header[Col.MATCH_3_B_B.getColIdx()] = "MATCH_3_B_B";
		header[Col.MATCH_3_AB_B.getColIdx()] = "MATCH_3_AB_B";
		header[Col.MATCH_3_O_AB.getColIdx()] = "MATCH_3_O_AB";
		header[Col.MATCH_3_A_AB.getColIdx()] = "MATCH_3_A_AB";
		header[Col.MATCH_3_B_AB.getColIdx()] = "MATCH_3_B_AB";
		header[Col.MATCH_3_AB_AB.getColIdx()] = "MATCH_3_AB_AB";
		header[Col.MATCH_4_O_O.getColIdx()] = "MATCH_4_O_O";
		header[Col.MATCH_4_A_O.getColIdx()] = "MATCH_4_A_O";
		header[Col.MATCH_4_B_O.getColIdx()] = "MATCH_4_B_O";
		header[Col.MATCH_4_AB_O.getColIdx()] = "MATCH_4_AB_O";
		header[Col.MATCH_4_O_A.getColIdx()] = "MATCH_4_O_A";
		header[Col.MATCH_4_A_A.getColIdx()] = "MATCH_4_A_A";
		header[Col.MATCH_4_B_A.getColIdx()] = "MATCH_4_B_A";
		header[Col.MATCH_4_AB_A.getColIdx()] = "MATCH_4_AB_A";
		header[Col.MATCH_4_O_B.getColIdx()] = "MATCH_4_O_B";
		header[Col.MATCH_4_A_B.getColIdx()] = "MATCH_4_A_B";
		header[Col.MATCH_4_B_B.getColIdx()] = "MATCH_4_B_B";
		header[Col.MATCH_4_AB_B.getColIdx()] = "MATCH_4_AB_B";
		header[Col.MATCH_4_O_AB.getColIdx()] = "MATCH_4_O_AB";
		header[Col.MATCH_4_A_AB.getColIdx()] = "MATCH_4_A_AB";
		header[Col.MATCH_4_B_AB.getColIdx()] = "MATCH_4_B_AB";
		header[Col.MATCH_4_AB_AB.getColIdx()] = "MATCH_4_AB_AB";
		header[Col.MATCH_5_O_O.getColIdx()] = "MATCH_5_O_O";
		header[Col.MATCH_5_A_O.getColIdx()] = "MATCH_5_A_O";
		header[Col.MATCH_5_B_O.getColIdx()] = "MATCH_5_B_O";
		header[Col.MATCH_5_AB_O.getColIdx()] = "MATCH_5_AB_O";
		header[Col.MATCH_5_O_A.getColIdx()] = "MATCH_5_O_A";
		header[Col.MATCH_5_A_A.getColIdx()] = "MATCH_5_A_A";
		header[Col.MATCH_5_B_A.getColIdx()] = "MATCH_5_B_A";
		header[Col.MATCH_5_AB_A.getColIdx()] = "MATCH_5_AB_A";
		header[Col.MATCH_5_O_B.getColIdx()] = "MATCH_5_O_B";
		header[Col.MATCH_5_A_B.getColIdx()] = "MATCH_5_A_B";
		header[Col.MATCH_5_B_B.getColIdx()] = "MATCH_5_B_B";
		header[Col.MATCH_5_AB_B.getColIdx()] = "MATCH_5_AB_B";
		header[Col.MATCH_5_O_AB.getColIdx()] = "MATCH_5_O_AB";
		header[Col.MATCH_5_A_AB.getColIdx()] = "MATCH_5_A_AB";
		header[Col.MATCH_5_B_AB.getColIdx()] = "MATCH_5_B_AB";
		header[Col.MATCH_5_AB_AB.getColIdx()] = "MATCH_5_AB_AB";
		header[Col.MATCH_6_O_O.getColIdx()] = "MATCH_6_O_O";
		header[Col.MATCH_6_A_O.getColIdx()] = "MATCH_6_A_O";
		header[Col.MATCH_6_B_O.getColIdx()] = "MATCH_6_B_O";
		header[Col.MATCH_6_AB_O.getColIdx()] = "MATCH_6_AB_O";
		header[Col.MATCH_6_O_A.getColIdx()] = "MATCH_6_O_A";
		header[Col.MATCH_6_A_A.getColIdx()] = "MATCH_6_A_A";
		header[Col.MATCH_6_B_A.getColIdx()] = "MATCH_6_B_A";
		header[Col.MATCH_6_AB_A.getColIdx()] = "MATCH_6_AB_A";
		header[Col.MATCH_6_O_B.getColIdx()] = "MATCH_6_O_B";
		header[Col.MATCH_6_A_B.getColIdx()] = "MATCH_6_A_B";
		header[Col.MATCH_6_B_B.getColIdx()] = "MATCH_6_B_B";
		header[Col.MATCH_6_AB_B.getColIdx()] = "MATCH_6_AB_B";
		header[Col.MATCH_6_O_AB.getColIdx()] = "MATCH_6_O_AB";
		header[Col.MATCH_6_A_AB.getColIdx()] = "MATCH_6_A_AB";
		header[Col.MATCH_6_B_AB.getColIdx()] = "MATCH_6_B_AB";
		header[Col.MATCH_6_AB_AB.getColIdx()] = "MATCH_6_AB_AB";
		header[Col.MATCH_7_O_O.getColIdx()] = "MATCH_7_O_O";
		header[Col.MATCH_7_A_O.getColIdx()] = "MATCH_7_A_O";
		header[Col.MATCH_7_B_O.getColIdx()] = "MATCH_7_B_O";
		header[Col.MATCH_7_AB_O.getColIdx()] = "MATCH_7_AB_O";
		header[Col.MATCH_7_O_A.getColIdx()] = "MATCH_7_O_A";
		header[Col.MATCH_7_A_A.getColIdx()] = "MATCH_7_A_A";
		header[Col.MATCH_7_B_A.getColIdx()] = "MATCH_7_B_A";
		header[Col.MATCH_7_AB_A.getColIdx()] = "MATCH_7_AB_A";
		header[Col.MATCH_7_O_B.getColIdx()] = "MATCH_7_O_B";
		header[Col.MATCH_7_A_B.getColIdx()] = "MATCH_7_A_B";
		header[Col.MATCH_7_B_B.getColIdx()] = "MATCH_7_B_B";
		header[Col.MATCH_7_AB_B.getColIdx()] = "MATCH_7_AB_B";
		header[Col.MATCH_7_O_AB.getColIdx()] = "MATCH_7_O_AB";
		header[Col.MATCH_7_A_AB.getColIdx()] = "MATCH_7_A_AB";
		header[Col.MATCH_7_B_AB.getColIdx()] = "MATCH_7_B_AB";
		header[Col.MATCH_7_AB_AB.getColIdx()] = "MATCH_7_AB_AB";
		header[Col.MATCH_8_O_O.getColIdx()] = "MATCH_8_O_O";
		header[Col.MATCH_8_A_O.getColIdx()] = "MATCH_8_A_O";
		header[Col.MATCH_8_B_O.getColIdx()] = "MATCH_8_B_O";
		header[Col.MATCH_8_AB_O.getColIdx()] = "MATCH_8_AB_O";
		header[Col.MATCH_8_O_A.getColIdx()] = "MATCH_8_O_A";
		header[Col.MATCH_8_A_A.getColIdx()] = "MATCH_8_A_A";
		header[Col.MATCH_8_B_A.getColIdx()] = "MATCH_8_B_A";
		header[Col.MATCH_8_AB_A.getColIdx()] = "MATCH_8_AB_A";
		header[Col.MATCH_8_O_B.getColIdx()] = "MATCH_8_O_B";
		header[Col.MATCH_8_A_B.getColIdx()] = "MATCH_8_A_B";
		header[Col.MATCH_8_B_B.getColIdx()] = "MATCH_8_B_B";
		header[Col.MATCH_8_AB_B.getColIdx()] = "MATCH_8_AB_B";
		header[Col.MATCH_8_O_AB.getColIdx()] = "MATCH_8_O_AB";
		header[Col.MATCH_8_A_AB.getColIdx()] = "MATCH_8_A_AB";
		header[Col.MATCH_8_B_AB.getColIdx()] = "MATCH_8_B_AB";
		header[Col.MATCH_8_AB_AB.getColIdx()] = "MATCH_8_AB_AB";

		
		return header;
	}

	public EthicalBloodOutput(String path) throws IOException {
		super(path, getHeader());
	}
	
	// "SEEN_1_O_O" --> header[Col.SEEN_1_O_O.getColIdx()] = "SEEN_1_O_O";
	public static void main(String args[]) {
		String s = "SEEN_1_O_O, SEEN_1_A_O, SEEN_1_B_O, SEEN_1_AB_O, SEEN_1_O_A, SEEN_1_A_A, SEEN_1_B_A, SEEN_1_AB_A, SEEN_1_O_B, SEEN_1_A_B, SEEN_1_B_B, SEEN_1_AB_B, SEEN_1_O_AB, SEEN_1_A_AB, SEEN_1_B_AB, SEEN_1_AB_AB, SEEN_2_O_O, SEEN_2_A_O, SEEN_2_B_O, SEEN_2_AB_O, SEEN_2_O_A, SEEN_2_A_A, SEEN_2_B_A, SEEN_2_AB_A, SEEN_2_O_B, SEEN_2_A_B, SEEN_2_B_B, SEEN_2_AB_B, SEEN_2_O_AB, SEEN_2_A_AB, SEEN_2_B_AB, SEEN_2_AB_AB, SEEN_3_O_O, SEEN_3_A_O, SEEN_3_B_O, SEEN_3_AB_O, SEEN_3_O_A, SEEN_3_A_A, SEEN_3_B_A, SEEN_3_AB_A, SEEN_3_O_B, SEEN_3_A_B, SEEN_3_B_B, SEEN_3_AB_B, SEEN_3_O_AB, SEEN_3_A_AB, SEEN_3_B_AB, SEEN_3_AB_AB, SEEN_4_O_O, SEEN_4_A_O, SEEN_4_B_O, SEEN_4_AB_O, SEEN_4_O_A, SEEN_4_A_A, SEEN_4_B_A, SEEN_4_AB_A, SEEN_4_O_B, SEEN_4_A_B, SEEN_4_B_B, SEEN_4_AB_B, SEEN_4_O_AB, SEEN_4_A_AB, SEEN_4_B_AB, SEEN_4_AB_AB, SEEN_5_O_O, SEEN_5_A_O, SEEN_5_B_O, SEEN_5_AB_O, SEEN_5_O_A, SEEN_5_A_A, SEEN_5_B_A, SEEN_5_AB_A, SEEN_5_O_B, SEEN_5_A_B, SEEN_5_B_B, SEEN_5_AB_B, SEEN_5_O_AB, SEEN_5_A_AB, SEEN_5_B_AB, SEEN_5_AB_AB, SEEN_6_O_O, SEEN_6_A_O, SEEN_6_B_O, SEEN_6_AB_O, SEEN_6_O_A, SEEN_6_A_A, SEEN_6_B_A, SEEN_6_AB_A, SEEN_6_O_B, SEEN_6_A_B, SEEN_6_B_B, SEEN_6_AB_B, SEEN_6_O_AB, SEEN_6_A_AB, SEEN_6_B_AB, SEEN_6_AB_AB, SEEN_7_O_O, SEEN_7_A_O, SEEN_7_B_O, SEEN_7_AB_O, SEEN_7_O_A, SEEN_7_A_A, SEEN_7_B_A, SEEN_7_AB_A, SEEN_7_O_B, SEEN_7_A_B, SEEN_7_B_B, SEEN_7_AB_B, SEEN_7_O_AB, SEEN_7_A_AB, SEEN_7_B_AB, SEEN_7_AB_AB, SEEN_8_O_O, SEEN_8_A_O, SEEN_8_B_O, SEEN_8_AB_O, SEEN_8_O_A, SEEN_8_A_A, SEEN_8_B_A, SEEN_8_AB_A, SEEN_8_O_B, SEEN_8_A_B, SEEN_8_B_B, SEEN_8_AB_B, SEEN_8_O_AB, SEEN_8_A_AB, SEEN_8_B_AB, SEEN_8_AB_AB, MATCH_1_O_O, MATCH_1_A_O, MATCH_1_B_O, MATCH_1_AB_O, MATCH_1_O_A, MATCH_1_A_A, MATCH_1_B_A, MATCH_1_AB_A, MATCH_1_O_B, MATCH_1_A_B, MATCH_1_B_B, MATCH_1_AB_B, MATCH_1_O_AB, MATCH_1_A_AB, MATCH_1_B_AB, MATCH_1_AB_AB, MATCH_2_O_O, MATCH_2_A_O, MATCH_2_B_O, MATCH_2_AB_O, MATCH_2_O_A, MATCH_2_A_A, MATCH_2_B_A, MATCH_2_AB_A, MATCH_2_O_B, MATCH_2_A_B, MATCH_2_B_B, MATCH_2_AB_B, MATCH_2_O_AB, MATCH_2_A_AB, MATCH_2_B_AB, MATCH_2_AB_AB, MATCH_3_O_O, MATCH_3_A_O, MATCH_3_B_O, MATCH_3_AB_O, MATCH_3_O_A, MATCH_3_A_A, MATCH_3_B_A, MATCH_3_AB_A, MATCH_3_O_B, MATCH_3_A_B, MATCH_3_B_B, MATCH_3_AB_B, MATCH_3_O_AB, MATCH_3_A_AB, MATCH_3_B_AB, MATCH_3_AB_AB, MATCH_4_O_O, MATCH_4_A_O, MATCH_4_B_O, MATCH_4_AB_O, MATCH_4_O_A, MATCH_4_A_A, MATCH_4_B_A, MATCH_4_AB_A, MATCH_4_O_B, MATCH_4_A_B, MATCH_4_B_B, MATCH_4_AB_B, MATCH_4_O_AB, MATCH_4_A_AB, MATCH_4_B_AB, MATCH_4_AB_AB, MATCH_5_O_O, MATCH_5_A_O, MATCH_5_B_O, MATCH_5_AB_O, MATCH_5_O_A, MATCH_5_A_A, MATCH_5_B_A, MATCH_5_AB_A, MATCH_5_O_B, MATCH_5_A_B, MATCH_5_B_B, MATCH_5_AB_B, MATCH_5_O_AB, MATCH_5_A_AB, MATCH_5_B_AB, MATCH_5_AB_AB, MATCH_6_O_O, MATCH_6_A_O, MATCH_6_B_O, MATCH_6_AB_O, MATCH_6_O_A, MATCH_6_A_A, MATCH_6_B_A, MATCH_6_AB_A, MATCH_6_O_B, MATCH_6_A_B, MATCH_6_B_B, MATCH_6_AB_B, MATCH_6_O_AB, MATCH_6_A_AB, MATCH_6_B_AB, MATCH_6_AB_AB, MATCH_7_O_O, MATCH_7_A_O, MATCH_7_B_O, MATCH_7_AB_O, MATCH_7_O_A, MATCH_7_A_A, MATCH_7_B_A, MATCH_7_AB_A, MATCH_7_O_B, MATCH_7_A_B, MATCH_7_B_B, MATCH_7_AB_B, MATCH_7_O_AB, MATCH_7_A_AB, MATCH_7_B_AB, MATCH_7_AB_AB, MATCH_8_O_O, MATCH_8_A_O, MATCH_8_B_O, MATCH_8_AB_O, MATCH_8_O_A, MATCH_8_A_A, MATCH_8_B_A, MATCH_8_AB_A, MATCH_8_O_B, MATCH_8_A_B, MATCH_8_B_B, MATCH_8_AB_B, MATCH_8_O_AB, MATCH_8_A_AB, MATCH_8_B_AB, MATCH_8_AB_AB";
		String[] names = s.split(", ");
		
		System.out.print("[");
		for (String name : names) {
			String[] n = name.split("_");
			if (n[0].equals("SEEN")) {
				System.out.print("\""+n[1]+"_"+n[2]+"_"+n[3]+"\", ");
			}	
		}
		System.out.println("];");
		
	}
}
