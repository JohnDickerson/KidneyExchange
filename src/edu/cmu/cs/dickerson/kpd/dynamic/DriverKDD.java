package edu.cmu.cs.dickerson.kpd.dynamic;

import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil.ProbabilityDistribution;

public class DriverKDD {
	
	/**
	 * Outputs the set of graphs we used for the KDD-2014 submission
	 * @param args (ignored)
	 */
	public static void main(String[] args) {
		
		// Output all graphs with APD bimodal distribution (more conservative than the UNOS one)
		ProbabilityDistribution failureDist = ProbabilityDistribution.BIMODAL_CORRELATED_APD;
		
	}

}
