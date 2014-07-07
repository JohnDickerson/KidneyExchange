package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import edu.cmu.cs.dickerson.kpd.fairness.solver.FairnessCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.generator.UNOSGenerator;

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

		int initialSize = 950;
		int initialAddition = 50;

		// Generate a sample pool with some pairs or altruists
		Pool pool = gen.generatePool(initialSize);

		// Add a few vertices
		gen.addVerticesToPool(pool, initialAddition);

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

		// Run a quick sanity check of edge distributions (MAY NOT ALWAYS BE TRUE)
		double inDegSumHigh = 0;
		int inDegCtHigh = 0;
		double inDegSumLow = 0;
		int inDegCtLow = 0;
		for(Vertex v : pool.getPairs()) {
			int inDeg = pool.incomingEdgesOf(v).size();
			// Track CPRA high and CPRA low stats differently
			if(v.getUnderlyingPair().getRecipient().cpra >= 0.8) {
				assertTrue("CPRA >= 80 --> UNOS highly-sensitized", v.getUnderlyingPair().getRecipient().highlySensitized);
				inDegSumHigh += inDeg;
				inDegCtHigh++;
			} else {
				assertFalse("CPRA < 80 --> UNOS lowly-sensitized", v.getUnderlyingPair().getRecipient().highlySensitized);
				inDegSumLow += inDeg;
				inDegCtLow++;
			}
		}
		double inDegAvgHigh = -1.0;
		if(inDegCtHigh > 0) { inDegAvgHigh = inDegSumHigh / (double) inDegCtHigh; }
		double inDegAvgLow = -1.0;
		if(inDegCtLow > 0) { inDegAvgLow = inDegSumLow / (double) inDegCtLow; }

		System.out.println("Total edge count: " + (inDegSumHigh+inDegSumLow));
		System.out.println("Avg in-degree highly-sensitized: " + inDegAvgHigh);
		System.out.println("Avg in-degree lowly-sensitized: " + inDegAvgLow);
		assertTrue("Probabilistic check: in-deg of highly-sensitized <= lowly-sensitized", inDegAvgHigh <= inDegAvgLow);

		// Make sure we can solve the pool
		boolean doSolve=false;
		if(doSolve) {
			CycleGenerator cg = new CycleGenerator(pool);
			System.out.println("Generating cycles and chains ...");
			List<Cycle> cycles = cg.generateCyclesAndChains(3, 4);
			CycleMembership membership = new CycleMembership(pool, cycles);
			
			FairnessCPLEXSolver s = new FairnessCPLEXSolver(pool, cycles, membership, new HashSet<Vertex>());
			try {
				System.out.println("Solving ...");
				Solution sol = s.solve(0.0);
				System.out.println("Solution size: " + sol.getObjectiveValue());
			} catch(SolverException e) {
				e.printStackTrace();
				fail("Solver exception.");
			}
		}

	}

}
