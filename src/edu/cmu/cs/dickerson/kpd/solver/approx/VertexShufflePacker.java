package edu.cmu.cs.dickerson.kpd.solver.approx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.WeightedRandomSample;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class VertexShufflePacker extends Packer {

	private Pool pool;
	private CycleMembership membership;
	private List<Cycle> cycles;
	private Set<Vertex> forbiddenVerts;
	
	public enum ShuffleType { UNIFORM_RANDOM, INVERSE_PROP_CYCLE_COUNT };
	private ShuffleType shuffleType;
	
	public VertexShufflePacker(Pool pool, List<Cycle> cycles, CycleMembership membership, ShuffleType shuffleType, Set<Vertex> forbiddenVerts) {
		this.pool = pool;
		this.cycles = cycles;
		this.membership = membership;
		this.shuffleType = shuffleType;
		this.forbiddenVerts = forbiddenVerts;
	}
	
	public VertexShufflePacker(Pool pool, List<Cycle> cycles, CycleMembership membership, ShuffleType shuffleType) {
		this(pool, cycles, membership, shuffleType, new HashSet<Vertex>());
	}
	
	private List<Vertex> shuffleVertices() {
		List<Vertex> vertices = null;
		switch(shuffleType) {
		default:
		case UNIFORM_RANDOM:
			// Shuffle to mimic random sampling
			vertices = new ArrayList<Vertex>( membership.getAllVertices() );
			Collections.shuffle(vertices);
			break;
		case INVERSE_PROP_CYCLE_COUNT:
			// Sample inversely proportional to how many cycles a vertex is in
			WeightedRandomSample<Vertex> S = new WeightedRandomSample<Vertex>();
			for(Vertex v : membership.getAllVertices()) {
				
				// If we're not allowed to match this vertex, don't add it to the set
				// NOTE: weight from cycles containing this vertex will still be included
				if(forbiddenVerts.contains(v)) { continue; }
				
				// Never want to sample vertices that are not in any cycles (no chance of matching)
				double cycleCount = membership.getMembershipSet(v).size();
				if(cycleCount == 0) { continue; }
				
				// Not worrying about overflow for now, since we won't be using this on big |cycle| counts
				double weight = (double) cycles.size() / cycleCount;
				S.add(weight, v);
			}
			vertices = S.weightedPermutation();
			break;
		}
		
		return vertices;
	}
	
	@Override
	public Solution pack(double upperBound) {
		
		Set<Cycle> matching = new HashSet<Cycle>();
		double objVal = 0.0;

		// Keep track of which vertices are in the matching so far
		Set<Vertex> matchedVerts = new HashSet<Vertex>();
		// Count any forbidden vertices as already being matched
		matchedVerts.addAll(forbiddenVerts);
		
		long start = System.nanoTime();
		
		// Shuffle vertices according to ShuffleType parameter
		List<Vertex> vertices = shuffleVertices();
		
		// Pack vertices
		for(Vertex v : vertices) {
			
			// If we've already matched this vertex, skip
			if(matchedVerts.contains(v)) {
				continue;
			}
			
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
