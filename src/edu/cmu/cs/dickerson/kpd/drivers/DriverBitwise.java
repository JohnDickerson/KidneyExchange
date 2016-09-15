package edu.cmu.cs.dickerson.kpd.drivers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.io.BitwiseOutput;
import edu.cmu.cs.dickerson.kpd.io.BitwiseOutput.Col;
import edu.cmu.cs.dickerson.kpd.solver.BitwiseThresholdCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSLoader;
import edu.cmu.cs.dickerson.kpd.structure.real.exception.LoaderException;

public class DriverBitwise {


	public static void main(String args[]) {

		// Meir -- for your debugging pleasure!
		final boolean isMeir = true;
		if(isMeir) {
			writeSaidmanGraphsToCNF();
			return;
		}


		// Solve within fraction of best node vs lower bound (ranges from 0=opt to 1)
		final double relativeMipGap = 0.05;

		// Solve the feasibility IP (true) or the minimization of error IP (false)? 
		final boolean doFeasibilitySolve = true;

		// Are we trying to solve this IP, or just dumping the SAT formulation in CNF to some files?
		final boolean doSATSolveDump = true;

		// Initialize our experimental output to .csv writer
		String path = "unos_bitwise_" + System.currentTimeMillis() + ".csv";
		BitwiseOutput eOut = null;
		try {
			eOut = new BitwiseOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		final int minThreshold = 1;   // set this to 0 for original experiments
		final int maxThreshold = minThreshold;   

		// Want to solve for each match run in a master directory consisting of single match directories
		File baseUNOSDir = new File(IOUtil.getBaseUNOSFilePath());
		List<File> matchDirList = Arrays.asList(baseUNOSDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return file.isDirectory() && !name.toLowerCase().endsWith(".zip");
			}
		}));

		for(File matchDir : matchDirList) {

			UNOSLoader loader = new UNOSLoader(',');

			// We assume a lot about filenames here.  Figure out which .csv files matter
			String matchRunID = "", donorFilePath = "", recipientFilePath = "", edgeFilePath = "";
			File[] csvFiles = matchDir.listFiles(new FilenameFilter() {  @Override public boolean accept(File file, String name) { return name.endsWith(".csv"); } });
			if(null == csvFiles || csvFiles.length < 1) { continue; }

			for(File csvFile : Arrays.asList(csvFiles)) {
				if(csvFile.getName().toUpperCase().contains("DONOR")) {
					donorFilePath = csvFile.getAbsolutePath();
					matchRunID = csvFile.getName().substring(0,8);
				} else if(csvFile.getName().toUpperCase().contains("RECIPIENT")) {
					recipientFilePath = csvFile.getAbsolutePath();
				} else if(csvFile.getName().toUpperCase().contains("EDGEWEIGHTS")) {
					edgeFilePath = csvFile.getAbsolutePath();
				}
			}

			// Make sure we're actually looking at a UNOS match run
			if(donorFilePath.isEmpty() || recipientFilePath.isEmpty() || edgeFilePath.isEmpty() || matchRunID.isEmpty()) {
				IOUtil.dPrintln("Couldn't figure out this directory!");
				System.exit(-1);
			}

			// Load the pool from donor, recipient, edge files
			Pool pool = null;
			try {
				pool = loader.loadFromFile(donorFilePath, recipientFilePath, edgeFilePath);
			} catch(LoaderException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			Integer numPairs = pool.getNumPairs();
			Integer numAlts = pool.getNumAltruists();
			double highlySensitizedThresh = 0.8;   // UNOS data is explicitly marked as highly or not highly sensitized   

			// Split pairs into highly- and not highly-sensitized patients 
			Set<Vertex> highV = new HashSet<Vertex>();
			for(VertexPair pair : pool.getPairs()) {
				if(pair.getPatientCPRA() >= highlySensitizedThresh) {
					highV.add(pair);
				}
			}

			// TODO
			// Remove once we make the IP faster!
			if(pool.vertexSet().size() > 150) { continue; }

			// Dump adjacency matrix to CP file
			//pool.writeUNOSGraphToDenseAdjacencyMatrix("unos"+matchRunID+".graph");
			//pool.writeUNOSGraphToDZN("unos"+matchRunID+".dzn", 10, 0);
			//if(1+1==2) continue;

			for(int k=2; k<=40; k++) {//pool.vertexSet().size(); k++) {
				//int k=25; {

				for(int threshold=minThreshold; threshold<=maxThreshold; threshold++) {
					if(doSATSolveDump) {
						//pool.writeUNOSGraphToBitwiseCNF(k, "unos"+matchRunID+"_v"+(numPairs+numAlts)+"_e"+pool.getNumNonDummyEdges()+"_k"+String.format("%04d",k)+".cnf");
						pool.writeUNOSGraphToBitwiseCNF(k, threshold, "unos"+matchRunID+"_v"+(numPairs+numAlts)+"_e"+pool.getNumNonDummyEdges()+"_k"+String.format("%04d",k)+".cnf");
						System.exit(-1);
						continue;  // don't do IP solve
					}

					IOUtil.dPrintln("Solving for n="+(numPairs+numAlts)+", |E|="+pool.getNumNonDummyEdges()+", k="+k+", t="+threshold+", feasibility="+doFeasibilitySolve+" ...");

					eOut.set(Col.NUM_PAIRS, numPairs);
					eOut.set(Col.NUM_ALTS, numAlts);
					eOut.set(Col.NUM_EDGES, pool.getNumNonDummyEdges());
					eOut.set(Col.HIGHLY_SENSITIZED_CPRA, highlySensitizedThresh);
					eOut.set(Col.GENERATOR, matchRunID);
					eOut.set(Col.kBITLENGTH, k);
					eOut.set(Col.THRESHOLD, threshold);
					eOut.set(Col.HIGHLY_SENSITIZED_COUNT, highV.size());
					eOut.set(Col.RELATIVE_MIP_GAP, relativeMipGap);
					eOut.set(Col.IS_FEASIBILITY_SOLVE, doFeasibilitySolve ? 1 : 0);
					// Solve the model -- can we k-induce this graph by threshold t?
					try {

						// The value of the solution will be how far from k-inducible this
						// graph was, e.g., value = 0 is perfect, value = 1 is off by 1, etc.
						BitwiseThresholdCPLEXSolver s = new BitwiseThresholdCPLEXSolver(pool, k, threshold);
						s.setRelativeMipGap(relativeMipGap);
						Solution sol = s.solve(doFeasibilitySolve);

						boolean isInducible = sol.isLegalMatching();
						assert isInducible == (sol.getObjectiveValue() == 0.0);
						eOut.set(Col.kINDUCIBLE, isInducible ? 1 : 0);
						eOut.set(Col.kINDUCIBLE_ERROR, sol.getObjectiveValue());

					} catch(SolverException e) {
						e.printStackTrace();
						eOut.set(Col.kINDUCIBLE, 0);
						eOut.set(Col.kINDUCIBLE_ERROR, -1);
					}

					// Write the experimental row of data
					try {
						eOut.record();
					} catch(IOException e) {
						IOUtil.dPrintln("Had trouble writing experimental output to file.  We assume this kills everything; quitting.");
						e.printStackTrace();
						return;
					}
				} // end of threshold in {0, 1, ..., k}
			} // end of k in kList
		} // matchDir & matchDirList

		// Flush and kill the CSV writer
		if(null != eOut) {
			try {
				eOut.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		IOUtil.dPrintln("All done with UNOS runs!");

		return;
	}




	/**
	 * For Meir!
	 */
	public static void writeSaidmanGraphsToCNF() {

		final int minThreshold = 1;             // minimum value of t
		final int maxThreshold = minThreshold;  // maximum value of t
		final int minK = 1;                     // minimum value of k
		final int maxK = 40;                    // maximum value of k
		final int numGraphs = 1;                // how many different graphs should we generate?
		final int numPairs = 100;               // how many patient-donor pairs should be generated (per graph)?
		final int numAlts = 0;                  // how many altruist donors should be generated (per graph)?

		// This generates random graphs; if you want to generate the same set of graphs
		// over and over again, pass in a Random object with the same seed (like I'm 
		// doing now, with seed = 12345L)
		PoolGenerator gen = new SaidmanPoolGenerator(new Random(12345L));

		// These for loops will: generate n graphs, and for each generated graph, for
		// each k \in [minK, maxK], for each t \in [minT, maxT], output a CNF SAT file
		for(int n=0; n<numGraphs; n++) {

			// Randomly generate a directed graph with numPairs pairs and numAlts altruists
			Pool pool = gen.generate(numPairs, numAlts);

			for(int k=minK; k<=maxK; k++) {
				for(int threshold=minThreshold; threshold<=maxThreshold; threshold++) {
					
					// Dump the CNF formula to a .cnf file; feel free to change this
					String outFilename = "saidman_"+n+"_v"+(numPairs+numAlts)+"_e"+pool.getNumNonDummyEdges()+"_k"+String.format("%04d",k)+"_t"+String.format("%04d",threshold)+".cnf";
					IOUtil.dPrintln("n="+n+"\tk="+k+"\tt="+threshold+"\t\tout="+outFilename);
					
					// Trigger a stack overflow ;)
					pool.writeUNOSGraphToBitwiseCNF(k, threshold, outFilename);
				}
			}
		}
		
		IOUtil.dPrintln("All done with Meir's Saidman runs!");
		return;
	}
}
