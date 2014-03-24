package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import edu.cmu.cs.dickerson.kpd.solver.approx.VertexRandomWalkPacker;
import edu.cmu.cs.dickerson.kpd.solver.approx.VertexShufflePacker.ShuffleType;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexAltruist;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class VertexRandomWalkPackerTest {

	private static boolean isWeightRoughly(Cycle cycle, double weight) {
		return (weight - 0.001 < cycle.getWeight() && weight + 0.001 > cycle.getWeight());
	}

	@Test
	public void test() {

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

		FailureProbabilityUtil.setFailureProbability(pool, FailureProbabilityUtil.ProbabilityDistribution.CONSTANT, new Random(), 0.7);

		int maxChainSize = Integer.MAX_VALUE;
		boolean usingFailureProbabilities = true;

		CycleGenerator cg = new CycleGenerator(pool);
		List<Cycle> cycles = cg.generateCyclesAndChains(3, maxChainSize, usingFailureProbabilities);
		CycleMembership membership = new CycleMembership(pool, cycles);

		VertexRandomWalkPacker packer = new VertexRandomWalkPacker(pool, cycles, membership, ShuffleType.UNIFORM_RANDOM, maxChainSize, usingFailureProbabilities);

		// Add some constant edge failure probabilities
		for(int testIdx=0; testIdx<10; testIdx++) {

			Solution sol = packer.pack(Double.MAX_VALUE);
			if(sol.getMatching().size() != 1) {
				fail("Solution size should be exactly 1 chain (was " + sol.getMatching().size() + " cycles or chains.");
			}

			Cycle chain = sol.getMatching().iterator().next();
			
			if(chain.getEdges().size() == 2) {
				assertTrue("Chain weight (" + chain.getWeight() + ") =? 0.3", isWeightRoughly(chain, 0.3));
			} else if(chain.getEdges().size() == 3) {
				assertTrue("Chain weight (" + chain.getWeight() + ") =? 0.39", isWeightRoughly(chain, 0.3*(0.7) + 2*0.3*0.3));
			} else if(chain.getEdges().size() == 4) {
				assertTrue("Chain weight (" + chain.getWeight() + ") =? 0.417", isWeightRoughly(chain, 0.3*(0.7) + 2*0.3*0.3*(0.7) + 3*0.3*0.3*0.3));
			}
		}

	}

}
