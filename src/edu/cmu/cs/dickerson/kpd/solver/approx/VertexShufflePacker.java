package edu.cmu.cs.dickerson.kpd.solver.approx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class VertexShufflePacker extends Packer {

	private Pool pool;
	private CycleMembership membership;
	private List<Cycle> cycles;

	public VertexShufflePacker(Pool pool, List<Cycle> cycles, CycleMembership membership) {
		this.pool = pool;
		this.cycles = cycles;
		this.membership = membership;
	}
	
	@Override
	public Solution pack(double upperBound) {
		
		Set<Cycle> matching = new HashSet<Cycle>();
		double objVal = 0.0;

		// Keep track of which vertices are in the matching so far
		Set<Vertex> matchedVerts = new HashSet<Vertex>();

		long start = System.nanoTime();
		
		// Shuffle to mimic random sampling
		List<Vertex> vertices = new ArrayList<Vertex>( membership.getAllVertices() );
		Collections.shuffle(vertices);
		
		// Pack vertices
		for(Vertex v : vertices) {
			
			// Find the first cycle that contains this vertex and is legal to pack, and pack it
			List<Integer> cycleIdxs = new ArrayList<Integer>( membership.getMembershipSet(v) );
			for(Integer cycleIdx : cycleIdxs) {
				
				// Index into full cycle set
				Cycle cycle = cycles.get(cycleIdx);
				Set<Vertex> cVerts = Cycle.getConstituentVertices(cycle, pool);

				// If no vertices in this cycle are matched, it's legal to add; add it
				if(Collections.disjoint(cVerts, matchedVerts)) {
					matchedVerts.addAll(cVerts);
					objVal += cycle.getWeight();
					matching.add(cycle);
				}
				
				// If we hit the upper bound, break out
				if(objVal >= upperBound) {
					break;
				}
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
