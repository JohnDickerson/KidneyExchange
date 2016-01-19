package edu.cmu.cs.dickerson.kpd.drivers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSLoader;
import edu.cmu.cs.dickerson.kpd.structure.real.exception.LoaderException;

public class DriverBitwise {


	public static void main(String args[]) {

		// Initialize our experimental output to .csv writer
		String path = "unos_bitwise_" + System.currentTimeMillis() + ".csv";
		BitwiseOutput eOut = null;
		try {
			eOut = new BitwiseOutput(path);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

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

			for(int k=1; k<=pool.vertexSet().size(); k++) {
				for(int threshold=0; threshold<=k; threshold++) {
					eOut.set(Col.NUM_PAIRS, numPairs);
					eOut.set(Col.NUM_ALTS, numAlts);
					eOut.set(Col.HIGHLY_SENSITIZED_CPRA, highlySensitizedThresh);
					eOut.set(Col.GENERATOR, matchRunID);
					eOut.set(Col.kBITLENGTH, k);
					eOut.set(Col.THRESHOLD, threshold);
					eOut.set(Col.HIGHLY_SENSITIZED_COUNT, highV.size());

					// Solve the model -- can we k-induce this graph by threshold t?
					try {

						// The value of the solution will be how far from k-inducible this
						// graph was, e.g., value = 0 is perfect, value = 1 is off by 1, etc.
						BitwiseThresholdCPLEXSolver s = new BitwiseThresholdCPLEXSolver(pool, k, threshold);
						Solution sol = s.solve();
						
						boolean isInducible = sol.isLegalMatching();
						assert isInducible == (sol.getObjectiveValue() == 0.0);
						eOut.set(Col.kINDUCIBLE, isInducible ? 1 : 0);
						eOut.set(Col.kINDUCIBLE_ERROR, sol.getObjectiveValue());
						
					} catch(SolverException e) {
						e.printStackTrace();
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
}
