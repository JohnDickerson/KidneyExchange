package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jgrapht.generate.CompleteGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.junit.Test;

import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexAltruist;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.factories.AllMatchVertexPairFactory;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class CycleGeneratorTest {

	@Test
	public void testFailureProbabilities() {
		Pool pool = new Pool(Edge.class);
		
		int ID = 0;
		VertexAltruist a1 = new VertexAltruist(ID++, BloodType.O);
		pool.addAltruist(a1);
		
		List<Vertex> a1Pairs = new ArrayList<Vertex>();
		a1Pairs.add(a1);
		int a1ChainLen = 4;
		for(int a1idx=1; a1idx<=a1ChainLen; a1idx++) {
			VertexPair vp = new VertexPair(ID++, BloodType.O, BloodType.O, false, 0.0, false);
			a1Pairs.add(vp);
			pool.addPair(vp);
			
			// Add an edge from the previous vertex to this vertex
			Edge e = pool.addEdge(a1Pairs.get(a1idx-1), a1Pairs.get(a1idx));
			pool.setEdgeWeight(e, 1.0);
			
			// Add a dummy edge back to the altruistic vertex
			Edge dummy = pool.addEdge(a1Pairs.get(a1idx), a1Pairs.get(0));
			pool.setEdgeWeight(dummy, 0.0);
		}
		
		// Add some constant edge failure probabilities
		FailureProbabilityUtil.setFailureProbability(pool, FailureProbabilityUtil.ProbabilityDistribution.CONSTANT, new Random());
		
		// Generate chains along with their discounted utilities
		CycleGenerator cg = new CycleGenerator(pool);
		
		List<Cycle> chains = cg.generateCyclesAndChains(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
		
		for(Cycle chain : chains) {
			assertTrue(Cycle.isAChain(chain, pool));
			if(chain.getEdges().size() == 2) {
				assertTrue(isWeightRoughly(chain, 0.3));
			} else if(chain.getEdges().size() == 3) {
				assertTrue(isWeightRoughly(chain, 0.3*(0.7) + 2*0.3*0.3));
			} else if(chain.getEdges().size() == 4) {
				assertTrue(isWeightRoughly(chain, 0.3*(0.7) + 2*0.3*0.3*(0.7) + 3*0.3*0.3*0.3));
			}
		}
		
		// TODO expand this to cycles+chains, also to weighted edges
	}
	
	private static boolean isWeightRoughly(Cycle cycle, double weight) {
		return (weight - 0.001 < cycle.getWeight() && weight + 0.001 > cycle.getWeight());
	}
	
	@Test
	public void testEdgeCases() {
		
		Pool emptyPool = new Pool(Edge.class);
		CycleGenerator cg1 = new CycleGenerator(emptyPool);
		assertEquals(0, cg1.generateCyclesAndChains(Integer.MAX_VALUE, Integer.MAX_VALUE).size());
		
		PoolGenerator pg = new SaidmanPoolGenerator(new Random());
		Pool saidmanPool = pg.generate(25, 5);
		CycleGenerator cg2 = new CycleGenerator(saidmanPool);
		assertEquals(0, cg2.generateCyclesAndChains(0, 0).size());
		assertEquals(0, cg2.generateCyclesAndChains(1, 0).size());
		
	}
	
	@Test
	public void testOnlyCyclesComplete() {
	
		Pool pool = new Pool(Edge.class);
		int numPairs = 10;
		assertTrue(numPairs >= 4);
		
		GraphGenerator<Vertex,Edge,Vertex> cgg = new CompleteGraphGenerator<Vertex,Edge>(numPairs);
		cgg.generateGraph(pool, new AllMatchVertexPairFactory(), null);
		
		// Number of edges in complete directed graph: n(n-1)
		assertEquals(numPairs*(numPairs-1), pool.edgeSet().size());
		
		CycleGenerator cg = new CycleGenerator(pool);
		
		// Number of 2-cycles = n(n-1)/2  (#edges in undirected complete graph)
		assertEquals(numPairs*(numPairs-1)/2, cg.generateCyclesAndChains(2, Integer.MAX_VALUE).size());
		
		// Number of at-most-3-cyles = number of 2-cycles + number of 3-cycles, so
		// n(n-1)/2   +  n(n-1)(n-2)/3
		assertEquals(numPairs*(numPairs-1)/2 + numPairs*(numPairs-1)*(numPairs-2)/3, cg.generateCyclesAndChains(3, Integer.MAX_VALUE).size());
		
		// ... + n(n-1)(n-2)(n-3)/4
		assertEquals(numPairs*(numPairs-1)/2 + numPairs*(numPairs-1)*(numPairs-2)/3 + numPairs*(numPairs-1)*(numPairs-2)*(numPairs-3)/4, cg.generateCyclesAndChains(4, Integer.MAX_VALUE).size());
	}

	@Test
	public void testOnlyChains() {
		
		// Make a graph consisting of two altruists and no non-chain cycles
		Pool pool = new Pool(Edge.class);
		
		int ID = 0;
		VertexAltruist a1 = new VertexAltruist(ID++, BloodType.O);
		VertexAltruist a2 = new VertexAltruist(ID++, BloodType.O);
		pool.addAltruist(a1);
		pool.addAltruist(a2);
		
		CycleGenerator cg = new CycleGenerator(pool);
		assertEquals(0, cg.generateCyclesAndChains(Integer.MAX_VALUE, Integer.MAX_VALUE).size());
		
		List<Vertex> a1Pairs = new ArrayList<Vertex>();
		a1Pairs.add(a1);
		int a1ChainLen = 15;
		for(int a1idx=1; a1idx<=a1ChainLen; a1idx++) {
			VertexPair vp = new VertexPair(ID++, BloodType.O, BloodType.O, false, 0.0, false);
			a1Pairs.add(vp);
			pool.addPair(vp);
			
			// Add an edge from the previous vertex to this vertex
			Edge e = pool.addEdge(a1Pairs.get(a1idx-1), a1Pairs.get(a1idx));
			pool.setEdgeWeight(e, 1.0);
			
			// Add a dummy edge back to the altruistic vertex
			Edge dummy = pool.addEdge(a1Pairs.get(a1idx), a1Pairs.get(0));
			pool.setEdgeWeight(dummy, 0.0);
		}
		
		assertEquals(a1ChainLen, cg.generateCyclesAndChains(Integer.MAX_VALUE, Integer.MAX_VALUE).size());
		assertEquals(a1ChainLen, cg.generateCyclesAndChains(Integer.MAX_VALUE, a1ChainLen).size());
		
		// Add a second altruist chain
		List<Vertex> a2Pairs = new ArrayList<Vertex>();
		a2Pairs.add(a2);
		int a2ChainLen = 10;
		for(int a2idx=1; a2idx<=a2ChainLen; a2idx++) {
			VertexPair vp = new VertexPair(ID++, BloodType.O, BloodType.O, false, 0.0, false);
			a2Pairs.add(vp);
			pool.addPair(vp);
			pool.setEdgeWeight(pool.addEdge(a2Pairs.get(a2idx-1), a2Pairs.get(a2idx)), 1.0);
			pool.setEdgeWeight(pool.addEdge(a2Pairs.get(a2idx), a2Pairs.get(0)), 0.0);
		}
		
		assertEquals(a1ChainLen+a2ChainLen, cg.generateCyclesAndChains(Integer.MAX_VALUE, Integer.MAX_VALUE).size());
		
		if(a2ChainLen > 1) {
			// Add some alternate paths and forks to the second altruist chain
			// This should add (a2ChainLen - 1) additional paths
			pool.setEdgeWeight(pool.addEdge(a2Pairs.get(0), a2Pairs.get(2)), 1.0);
			assertEquals(a1ChainLen+a2ChainLen+a2ChainLen-1, 
					cg.generateCyclesAndChains(Integer.MAX_VALUE, Integer.MAX_VALUE).size());
		}
		
	}
}
