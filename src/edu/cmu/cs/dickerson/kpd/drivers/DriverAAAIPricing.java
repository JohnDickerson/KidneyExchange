package edu.cmu.cs.dickerson.kpd.drivers;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil.ProbabilityDistribution;
import edu.cmu.cs.dickerson.kpd.structure.generator.UNOSGenerator;

public class DriverAAAIPricing {
	/**
	 * Outputs the set of graphs we used for the AAAI submission with Ben Plaut
	 * @param args (ignored)
	 */
	public static void main(String[] args) {

		long seed = System.currentTimeMillis();
		Random r = new Random();   // add a seed if you want
		r.setSeed(seed);

		// Output all graphs with APD bimodal distribution (more conservative than the UNOS one)
		ProbabilityDistribution failureDist = ProbabilityDistribution.CONSTANT;
		double constantFailureRate = 0.0;

		// Where are the (unzipped, raw) UNOS files located?
		String basePath = IOUtil.getBaseUNOSFilePath();

		// Generate draws from all UNOS match runs currently on the machine
		UNOSGenerator gen = UNOSGenerator.makeAndInitialize(basePath, ',', r);
		IOUtil.dPrintln("UNOS generator operating on #donors: " + gen.getDonors().size() + " and #recipients: " + gen.getRecipients().size());

		// Iterate over tuples of (#pairs, %alts)
		List<Integer> numVertsList = Arrays.asList(new Integer[] {50, 100, 150, 200, 250, 300, 500, 700, 1000});
		List<Double> pctAltsList = Arrays.asList(new Double[] {0.0, 0.01, 0.02, 0.05, 0.10, 0.25});

		// Number of base graphs to generate; just doing 62 for now (for one 64-core Steamroller node)
		int numGraphReps = 64; 
		for(int typeIdx=0; typeIdx<numVertsList.size(); typeIdx++) {

			int numVerts = numVertsList.get(typeIdx);
			for(Double pctAlts : pctAltsList) {
				int numAlts = (int) Math.ceil(pctAlts * numVerts);

				for(int graphRep=0; graphRep<numGraphReps; graphRep++) {

					IOUtil.dPrintln("|V| = " + numVerts + ", %A = " + pctAlts + ", rep: " + graphRep + "/" + numGraphReps + "...");

					// Base output filename, leads to files ${baseOut}.input, ${baseOut}-details.input
					String baseOut = "unos_v" + numVerts + "_a" + numAlts + "_i" + graphRep;

					// Generates base pool: unit edge weights, no failure probabilities
					Pool pool = gen.generate(numVerts, numAlts);

					// Assign failure probabilities to edges (can be ignored by optimizer)
					FailureProbabilityUtil.setFailureProbability(pool, failureDist, r, constantFailureRate); 

					// Write to .input and .input details file for C++ optimizer
					pool.writeToUNOSKPDFile(baseOut);

				} // graphRep
			} // pctAlts in pctAltsList
		} // typeIdx	
	}	

}
