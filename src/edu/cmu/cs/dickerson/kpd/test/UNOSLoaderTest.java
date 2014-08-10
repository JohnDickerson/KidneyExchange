package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSLoader;
import edu.cmu.cs.dickerson.kpd.structure.real.exception.LoaderException;

public class UNOSLoaderTest {

	// These tests will fail unless you have local access to actual UNOS data;
	// set this variable to false if you don't and the tests will pass
	public static boolean HAVE_ACCESS_TO_UNOS_DATA = true;

	@Test
	public void test() {

		if(!HAVE_ACCESS_TO_UNOS_DATA) { 
			assertTrue("Skipping UNOS data tests.", true);
			return;
		}

		UNOSLoader loader = new UNOSLoader(',');

		// Change this to reflect base loading path for UNOS files
		String basePath = IOUtil.getBaseUNOSFilePath() + "/KPD_CSV_IO_091012/";
		String donorFilePath = basePath + "20120910_donor.csv";
		String recipientFilePath = basePath + "20120910_recipient.csv";
		String edgeFilePath = basePath + "31_edgeweights.csv";

		
		Pool pool = null;
		try {
			pool = loader.loadFromFile(donorFilePath, recipientFilePath, edgeFilePath);
		} catch(LoaderException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}

		// Check vertex sizes, edge counts, etc
		assertTrue(184 == pool.getNumPairs());
		assertTrue(7 == pool.getNumAltruists());
		
		//pool.writeToVizFile("unos20120910");
	}

}
