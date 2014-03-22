package edu.cmu.cs.dickerson.kpd.fairness.io;

import java.io.IOException;

public class DriverApproxOutput extends Output {
	public enum Col implements OutputCol { 
		NUM_PAIRS, 
		NUM_ALTS, 
		CYCLE_CAP, 
		CHAIN_CAP, 
		GENERATOR, 
		APPROX_REP_COUNT,
		OPT_OBJECTIVE,
		APPROX_CYCLE_OBJECTIVE,
		APPROX_VERTEX_UNIFORM_OBJECTIVE,
		APPROX_VERTEX_INVPROP_OBJECTIVE,
		CYCLE_GEN_TIME,
		OPT_RUNTIME,
		APPROX_CYCLE_RUNTIME,
		APPROX_VERTEX_UNIFORM_RUNTIME,
		APPROX_VERTEX_INVPROP_RUNTIME,
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
		header[Col.APPROX_REP_COUNT.getColIdx()] = "Approx Rep Count";
		header[Col.OPT_OBJECTIVE.getColIdx()] = "Optimal Objective";
		header[Col.APPROX_CYCLE_OBJECTIVE.getColIdx()] = "Approx Cycle Objective";
		header[Col.APPROX_VERTEX_UNIFORM_OBJECTIVE.getColIdx()] = "Approx Vertex [UNIFORM] Objective";
		header[Col.APPROX_VERTEX_INVPROP_OBJECTIVE.getColIdx()] = "Approx Vertex [INVPROP] Objective";
		header[Col.CYCLE_GEN_TIME.getColIdx()] = "Cycle generation runtime";
		header[Col.OPT_RUNTIME.getColIdx()] = "Optimal Runtime";
		header[Col.APPROX_CYCLE_RUNTIME.getColIdx()] = "Approx Cycle Runtime";
		header[Col.APPROX_VERTEX_UNIFORM_RUNTIME.getColIdx()] = "Approx Vertex [UNIFORM] Runtime";
		header[Col.APPROX_VERTEX_INVPROP_RUNTIME.getColIdx()] = "Approx Vertex [INVPROP] Runtime";
		
		return header;
	}
}
