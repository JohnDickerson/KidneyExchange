package edu.cmu.cs.dickerson.kpd.io;

import java.io.IOException;

public class DriverApproxOutput extends Output {
	public enum Col implements OutputCol { 
		NUM_PAIRS, 
		NUM_ALTS, 
		CYCLE_CAP, 
		CHAIN_CAP, 
		GENERATOR, 
		FAILURE_PROBABILITIES_USED,
		FAILURE_PROBABILITY_DIST,
		FAILURE_PARAMETER_1,
		APPROX_REP_COUNT,
		OPT_IP_OBJECTIVE,
		OPT_LP_OBJECTIVE,
		OPT_UB1_OBJECTIVE,
		OPT_UB2_OBJECTIVE,
		APPROX_CYCLE_UNIFORM_OBJECTIVE,
		APPROX_CYCLE_LPRELAX_OBJECTIVE,
		APPROX_VERTEX_UNIFORM_OBJECTIVE,
		APPROX_VERTEX_INVPROP_OBJECTIVE,
		APPROX_VERTEX_RANDWALK_OBJECTIVE,
		APPROX_CYCLE_CYCCHAIN_OBJECTIVE,
		APPROX_CYCLE_IPSAMPLE_OBJECTIVE,
		APPROX_CYCLE_CYCCHAIN_IP_OBJECTIVE,
		APPROX_CORRELATED_CHAINS_OBJECTIVE,
		CYCLE_CYCCHAIN_CONSTANT,
		CYCLE_GEN_TIME,
		CYCLE_REDUCED_GEN_TIME,  // only 2- and 3-cycles
		OPT_IP_RUNTIME,
		OPT_LP_RUNTIME,
		OPT_UB1_RUNTIME,
		OPT_UB2_RUNTIME,
		APPROX_CYCLE_UNIFORM_RUNTIME,
		APPROX_CYCLE_LPRELAX_RUNTIME,
		APPROX_VERTEX_UNIFORM_RUNTIME,
		APPROX_VERTEX_INVPROP_RUNTIME,
		APPROX_VERTEX_RANDWALK_RUNTIME,
		APPROX_CYCLE_CYCCHAIN_RUNTIME,
		APPROX_CYCLE_IPSAMPLE_RUNTIME,
		APPROX_CYCLE_CYCCHAIN_IP_RUNTIME,
		APPROX_CORRELATED_CHAINS_RUNTIME,
		;
		
		public int getColIdx() { return this.ordinal(); }
		};

	public DriverApproxOutput(String path) throws IOException {
		super(path, getHeader());
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.NUM_PAIRS.getColIdx()] = "Num Pairs";
		header[Col.NUM_ALTS.getColIdx()] = "Num Alts";
		header[Col.CYCLE_CAP.getColIdx()] = "Cycle Cap";
		header[Col.CHAIN_CAP.getColIdx()] = "Chain Cap";
		header[Col.GENERATOR.getColIdx()] = "Generator";
		header[Col.FAILURE_PROBABILITIES_USED.ordinal()] = "Failure Probabilities Used?";
		header[Col.FAILURE_PROBABILITY_DIST.ordinal()] = "Failure Probability Distribution";
		header[Col.FAILURE_PARAMETER_1.ordinal()] = "Failure Parameter 1";
		header[Col.APPROX_REP_COUNT.getColIdx()] = "Approx Rep Count";
		header[Col.OPT_IP_OBJECTIVE.getColIdx()] = "Reduced Optimal IP Objective";
		header[Col.OPT_LP_OBJECTIVE.getColIdx()] = "Reduced Optimal LP Objective";
		header[Col.OPT_UB1_OBJECTIVE.getColIdx()] = "Full Optimal UB1";
		header[Col.OPT_UB2_OBJECTIVE.getColIdx()] = "Full Optimal UB2";
		header[Col.APPROX_CYCLE_UNIFORM_OBJECTIVE.getColIdx()] = "Approx Cycle [UNIFORM] Objective";
		header[Col.APPROX_CYCLE_LPRELAX_OBJECTIVE.getColIdx()] = "Approx Cycle [LPRELAX] Objective";
		header[Col.APPROX_VERTEX_UNIFORM_OBJECTIVE.getColIdx()] = "Approx Vertex [UNIFORM] Objective";
		header[Col.APPROX_VERTEX_INVPROP_OBJECTIVE.getColIdx()] = "Approx Vertex [INVPROP] Objective";
		header[Col.APPROX_VERTEX_RANDWALK_OBJECTIVE.getColIdx()] = "Approx Vertex [RANDWALK] Objective";
		header[Col.APPROX_CYCLE_CYCCHAIN_OBJECTIVE.getColIdx()] = "Approx Cycle [CYCCHAIN] Objective";
		header[Col.APPROX_CYCLE_IPSAMPLE_OBJECTIVE.getColIdx()] = "Approx Cycle [IPSAMPLE] Objective";
		header[Col.APPROX_CYCLE_CYCCHAIN_IP_OBJECTIVE.getColIdx()] = "Approx Cycle [CYCCHAIN-IP] Objective";
		header[Col.APPROX_CORRELATED_CHAINS_OBJECTIVE.getColIdx()] = "Approx Cycle [CHAINS-CYCLES] Objective";
		header[Col.CYCLE_CYCCHAIN_CONSTANT.getColIdx()] = "CYCCHAIN Samples per Altruist";
		header[Col.CYCLE_GEN_TIME.getColIdx()] = "Cycle generation runtime";
		header[Col.CYCLE_REDUCED_GEN_TIME.getColIdx()] = "Cycle generation runtime (only 2- and 3-cycles)";
		header[Col.OPT_IP_RUNTIME.getColIdx()] = "Reduced Optimal IP Runtime";
		header[Col.OPT_LP_RUNTIME.getColIdx()] = "Reduced Optimal LP Runtime";
		header[Col.OPT_UB1_RUNTIME.getColIdx()] = "Full Optimal UB1 Runtime";
		header[Col.OPT_UB2_RUNTIME.getColIdx()] = "Full Optimal UB2 Runtime";
		header[Col.APPROX_CYCLE_UNIFORM_RUNTIME.getColIdx()] = "Approx Cycle [UNIFORM] Runtime";
		header[Col.APPROX_CYCLE_LPRELAX_RUNTIME.getColIdx()] = "Approx Cycle [LPRELAX] Runtime";
		header[Col.APPROX_VERTEX_UNIFORM_RUNTIME.getColIdx()] = "Approx Vertex [UNIFORM] Runtime";
		header[Col.APPROX_VERTEX_INVPROP_RUNTIME.getColIdx()] = "Approx Vertex [INVPROP] Runtime";
		header[Col.APPROX_VERTEX_RANDWALK_RUNTIME.getColIdx()] = "Approx Vertex [RANDWALK] Runtime";
		header[Col.APPROX_CYCLE_CYCCHAIN_RUNTIME.getColIdx()] = "Approx Cycle [CYCCHAIN] Runtime";
		header[Col.APPROX_CYCLE_IPSAMPLE_RUNTIME.getColIdx()] = "Approx Cycle [IPSAMPLE] Runtime";
		header[Col.APPROX_CYCLE_CYCCHAIN_IP_RUNTIME.getColIdx()] = "Approx Cycle [CYCCHAIN-IP] Runtime";
		header[Col.APPROX_CORRELATED_CHAINS_RUNTIME.getColIdx()] = "Approx Cycle [CHAINS-CYCLES] Runtime";
		
		return header;
	}
}
