package edu.cmu.cs.dickerson.kpd.io;

import java.io.IOException;

public class IRICOutput extends Output {
	
	public enum Col implements OutputCol { 
		CYCLE_CAP,
		CHAIN_CAP,
		GENERATOR,
		;
		public int getColIdx() { return this.ordinal(); }
	}

	public static String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.CYCLE_CAP.getColIdx()] = "Cycle Cap";
		header[Col.CHAIN_CAP.getColIdx()] = "Chain Cap";
		header[Col.GENERATOR.getColIdx()] = "Generator";
		
		return header;
	}
	
	public IRICOutput(String path) throws IOException {
		super(path, getHeader());
	}

}

