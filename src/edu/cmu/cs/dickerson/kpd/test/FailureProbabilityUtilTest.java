package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
import edu.cmu.cs.dickerson.kpd.structure.generator.factories.AllMatchVertexPairFactory;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class FailureProbabilityUtilTest {

	@Test
	public void testConstantFailureProbs() {

		Pool pool = new Pool(Edge.class);
		int numPairs = 3;
		assertTrue(numPairs >= 0);

		GraphGenerator<Vertex,Edge,Vertex> cgg = new CompleteGraphGenerator<Vertex,Edge>(numPairs);
		cgg.generateGraph(pool, new AllMatchVertexPairFactory(), null);

		// Add some constant edge failure probabilities
		FailureProbabilityUtil.setFailureProbability(pool, FailureProbabilityUtil.ProbabilityDistribution.CONSTANT, new Random());

		for(Edge e : pool.edgeSet()) {
			assertTrue(0.7 == e.getFailureProbability());
		}
	}


	@Test
	public void testCalculateDiscountedChainUtility() {
		Pool pool = new Pool(Edge.class);

		int ID = 0;
		VertexAltruist a1 = new VertexAltruist(ID++, BloodType.O);
		pool.addAltruist(a1);

		List<Vertex> a1Pairs = new ArrayList<Vertex>();
		a1Pairs.add(a1);
		int a1ChainLen = 6;
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
			assertTrue(chain.getWeight() == FailureProbabilityUtil.calculateDiscountedChainUtility(chain, pool, pool.vertexSet()));
		}
		
		Set<Vertex> specialV = new HashSet<Vertex>();
		for(Cycle chain : chains) {
			assertTrue(0.0 == FailureProbabilityUtil.calculateDiscountedChainUtility(chain, pool, specialV));
		}
		
	}
}
