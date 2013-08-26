package edu.cmu.cs.dickerson.kpd.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.KPDEdge;
import edu.cmu.cs.dickerson.kpd.structure.KPDPool;
import edu.cmu.cs.dickerson.kpd.structure.KPDVertex;

public class SaidmanGenerator extends Generator {

	private final double Pr_FEMALE = 0.4090;
	private final double Pr_MALE = 1 - Pr_FEMALE;

	private final double Pr_SPOUSAL_DONOR = 0.4897;
	private final double Pr_NONSPOUSAL_DONOR = 1 - Pr_SPOUSAL_DONOR;

	private final double Pr_LOW_PRA = 0.7019;
	private final double Pr_MED_PRA = 0.2;
	private final double Pr_HIGH_PRA = 0.0981;

	// Numbers taken from Saidman et al.'s 2006 paper "Increasing
	// the Opportunity of Live Kidney Donation...", third page.
	private final double Pr_LOW_PRA_INCOMPATIBILITY = 0.05;
	private final double Pr_MED_PRA_INCOMPATIBILITY = 0.45;
	private final double Pr_HIGH_PRA_INCOMPATIBILITY = 0.9;

	private final double Pr_SPOUSAL_PRA_COMPATIBILITY = 0.75;

	private final double Pr_TYPE_A = 0.3373;
	private final double Pr_TYPE_B = 0.1428;
	private final double Pr_TYPE_AB = 0.0385;
	private final double Pr_TYPE_O = 0.4814;
	
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
