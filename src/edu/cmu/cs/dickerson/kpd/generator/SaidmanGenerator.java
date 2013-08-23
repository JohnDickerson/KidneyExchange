package edu.cmu.cs.dickerson.kpd.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.KPDEdge;
import edu.cmu.cs.dickerson.kpd.structure.KPDPool;
import edu.cmu.cs.dickerson.kpd.structure.KPDVertex;

public class SaidmanGenerator extends Generator {

	public SaidmanGenerator(Random random) {
		super(random);
	}

	private KPDVertex generatePair() {
	
		return null;
	}
	
	private KPDVertex generateAltruist() {
		
		return null;
	}
	
	@Override
	public KPDPool generate(int numPairs, int numAltruists) {
	
		assert(numPairs > 0);
		assert(numAltruists >= 0);
		
		// Keep track of the three types of vertices we can generate: 
		// altruist-no_donor, patient-compatible_donor, patient-incompatible_donor
		List<KPDVertex> incompatiblePairs = new ArrayList<KPDVertex>();
		List<KPDVertex> compatiblePairs = new ArrayList<KPDVertex>();
		List<KPDVertex> altruists = new ArrayList<KPDVertex>();
		
		
		// Generate enough incompatible and compatible patient-donor pair vertices
		while(incompatiblePairs.size() < numPairs) {
			
			KPDVertex v = generatePair();
			if(v.isCompatible()) {
				compatiblePairs.add(v);
			} else {
				incompatiblePairs.add(v);
			}
			
		}
		
		
		// Only add the incompatible pairs to the pool
		KPDPool pool = new KPDPool(KPDEdge.class);
		for(KPDVertex pair : incompatiblePairs) {
			pool.addVertex(pair);	
		}
		
		
		// Generate altruistic donor vertices
		while(altruists.size() < numAltruists) {
			KPDVertex altruist = generateAltruist();
			altruists.add(altruist);
		}
		
		
		// Add altruists to the pool
		for(KPDVertex altruist : altruists) {
			pool.addVertex(altruist);
		}
		
	
		// Add edges between compatible donors and other patients, and all donors to all altruists
		for(KPDVertex pair : pool.vertexSet()) {
			
			
			// Add edges from a donor to a compatible patient elsewhere
			
			
			// Add edges from a non-altruist donor to each of the altruists
			if(!pair.isAltruist()) {
				for(KPDVertex incompatiblePair : pool.getPairs()) {
					
				}
			}
		}
		
		return pool;
	}

}
