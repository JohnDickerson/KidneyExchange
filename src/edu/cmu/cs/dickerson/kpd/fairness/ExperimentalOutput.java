package edu.cmu.cs.dickerson.kpd.fairness;

import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

public class ExperimentalOutput {

	// Output file location
	private String path;

	// Current row (created incremenetally)
	private String[] row; 
	
	private CSVWriter writer;

	public enum Col { START_TIME, 
		RANDOM_SEED, 
		NUM_PAIRS, 
		NUM_ALTS, 
		CYCLE_CAP, 
		CHAIN_CAP, 
		GENERATOR, 
		HIGHLY_SENSITIZED_CPRA, 
		HIGHLY_SENSITIZED_COUNT, 
		ALPHA_STAR, 
		FAIR_OBJECTIVE, 
		UNFAIR_OBJECTIVE,
		FAIR_HIGHLY_SENSITIZED_MATCHED, 
		UNFAIR_HIGHLY_SENSITIZED_MATCHED,
		};

	public ExperimentalOutput(String path) throws IOException {
		this.path = path;
		init();
	}

	private  void init() throws IOException {

		// Open a .csv file, write the header to the file
		writer = new CSVWriter(new FileWriter(path), ',');
		writer.writeNext(getHeader());
		writer.flush();

		// Subsequent data rows must be same length as header
		row = new String[getHeader().length];
	}

	public String[] getHeader() {
		String[] header = new String[Col.values().length];
		header[Col.START_TIME.ordinal()] = "Start Time";
		header[Col.RANDOM_SEED.ordinal()] = "Random Seed";
		header[Col.NUM_PAIRS.ordinal()] = "Num Pairs";
		header[Col.NUM_ALTS.ordinal()] = "Num Alts";
		header[Col.CYCLE_CAP.ordinal()] = "Cycle Cap";
		header[Col.CHAIN_CAP.ordinal()] = "Chain Cap";
		header[Col.GENERATOR.ordinal()] = "Generator";
		header[Col.HIGHLY_SENSITIZED_CPRA.ordinal()] = "Highly-Sensitized CPRA";
		header[Col.HIGHLY_SENSITIZED_COUNT.ordinal()] = "Highly-Sensitized Count";
		header[Col.ALPHA_STAR.ordinal()] = "Alpha*";
		header[Col.FAIR_OBJECTIVE.ordinal()] = "Fair Objective";
		header[Col.UNFAIR_OBJECTIVE.ordinal()] = "Unfair Objective";
		header[Col.FAIR_HIGHLY_SENSITIZED_MATCHED.ordinal()] = "Fair Highly-Sensitized Matched";
		header[Col.UNFAIR_HIGHLY_SENSITIZED_MATCHED.ordinal()] = "Unfair Highly-Sensitized Matched";
		return header;
	}

	public void set(Col column, Object o) {
		row[column.ordinal()] = String.valueOf(o.toString());
	}

	public void record() throws IOException {

		// Record current experimental run to file, flush in case we error out somewhere later
		writer.writeNext(row);
		writer.flush();

		// Clear the row so next write doesn't have to guarantee overriding 
		for(int cellIdx=0; cellIdx<row.length; cellIdx++) {
			row[cellIdx] = "";
		}
	}

	public void close() throws IOException {
		if(null != writer) {
			writer.flush();
			writer.close();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}
}
