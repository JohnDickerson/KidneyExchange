package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSGenerator;

public class UNOSGeneratorTest {

	// These tests will fail unless you have local access to actual UNOS data;
	// set this variable to false if you don't and the tests will pass
	public static boolean HAVE_ACCESS_TO_UNOS_DATA = true;

	// TODO flesh this test out
	
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

		int initialSize = 1000;
		int initialAddition = 500;
		
		// Generate a sample pool with some pairs or altruists
		Pool pool = gen.generatePool(initialSize);
		
		// Add a few vertices
		gen.addVertices(pool, initialAddition);

		// Make sure we've generated something sane
		assertEquals("Raw vertex count check", initialSize+initialAddition, pool.getNumPairs() + pool.getNumAltruists());
		System.out.println("#Altruists: " + pool.getNumAltruists());
		System.out.println("#Pairs:     " + pool.getNumPairs());
	
		// Do some simple checks on edges for the altruists
		for(Vertex alt : pool.getAltruists()) {
			for(Vertex pair : pool.getPairs()) {
				Edge e = pool.getEdge(pair, alt);
				if(null == e) {
					fail("No edge from pair " + pair + " to altruist " + alt);
				}
				if(pool.getEdgeWeight(e) != 0) {
					fail("Dummy edge from pair " + pair + " to altruist " + alt + " had nonzero weight");
				}
			}
		}
		// Do some simple checks on edges for verts
		for(Edge e : pool.edgeSet()) {
			if(pool.getEdgeWeight(e) != 0) {
				assertTrue("Non-dummy edge check (source is vert or alt, sink is vert)", 
						!pool.getEdgeTarget(e).isAltruist());
			} else {
				assertTrue("Dummy edge check (source is vert, sink is alt)", 
						!pool.getEdgeSource(e).isAltruist() && pool.getEdgeTarget(e).isAltruist());
			}
		}
		
	}

}
