package edu.cmu.cs.dickerson.kpd.helper;

import java.util.Comparator;

import edu.cmu.cs.dickerson.kpd.structure.Vertex;

public class VertexTimeComparator implements Comparator<Pair<Double,Vertex>> {

	@Override
	public int compare(Pair<Double, Vertex> o1, Pair<Double, Vertex> o2) {
		
		// Primary: Sort in increasing order of doubles
		int first = o1.getLeft().compareTo(o2.getLeft());
		if(first == 0) {
			// Secondary: Sort in increasing order of vertex IDs
			return o1.getRight().compareTo(o2.getRight());
		} else {
			return first;
		}
	}

}
