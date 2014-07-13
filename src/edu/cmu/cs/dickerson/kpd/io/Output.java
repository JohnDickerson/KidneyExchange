package edu.cmu.cs.dickerson.kpd.io;

import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

public abstract class Output {

	// Output file location
	private String path;

	// Current row (created incrementally)
	private String[] row; 
	
	private CSVWriter writer;

	public Output(String path, String[] header) throws IOException {
		this.path = path;
		init(header);
	}

	private  void init(String[] header) throws IOException {

		// Open a .csv file, write the header to the file
		writer = new CSVWriter(new FileWriter(path), ',', CSVWriter.NO_QUOTE_CHARACTER);
		writer.writeNext(header);
		writer.flush();

		// Subsequent data rows must be same length as header
		row = new String[header.length];
	}
	
	public void set(OutputCol column, Object o) {
		row[column.getColIdx()] = String.valueOf(o.toString());
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
