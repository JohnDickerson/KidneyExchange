package edu.cmu.cs.dickerson.kpd.structure.real.sampler;

import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.real.UNOSPair;

public class RealSplitUNOSSampler extends UNOSSampler {

	private List<UNOSPair> pairs;
	private Random r;
	
	public RealSplitUNOSSampler(List<UNOSPair> pairs, Random r) {
		this.pairs = pairs;
		this.r = r;
	}
	
	@Override
	public UNOSPair takeSample() {
		int rndPairIdx = this.r.nextInt(pairs.size());
		UNOSPair samplePair = pairs.get( rndPairIdx );
		return samplePair;
	}

}
