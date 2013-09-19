package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.*;

import java.util.Random;

import org.jgrapht.generate.CompleteGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.junit.Test;

import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.generator.factories.AllMatchVertexPairFactory;

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
			assert(0.7 == e.getFailureProbability());
		}
	}

}
