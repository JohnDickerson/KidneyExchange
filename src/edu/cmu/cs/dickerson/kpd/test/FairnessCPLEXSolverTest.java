package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import edu.cmu.cs.dickerson.kpd.fairness.solver.FairnessCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;


public class FairnessCPLEXSolverTest {

	@Test
	public void test() {

		Pool pool = new Pool(Edge.class);

		int ID = 0;
		int numWidgets = 3;

		Set<Vertex> hardVerts = new HashSet<Vertex>();

		for(int widgetIdx=0; widgetIdx<numWidgets; widgetIdx++) {
			VertexPair vEasy1 = new VertexPair(ID++, BloodType.O, BloodType.O, false, 0.0, false);
			pool.addPair(vEasy1);
			VertexPair vEasy2 = new VertexPair(ID++, BloodType.O, BloodType.O, false, 0.0, false);
			pool.addPair(vEasy2);
			VertexPair vEasy3 = new VertexPair(ID++, BloodType.O, BloodType.O, false, 0.0, false);
			pool.addPair(vEasy3);
			VertexPair vHard = new VertexPair(ID++, BloodType.O, BloodType.O, false, 0.0, false);
			hardVerts.add(vHard);
			pool.addPair(vHard);
			
			// Form a 3-cycle between the three easy-to-matches
			pool.setEdgeWeight(pool.addEdge(vEasy1, vEasy2), 1.0);
			pool.setEdgeWeight(pool.addEdge(vEasy2, vEasy3), 1.0);
			pool.setEdgeWeight(pool.addEdge(vEasy3, vEasy1), 1.0);
			
			// Form a 2-cycle between an easy, hard that breaks the 3-cycle above
			pool.setEdgeWeight(pool.addEdge(vEasy1, vHard), 1.0);
			pool.setEdgeWeight(pool.addEdge(vHard, vEasy1), 1.0);
		}

		CycleGenerator cg = new CycleGenerator(pool);
		List<Cycle> cycles = cg.generateCyclesAndChains(3, 3);

		// For each vertex, get list of cycles that contain this vertex
		CycleMembership membership = new CycleMembership(pool, cycles);

		try {
			FairnessCPLEXSolver s = new FairnessCPLEXSolver(pool, cycles, membership, hardVerts);

			Solution alphaStarSol = s.solveForAlphaStar();
			Solution fairSol = s.solve(alphaStarSol.getObjectiveValue());
			Solution unfairSol = s.solve(0.0);

			assertTrue(numWidgets*2 == fairSol.getObjectiveValue());
			assertTrue(numWidgets*3 == unfairSol.getObjectiveValue());
			
		} catch(SolverException e) {
			e.printStackTrace();
		}
	}

}
