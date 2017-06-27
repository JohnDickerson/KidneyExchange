package edu.cmu.cs.dickerson.kpd.Ethics;

import java.io.*;
import java.util.*;

/*
 * Static methods to clean and interpret output printed to console
 */

public class OutputCleaner {

	// Reads console output from txt file, writes pool size data to csv 
	public static void poolSize(String filename) throws IOException {
		
		PrintWriter out = new PrintWriter("pool_size_" + System.currentTimeMillis() + ".csv");
		
		out.println("iteration,size,run,type");
		BufferedReader in = new BufferedReader(new FileReader(filename));
		try {
		    String line = in.readLine();
		    while (line != null) {
		    	if (line.startsWith("Iteration: ")) {
		    		String[] data = line.split(" ");
		    		String formattedString = data[1] + "," + data[3] + "," + data[5] + "," + data[7];
		    		System.out.println(formattedString);  
		    		out.println(formattedString);
		    	}
		    	line = in.readLine();
		    }
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		    in.close();
		    out.close();
		}
	}
	
	public static void main(String[] args) {
		String filename = "/Users/Rachel/Documents/Summer 2017/Research/Summer Work/Simulation/Output/output_130_iters.txt";
		try {
			poolSize(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
