package edu.cmu.cs.dickerson.kpd.dynamic;

import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.fairness.alg.FairnessUtil;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil.ProbabilityDistribution;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSDonor;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSGenerator;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSRecipient;

public class DriverKDD {
	
	/**
	 * Outputs the set of graphs we used for the KDD-2014 submission
	 * @param args (ignored)
	 */
	public static void main(String[] args) {
		
		// Output all graphs with APD bimodal distribution (more conservative than the UNOS one)
		ProbabilityDistribution failureDist = ProbabilityDistribution.BIMODAL_CORRELATED_APD;
		
		// Generate draws from all UNOS match runs currently on the machine
		Random r = new Random(12345);
		String basePath = "/Users/spook/amem/kpd/files_real_runs/zips";
		UNOSGenerator gen = UNOSGenerator.makeAndInitialize(basePath, ',', r);

		// Each graph starts out with K pairs, then K' enter per time period for T time periods
		int numTimePeriods = 24;
		int initialPoolSize = 200;
		int enterPerPeriod = 25;
		int graphSize = initialPoolSize + (numTimePeriods * enterPerPeriod);
		IOUtil.dPrintln("Total graph size: " + graphSize + " (I" + initialPoolSize + " + T" + numTimePeriods + " x E" + enterPerPeriod + ")");

		// Base output filename, leads to files ${baseOut}.input, ${baseOut}-details.input
		String baseOut = "unos_bimodal_apd_v" + graphSize;
				
		// Number of base graphs to generate; note we'll generate 3x this number for the different weights
		int numGraphReps = 25; 
		
		for(int graphRep=0; graphRep<numGraphReps; graphRep++) {
			
			IOUtil.dPrint("Graph repetition " + graphRep + "/" + numGraphReps + "...");
			
			// Generates base pool: unit edge weights, no failure probabilities
			Pool pool = gen.generatePool(graphSize);
			
			// Assign failure probabilities to edges (can be ignored by optimizer)
			FailureProbabilityUtil.setFailureProbability(pool, failureDist, r);
			
			// Want to output three different sets of weights for each graph:
			// (1)  1 (for max cardinality)
			// (2)  1 if non-marginalized, (1+beta) if marginalized (for max cardinality + fairness)
			// (3)  w, where is the weight from the Cox regression (for max life)
				
			// Max cardinality
			pool.writeToUNOSKPDFile(baseOut + "_maxcard");
			
			// Max cardinality subject to fairness: set all marginalized transplants to (1+beta)
			double beta = 1.0;
			Set<Vertex> marginalizedVertices = DriverKDD.getMarginalizedVertices(pool);
			FairnessUtil.setFairnessEdgeWeights(pool, beta, marginalizedVertices);
			
			pool.writeToUNOSKPDFile(baseOut + "_maxcardfair");
			
			// Max life -- reweight via our Cox proportional hazard regression model
			
			
			
			pool.writeToUNOSKPDFile(baseOut + "_maxlife");
			
			System.exit(-1);
		}
	}

	public static Set<Vertex> getMarginalizedVertices(Pool pool) {
		return null;
	}
	
	public static void setMaxLifeEdgeWeights(Pool pool) {
		
		for(Edge e : pool.edgeSet()) {
			
			if(pool.getEdgeTarget(e).isAltruist()) {
				pool.setEdgeWeight(e, 0.0);
			} else {
				
				UNOSRecipient r = pool.getEdgeTarget(e).getUnderlyingPair().getRecipient();
				UNOSDonor d = pool.getEdgeSource(e).getUnderlyingPair().getDonors().iterator().next(); // assumes one donor per patient
				double coxWgt = 
						1.0052498 * d.age +
						1.0022858 * r.age + 
						1.0464164 * r.getHLA_A_Mismatch(d) + 
						1.0171467 * r.getHLA_B_Mismatch(d) +
						1.0795165 * r.getHLA_DR_Mismatch(d) +
						1.0120265 * r.getABOMismatch(d);
				coxWgt = Math.exp(-coxWgt);
				
				IOUtil.dPrintln("Cox Weight: " + coxWgt);
				pool.setEdgeWeight(e, coxWgt);
			}
		}
		
	}

}
