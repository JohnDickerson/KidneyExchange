package edu.cmu.cs.dickerson.kpd.Ethics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TestVertexGeneration {
	
	private static void runSimulation(int[] versions) {
		ArrayList<Integer> v = new ArrayList<Integer>();
		for (int i : versions) {
			v.add(i);
		}
		EthicalDriver.runSimulationWithWeights(v);
	}
	
	private static void compareVertices(int[] versions) throws IOException {
		
		String filename1 = "/Users/Rachel/KidneyExchange/vertices_"+versions[0]+".csv";
		BufferedReader in1 = new BufferedReader(new FileReader(filename1));
		String filename2 = "/Users/Rachel/KidneyExchange/vertices_"+versions[1]+".csv";
		BufferedReader in2 = new BufferedReader(new FileReader(filename2));
		
		//Read and compare line-by-line
		String line1 = in1.readLine();
		String line2 = in2.readLine();
		
		while ((line1 != null) && (line2 != null)) {
			if (!(line1.equals(line2))) {
				String[] data1 = line1.split(",");
				String[] data2 = line2.split(",");
				System.out.println("Difference on vertex "+data1[0]+":");
				System.out.println("\t\t\ttype\trAge\t\t\trAlcohol\t\trHealth");
				System.out.println("\tVersion "+ versions[0]+":\t"+data1[1]+"\t"+data1[2]+"\t"+data1[3]+"\t"+data1[4]);
				System.out.println("\tVersion "+ versions[1]+":\t"+data2[1]+"\t"+data2[2]+"\t"+data2[3]+"\t"+data2[4]);
				break;
			}
			//If they are equal, continue.
			line1 = in1.readLine();
			line2 = in2.readLine();
			
			//If reach the end, there's no difference
			if ((line1 == null) || (line2 == null)) {
				System.out.println("No differences found.");
			}
		}
				
		in1.close();
		in2.close();
		
	}
	
	public static void main(String[] args) throws IOException {
		
		//Must only test two versions at a time (or adapt compareVertices method above)
		int[] versionsToTest = {4, 5};
		
		//Run Ethical Driver
		runSimulation(versionsToTest);
		
		//Compare resulting vertices_x.csv until find first line of difference
		compareVertices(versionsToTest);
	}
}
