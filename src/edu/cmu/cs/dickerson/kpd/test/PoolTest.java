package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.generate.CompleteGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.junit.Test;

import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexAltruist;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.UNOSGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.factories.AllMatchVertexPairFactory;

public class PoolTest {

	// These tests will fail unless you have local access to actual UNOS data;
	// set this variable to false if you don't and the tests will pass
	public static boolean HAVE_ACCESS_TO_UNOS_DATA = false;

	@Test
	public void testSubPool() {
		
		// Make a complete Parent graph
		Pool pool = new Pool(Edge.class);
		int numPairs = 10;
		assertTrue(numPairs >= 4);
		
		GraphGenerator<Vertex,Edge,Vertex> cgg = new CompleteGraphGenerator<Vertex,Edge>(numPairs);
		cgg.generateGraph(pool, new AllMatchVertexPairFactory(), null);
		assertEquals(numPairs*(numPairs-1), pool.edgeSet().size());
		
		
		// Take some subset of 50% of the vertices (should still be a complete graph)
		Set<Vertex> subsetV = new HashSet<Vertex>();
		for(Vertex v : pool.vertexSet()) {
			subsetV.add(v);
			if(subsetV.size()>=numPairs/2) { break; }  // break once we have 50%ish of the vertices
		}
		Pool subPool = pool.makeSubPool(subsetV);
		assertEquals(numPairs/2, subPool.vertexSet().size());
		assertEquals((numPairs/2)*((numPairs/2)-1), subPool.edgeSet().size());
				
		// Number of 2-cycles = n(n-1)/2  (#edges in undirected complete graph)
		CycleGenerator cg = new CycleGenerator(subPool);
		assertEquals(numPairs/2*((numPairs/2)-1)/2, cg.generateCyclesAndChains(2, Integer.MAX_VALUE).size());
		
		for(Vertex v : subPool.vertexSet()) {
			assertTrue(pool.vertexSet().contains(v));
		}
	}
	
	@Test
	public void testRemoveVertex() {
		PoolGenerator poolGen = new SaidmanPoolGenerator(new Random());
		Pool p = poolGen.generate(3, 2);
		assertEquals(p.getAltruists().size(), 2);
		assertEquals(p.getPairs().size(), 3);
		assertEquals(p.vertexSet().size(), 5);
		
		SortedSet<VertexAltruist> alts = new TreeSet<VertexAltruist>(p.getAltruists());
		int removedAlts = 0;
		for(VertexAltruist alt : alts) {
			p.removeVertex(alt); removedAlts++;
			assertEquals(p.getAltruists().size(), 2-removedAlts);
			assertEquals(p.getPairs().size(), 3);
			assertEquals(p.vertexSet().size(), 5-removedAlts);
		}
		
		SortedSet<VertexPair> pairs = new TreeSet<VertexPair>(p.getPairs());
		int removedPairs = 0;
		for(VertexPair pair : pairs) {
			p.removeVertex(pair); removedPairs++;
			assertEquals(p.getAltruists().size(), 2-removedAlts);
			assertEquals(p.getPairs().size(), 3-removedPairs);
			assertEquals(p.vertexSet().size(), 5-removedAlts-removedPairs);
		}
		
		assertEquals(p.getNumPairs(),0);
		assertEquals(p.getNumAltruists(),0);
	}
	
	@Test
	public void testKTClusterRecursion() {
		int k=5;
		int t=3; 
		Pool p = new Pool(Edge.class);
		System.out.println( p.getKTClusters(2, 1).size() );
	}
	
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
