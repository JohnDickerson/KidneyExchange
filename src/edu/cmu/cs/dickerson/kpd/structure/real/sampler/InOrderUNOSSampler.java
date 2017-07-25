package edu.cmu.cs.dickerson.kpd.structure.real.sampler;

import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.dickerson.kpd.structure.real.UNOSPair;

public class InOrderUNOSSampler extends UNOSSampler {

	private Iterator<UNOSPair> iter;
	
	public InOrderUNOSSampler(List<UNOSPair> pairs) {
		iter = pairs.iterator();
	}
	
	@Override
	public UNOSPair takeSample() {
		if(!iter.hasNext()) {
			throw new RuntimeException("Out of vertices to sample uniquely.");
		}
		return iter.next();
	}
}
