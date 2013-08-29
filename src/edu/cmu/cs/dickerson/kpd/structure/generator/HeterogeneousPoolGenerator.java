package edu.cmu.cs.dickerson.kpd.structure.generator;

import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.VertexAltruist;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

/**
 * Compatibility graph generator based on the following paper:
 * <i>Kidney Exchange in Dynamic Sparse Heterogeneous Pools.</i>
 * Itai Ashlagi, Patrick Jaillet, Vahideh H. Manshadi
 * <b>EC-2013</b>.  (Extended abstract.)
 * 
 * @author John P. Dickerson
 *
 */
public class HeterogeneousPoolGenerator extends PoolGenerator {

	public HeterogeneousPoolGenerator(Random random) {
		super(random);
	}

	@Override
	public Pool generate(int numPairs, int numAltruists) {
		return generate(numPairs, numAltruists, 0.5);
	}

	public Pool generate(int numPairs, int numAltruists, double pctEasyToMatch) {

		Pool pool = new Pool(Edge.class);

		int numEasyToMatch = (int) Math.round(pctEasyToMatch * numPairs);


		double EASY_CPRA = 0.5;
		double HARD_CPRA = 1.0 - (1.0) / (double)numPairs;    // Ashlagi's model uses constant/|V| for highly-sensitized probability
		
		
		// Make n1 easy-to-match vertices with low CPRA and n2 hard-to-match with high CPRA
		int ID = 0;
		for(int pairIdx=0; pairIdx < numPairs; pairIdx++) {

			double cpra;
			if(pairIdx < numEasyToMatch) {
				cpra = EASY_CPRA;
			} else {
				cpra = HARD_CPRA;
			}

			VertexPair v = new VertexPair(ID++, BloodType.O, BloodType.O, false, cpra, false);
			pool.addPair(v);
		}

		// Connect vertices randomly according to CPRA
		for(VertexPair donorV : pool.getPairs()) {
			for(VertexPair patientV : pool.getPairs()) {
				
				// No self-loops (assume pairs aren't compatible)
				if(donorV.equals(patientV)) { continue; }

				// Forms an incoming edge with probability CPRA (either high or low)
				if(random.nextDouble() >= patientV.getPatientCPRA()) {
					pool.setEdgeWeight(pool.addEdge(donorV, patientV), 1.0);
				}

			}
		}

		// Add in altruists, with high probability of edges going to easy-to-match patients
		// and low probability otherwise (along with 0.0-weight dummy back-edges)
		for(int altIdx=0; altIdx < numAltruists; altIdx++) {
			VertexAltruist alt = new VertexAltruist(ID++, BloodType.O);
			pool.addAltruist(alt);
			for(VertexPair v : pool.getPairs()) {
				if(random.nextDouble() > v.getPatientCPRA()) {
					pool.setEdgeWeight(pool.addEdge(alt, v), 1.0);
				}
				pool.setEdgeWeight(pool.addEdge(v, alt), 0.0);
			}
		}

		return pool;
	}

}
