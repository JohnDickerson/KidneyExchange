package edu.cmu.cs.dickerson.kpd.structure.real.sampler;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.real.UNOSPair;

public class ExactSplitUNOSSampler extends UNOSSampler {

	private Queue<UNOSPair> sampledPairs;
	
	public ExactSplitUNOSSampler(List<UNOSPair> pairs, Random r, int numPairs, int numAlts) {
		int pairCt=0, altCt=0;
		sampledPairs = new LinkedList<UNOSPair>();
		while(sampledPairs.size() < numPairs+numAlts) {
			
			// Sample a random pair or altruist from the entire list
			UNOSPair samplePair = pairs.get( r.nextInt(pairs.size()) );
			
			// Keep the pair or altruist if we need it, otherwise ignore and repeat
			if( samplePair.isAltruist() && altCt<numAlts ) {
				sampledPairs.add(samplePair);
				altCt++;
			} else if( !samplePair.isAltruist() && pairCt<numPairs) {
				sampledPairs.add(samplePair);
				pairCt++;
			} else {
				// We have enough of this vertex type already; resample until we hit a different kind
			}
		}
	}
	
	@Override
	public UNOSPair takeSample() {
		return sampledPairs.poll();
	}

}
