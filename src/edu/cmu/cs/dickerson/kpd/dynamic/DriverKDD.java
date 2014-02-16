package edu.cmu.cs.dickerson.kpd.dynamic;

import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.fairness.alg.FairnessUtil;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
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

		// Where are the (unzipped, raw) UNOS files located?
		String basePath = "";
		if(new File("/Users/spook").exists()) {
			basePath = "/Users/spook/amem/kpd/files_real_runs/zips";
		} else if(new File("/home/spook").exists()) {
			basePath = "/home/spook/amem/kpd/files_real_runs/zips";	
		} else if(new File("usr0/home/jpdicker").exists()) {
			basePath = "/usr0/home/jpdicker/amem/kpd/files_real_runs/zips";	
		} else {
			System.err.println("Can't find path to UNOS files!");
			System.exit(-1);
		}

		// Generate draws from all UNOS match runs currently on the machine
		Random r = new Random(12345);
		UNOSGenerator gen = UNOSGenerator.makeAndInitialize(basePath, ',', r);
		IOUtil.dPrintln("UNOS generator operating on #donors: " + gen.getDonors().size() + " and #recipients: " + gen.getRecipients().size());


		// Each graph starts out with K pairs, then K' enter per time period for T time periods
		int numTimePeriods = 24;
		int initialPoolSize = 200;
		int enterPerPeriod = 25;
		int graphSize = initialPoolSize + (numTimePeriods * enterPerPeriod);
		IOUtil.dPrintln("Total graph size: " + graphSize + " (I" + initialPoolSize + " + T" + numTimePeriods + " x E" + enterPerPeriod + ")");

		// Number of base graphs to generate; note we'll generate 3x this number for the different weights
		int numGraphReps = 25; 

		for(int graphRep=0; graphRep<numGraphReps; graphRep++) {

			// Base output filename, leads to files ${baseOut}.input, ${baseOut}-details.input
			String baseOut = "unos_bimodal_apd_v" + graphSize + "_i" + graphRep;

			IOUtil.dPrintln("Graph repetition " + graphRep + "/" + numGraphReps + "...");

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
			for(Edge e : pool.edgeSet()) {
				pool.setEdgeWeight(e, DriverKDD.getCoxWeight(pool, e) );
			}
			pool.writeToUNOSKPDFile(baseOut + "_maxlife");

			//System.exit(-1);
		}
	}

	/**
	 * Computes the set of vertices that are highly-sensitized (as marked by UNOS data)
	 * or below the age of 18
	 * @param pool
	 * @return
	 */
	public static Set<Vertex> getMarginalizedVertices(Pool pool) {
		Set<Vertex> marginalized = new HashSet<Vertex>();
		for(VertexPair pair : pool.getPairs()) {
			UNOSRecipient r = pair.getUnderlyingPair().getRecipient();
			if(r.highlySensitized || r.age < 18) {
				marginalized.add(pair);
			}
		}
		return marginalized;
	}



	/**
	 * Sets edge weights based on the weight function defined in the KDD submission,
	 * where the weight of an edge is proportional to a Cox proportional hazards model
	 * @param pool
	 */
	public static double getCoxWeight(Pool pool, Edge e) {

		if(pool.getEdgeTarget(e).isAltruist()) {
			return 0.0;
		} else {

			UNOSRecipient r = pool.getEdgeTarget(e).getUnderlyingPair().getRecipient();
			UNOSDonor d = pool.getEdgeSource(e).getUnderlyingPair().getDonors().iterator().next(); // assumes one donor per patient

			// Each of the b_i's is positive, so larger magnitude is correlated with worse graft performance
			//                       coef exp(coef)  se(coef)     z Pr(>|z|)    
			//r_age            0.0075033 1.0075315 0.0007723 9.715  < 2e-16 ***
			//age_diff         0.0052361 1.0052498 0.0006743 7.766 8.10e-15 ***
			//hla_a            0.0513873 1.0527306 0.0119587 4.297 1.73e-05 ***
			//hla_dr           0.0832395 1.0868021 0.0119179 6.984 2.86e-12 ***
			//abo_incompatible 0.3211514 1.3787143 0.0747674 4.295 1.74e-05 ***
			double coxWgt = 0.0 + 
					0.0075033 * r.age +
					0.0052361 * (d.age - r.age) +
					0.0513873 * r.getHLA_A_Mismatch(d) + 
					0.0832395 * r.getHLA_DR_Mismatch(d) +
					0.3211514 * (r.abo.canGetFrom(d.abo) ? 0.0 : 1.0)
					;
			coxWgt = 100.0 * Math.exp(-coxWgt);
			return coxWgt;
		}
	}
}
