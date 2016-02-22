package edu.cmu.cs.dickerson.kpd.drivers;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSLoader;
import edu.cmu.cs.dickerson.kpd.structure.real.exception.LoaderException;

/**
 * 
 * Reads each UNOS input from raw file, then writes it out to a (.vert, .edge)
 * tuple of files that my Python viz code can read.
 * 
 * @author John P Dickerson
 *
 */
public class DriverViz {

	public static void main(String args[]) {
	

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

			// Load the pool from donor, recipient, edge files -- force unit weights
			Pool pool = null;
			try {
				pool = loader.loadFromFile(donorFilePath, recipientFilePath, edgeFilePath, true);
			} catch(LoaderException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			// Write this as an input file to our solver
			pool.writeToUNOSKPDFile("unos_"+matchRunID, false);
			
			// Write this out to a NetworkX-friendly set of files
			//pool.writeToVizFile("unos"+matchRunID);
		}
		return;
	} // end of main
}
