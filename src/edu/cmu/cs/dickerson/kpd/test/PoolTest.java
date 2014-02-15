package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSGenerator;

public class PoolTest {

	// These tests will fail unless you have local access to actual UNOS data;
	// set this variable to false if you don't and the tests will pass
	public static boolean HAVE_ACCESS_TO_UNOS_DATA = true;

	@Test
	public void test() {
		
		if(!HAVE_ACCESS_TO_UNOS_DATA) { 
			assertTrue("Skipping UNOS data tests.", true);
			return;
		}

		// Change this to reflect base loading path for UNOS files
		String basePath = "/Users/spook/amem/kpd/files_real_runs/zips";
		long seed = 12345;

		// Load in data from all runs that are unzipped
		UNOSGenerator gen = UNOSGenerator.makeAndInitialize(basePath, ',', new Random(seed));

		int initialSize = 200;

		// Generate a sample pool with some pairs or altruists
		Pool pool = gen.generatePool(initialSize);
		
		// Write the pool to a file
		pool.writeToUNOSKPDFile("test-generated-output");
		
		return;
	}

}
