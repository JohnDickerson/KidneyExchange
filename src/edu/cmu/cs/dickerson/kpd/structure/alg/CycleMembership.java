package edu.cmu.cs.dickerson.kpd.structure.alg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;

public class CycleMembership {
	
	private Map<Vertex, Set<Integer>> membership;
	
	public CycleMembership(Pool pool, List<Cycle> cycles) {

		IOUtil.dPrintln(getClass().getSimpleName(), "Computing membership for " + pool.vertexSet().size() + " vertices and " + cycles.size() + " cycles/chains.");
		
		// Want to make sure EVERY vertex in the pool is in this map
		membership = new HashMap<Vertex, Set<Integer>>();
		for(Vertex v : pool.vertexSet()) {
			membership.put(v, new HashSet<Integer>());
		}

		// For each cycle c, for each vertex v in c, add c's IP column ID to v's membership list
		int cycleIdx = 0;
		for(Cycle c : cycles) {
			for(Edge e : c.getEdges()) {
				Vertex v = pool.getEdgeTarget(e);
				membership.get(v).add(cycleIdx);
			}
			cycleIdx++;
		}
		
		IOUtil.dPrintln(getClass().getSimpleName(), "Done computing membership.");
	}

	public Set<Integer> getMemberList(Vertex v) {
		return membership.get(v);
	}
	

}
