package edu.cmu.cs.dickerson.kpd.drivers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexAltruist;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil;
import edu.cmu.cs.dickerson.kpd.structure.alg.FailureProbabilityUtil.ProbabilityDistribution;
import edu.cmu.cs.dickerson.kpd.structure.generator.UNOSGenerator;

public class DriverJAIRRevisions {

	/**
	 * Outputs the set of graphs we used for the JAIR Revisions for the Multi-Organ paper
	 * @param args (ignored)
	 */
	public static void main(String[] args) {

		// Output all graphs with APD bimodal distribution (more conservative than the UNOS one)
		ProbabilityDistribution failureDist = ProbabilityDistribution.CONSTANT;
		double constantFailureRate = 0.2;

		// Where are the (unzipped, raw) UNOS files located?
		String basePath = IOUtil.getBaseUNOSFilePath();

		// Generate draws from all UNOS match runs currently on the machine
		Random r = new Random();   // add a seed if you want
		UNOSGenerator gen = UNOSGenerator.makeAndInitialize(basePath, ',', r);
		IOUtil.dPrintln("UNOS generator operating on #donors: " + gen.getDonors().size() + " and #recipients: " + gen.getRecipients().size());

		// Iterate over tuples of (#pairs, kidney vs liver pct, and #kidney altruists)
		List<Integer> numPairsList = Arrays.asList(new Integer[] {6000});//, 5122, 877}); 
		List<Double>  pctKidneyList = Arrays.asList(new Double[] {0.85377});//, 1.0, 0.0}); 
		List<Integer> numKidneyAltsList = Arrays.asList(new Integer[] {100});//, 100, 0});
		assert( numPairsList.size() == pctKidneyList.size());
		assert( pctKidneyList.size() == numKidneyAltsList.size());

		// Probability that a kidney donor is willing to give a liver to a liver patient
		List<Double> probKidneyToLiverList = Arrays.asList(new Double[] {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0});

		
		// Number of base graphs to generate; just doing 62 for now (for one 64-core Steamroller node)
		int numGraphReps = 32; 
		for(int typeIdx=0; typeIdx<numPairsList.size(); typeIdx++) {

			int numPairs = numPairsList.get(typeIdx);
			double pctKidney = pctKidneyList.get(typeIdx);
			int numKidneyAlts = numKidneyAltsList.get(typeIdx);
			for(int graphRep=0; graphRep<numGraphReps; graphRep++) {

				IOUtil.dPrintln("Graph repetition group: " + graphRep + "/" + numGraphReps + "...");

				// Want to use the same seed for each of the probKidneyToLiver graph generations,
				// but a new seed every time we loop through the entire list
				long seed = System.currentTimeMillis();
				for(double probKidneyToLiver : probKidneyToLiverList) {

					r.setSeed(seed);
					IOUtil.dPrintln("Looking at numPairs=" + numPairs + ", pctKidney=" + pctKidney + ", numKidneyAlts=" + numKidneyAlts, ", probKidneyToLiver=" + probKidneyToLiver);

					// Base output filename, leads to files ${baseOut}.input, ${baseOut}-details.input
					String baseOut = "unos_v" + numPairs + "_p" + pctKidney + "_a" + numKidneyAlts + "_f0.0_kl" + probKidneyToLiver + "_i" + graphRep;

					// Generates base pool: unit edge weights, no failure probabilities
					Pool pool = gen.generate(numPairs, numKidneyAlts);

					// Assign failure probabilities to edges (can be ignored by optimizer)
					FailureProbabilityUtil.setFailureProbability(pool, failureDist, r, constantFailureRate);    // 0.9 =  constant failure rate used for UNOS runs

					// Post-process: remove some edges from kidney donors to liver patients, depending
					//               on the probability of the former giving to the latter
					if(pctKidney > 0.0) {
						DriverJAIRRevisions.removeKidneyToLiverEdges(pool, r, probKidneyToLiver, pctKidney);
					}

					// Write to .input and .input details file for C++ optimizer
					pool.writeToUNOSKPDFile(baseOut);

				} // graphRep
			} // probKidneyToLiver in probKidneyToLiverList
		} // typeIdx
	}



	public static void removeKidneyToLiverEdges(Pool pool, Random r, double probKidneyToLiver, double pctKidney) {

		IOUtil.dPrintln("Removing edges from some kidney donors to liver pairs.");

		// First, label the vertices as either kidney- or liver-needing (all altruists are assumed kidney)
		Set<Vertex> kidneyPairedDonors = new HashSet<Vertex>();
		Set<Vertex> liverPairedDonors = new HashSet<Vertex>();
		for(VertexPair vp : pool.getPairs()) {
			if(r.nextDouble() < pctKidney) { 
				kidneyPairedDonors.add(vp);
			} else {
				liverPairedDonors.add(vp);
			}
		}
		for(VertexAltruist alt : pool.getAltruists()) {
			kidneyPairedDonors.add(alt);
		}

		// Next, for each kidney-paired donor, determine if that donor is willing
		// to give a liver.  If not, remove all outgoing edges to liver-paired donors
		Set<Edge> edgesToRemove = new HashSet<Edge>();
		for(Vertex kidneyV : kidneyPairedDonors) {
			boolean willingToGive = (r.nextDouble() < probKidneyToLiver);
			willingToGive &= !(kidneyV.isAltruist());   // disallow any kidney altruists from given to liver pairs
			if(willingToGive) { continue; }

			for(Edge e : pool.outgoingEdgesOf(kidneyV)) {
				if(liverPairedDonors.contains( pool.getEdgeTarget(e) )) {
					edgesToRemove.add(e);
				}
			}
		}
		int removedEdgeCt = 0;
		for(Edge e : edgesToRemove) {
			pool.removeEdge(e);
			removedEdgeCt++;
		}
		
		IOUtil.dPrintln("Removed " + removedEdgeCt + " edges from kidney donors to liver pairs (" + pool.edgeSet().size() + " remain).");

	}
}
