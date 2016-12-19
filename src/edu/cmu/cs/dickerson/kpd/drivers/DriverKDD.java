package edu.cmu.cs.dickerson.kpd.drivers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import edu.cmu.cs.dickerson.kpd.structure.generator.UNOSGenerator;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSDonor;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSRecipient;

public class DriverKDD {

	/**
	 * Outputs the set of graphs we used for the AAAI-2015 FutureMatch paper
	 * @param args (ignored)
	 */
	public static void main(String[] args) {

		// Output all graphs with APD bimodal distribution (more conservative than the UNOS one)
		ProbabilityDistribution failureDist = ProbabilityDistribution.CONSTANT;

		// Where are the (unzipped, raw) UNOS files located?
		String basePath = IOUtil.getBaseUNOSFilePath();
		
		// Generate draws from all UNOS match runs currently on the machine
		Random r = new Random();   // add a seed if you want
		UNOSGenerator gen = UNOSGenerator.makeAndInitialize(basePath, ',', r);
		IOUtil.dPrintln("UNOS generator operating on #donors: " + gen.getDonors().size() + " and #recipients: " + gen.getRecipients().size());

		// Each graph starts out with K pairs, then K' enter per time period for T time periods
		//int numTimePeriods = 24;
		//int initialPoolSize = 200;
		//int enterPerPeriod = 25;
		//int graphSize = initialPoolSize + (numTimePeriods * enterPerPeriod);
		//IOUtil.dPrintln("Total graph size: " + graphSize + " (I" + initialPoolSize + " + T" + numTimePeriods + " x E" + enterPerPeriod + ")");
		//List<Integer> graphSizeList = Arrays.asList(new Integer[] {300, 400, 500, 600, 700, 800, 900});
		List<Integer> graphSizeList = Arrays.asList(new Integer[] {64,128,256,512}); // used for UNOS runs in April 2014 (start with 250 + 52*5 per week)
		List<Double> betaList = Arrays.asList(new Double[] {});//1.0, 2.0, 3.0, 4.0, 5.0});


		// Number of base graphs to generate; note we'll generate 3x this number for the different weights
		int numGraphReps = 32; 
		for(int graphSize : graphSizeList) {
			
				IOUtil.dPrintln("Total graph size: " + graphSize);
				for(int graphRep=0; graphRep<numGraphReps; graphRep++) {

					// Base output filename, leads to files ${baseOut}.input, ${baseOut}-details.input
					String baseOut = "unos_bimodal_apd_v" + graphSize  +"_i" + graphRep;

					IOUtil.dPrintln("Graph repetition: " + graphRep + "/" + numGraphReps + "...");

					// Generates base pool: unit edge weights, no failure probabilities
					Pool pool = gen.generatePool(graphSize);

					// Assign failure probabilities to edges (can be ignored by optimizer)
					FailureProbabilityUtil.setFailureProbability(pool, failureDist, r, 0.9);    // 0.9 =  constant failure rate used for UNOS runs

					// Want to output three different sets of weights for each graph:
					// (1)  1 (for max cardinality)
					// (2)  1 if non-marginalized, (1+beta) if marginalized (for max cardinality + fairness)
					// (3)  w, where is the weight from the Cox regression (for max life)

					// Max cardinality
					pool.writeToUNOSKPDFile(baseOut + "_maxcard");
					
					// Max cardinality subject to fairness: set all marginalized transplants to (1+beta)
					Set<Vertex> marginalizedVertices = DriverKDD.getMarginalizedVertices(pool);
					for(double beta : betaList) {
						FairnessUtil.setFairnessEdgeWeights(pool, beta, marginalizedVertices);
					//	pool.writeToUNOSKPDFile(baseOut + "_maxcardfair" + beta);
					} // beta, betaList
					
					// Max life -- reweight via our Cox proportional hazard regression model
					for(Edge e : pool.edgeSet()) {
						pool.setEdgeWeight(e, DriverKDD.getCoxWeight(pool, e) );
					}
					//pool.writeToUNOSKPDFile(baseOut + "_maxlife");
				}

		} //graphSize, graphSizeList
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
			if(null==pair.getUnderlyingPair()) { continue; }
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
