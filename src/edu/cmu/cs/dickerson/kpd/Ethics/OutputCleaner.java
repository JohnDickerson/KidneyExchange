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
		
		out.println("iteration,size,run,weights_version");
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
	
	// Reads console output from txt file, writes pool size data to csv 
	public static void profileGeneration(String filename) throws IOException {
		
		BufferedReader in = new BufferedReader(new FileReader(filename));
		try {
		    String line = in.readLine();
		    int i = 1;
		    while (line != null) {
		    	if (line.startsWith("temp: ")){
		    		String[] split = line.split(" ");
		    		System.out.println("vertex "+i+"\t"+split[5]+"\t"+split[1]+"\t"+split[2]+"\t"+split[3]);
		    		i++;
		    	}
		    	if (line.startsWith("Testing weights ")){
		    		System.out.println("\n\nWEIGHTS VERSION "+line.split(" ")[3]);
		    		i = 1;
		    	}
		    	line = in.readLine();
		    }
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		    in.close();
		}
	}
	
	public static void main(String[] args) {
		String filename = "/Users/Rachel/Documents/Summer 2017/Research/Summer Work/Simulation/Output/vertex_types2.txt";
		try {
			profileGeneration(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
