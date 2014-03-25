package edu.cmu.cs.dickerson.kpd.solver.approx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.helper.MapUtil;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationLPRelaxCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class CycleLPRelaxationPacker extends Packer {

	private List<Cycle> cycles;
	private CycleMembership membership;
	private Pool pool;
	private Map<Integer, Double> sortedIndex;

	private boolean isInitialized = false;

	public CycleLPRelaxationPacker(Pool pool, List<Cycle> cycles, CycleMembership membership, boolean doInitialization) {
		this.pool = pool;
		this.cycles = cycles;
		this.membership = membership;
		if(doInitialization) { init(); }
	}

	/**
	 * Gets LP relaxation weights for each cycle in the Pool
	 */
	private void init() {

		IOUtil.dPrintln(this.getClass().getSimpleName(), "Doing one-time heavyweight LP relaxation initialization");

		CycleFormulationLPRelaxCPLEXSolver solver = new CycleFormulationLPRelaxCPLEXSolver(this.pool, this.cycles, this.membership);
		try {
			// Sort CycleIdx->Weight in reverse order by value (higher weights first)
			this.sortedIndex = MapUtil.sortByValue( solver.solve(), true);
		} catch(SolverException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		this.cycles = new ArrayList<Cycle>(cycles);
		this.isInitialized = true;
	}

	@Override
	public Solution pack(double upperBound) {

		if(!isInitialized) { init(); }

		Set<Cycle> matching = new HashSet<Cycle>();
		double objVal = 0.0;

		// Pack cycles
		long start = System.nanoTime();

		// Keep track of which vertices are in the matching so far
		Set<Vertex> matchedVerts = new HashSet<Vertex>();

		// Iterate from highest to lowest weight cycle in the set,
		// shuffling little chunks at a time to add some randomization
		List<Cycle> cycleSubList = new ArrayList<Cycle>();
		int cycleSubListMaxSize = 10;

		for(Iterator<Integer> fullCycleIt = sortedIndex.keySet().iterator(); fullCycleIt.hasNext(); ) {

			// Index into full cycle list, add this next cycle to list
			cycleSubList.add( cycles.get(fullCycleIt.next()) );

			// Every K cycles (or, if we're at the end, fewer than K cycles), pack
			if(cycleSubList.size() >= cycleSubListMaxSize || !fullCycleIt.hasNext()) {

				Collections.shuffle(cycleSubList);
				for(Cycle cycle : cycleSubList) {
					Set<Vertex> cVerts = Cycle.getConstituentVertices(cycle, pool);

					// If no vertices in this cycle are matched, it's legal to add; add it
					if(Collections.disjoint(cVerts, matchedVerts)) {
						matchedVerts.addAll(cVerts);
						objVal += cycle.getWeight();
						matching.add(cycle);
					}
				}
				cycleSubList.clear();
			}
			
			// If we hit the upper bound, break out
			if(objVal >= upperBound) {
				break;
			}
		}

		long end = System.nanoTime();
		long totalTime = end - start;


		// Construct formal matching, return
		Solution sol = new Solution();
		sol.setMatching(matching);
		sol.setObjectiveValue(objVal);
		sol.setSolveTime(totalTime);
		return sol;
	}
}
