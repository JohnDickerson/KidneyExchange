package edu.cmu.cs.dickerson.kpd.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.KPDEdge;
import edu.cmu.cs.dickerson.kpd.structure.KPDPool;
import edu.cmu.cs.dickerson.kpd.structure.KPDVertexAltruist;
import edu.cmu.cs.dickerson.kpd.structure.KPDVertexPair;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

/**
 * Compatibility graph generator based on the following paper:
 * <i>Increasing the Opportunity of Live Kidney Donation by Matching for Two and Three Way Exchanges.</i>
 * S. L. Saidman, Alvin Roth, Tayfun Sönmez, Utku Ünver, Frank Delmonico.
 * <b>Transplantation</b>.  Volume 81, Number 5, March 15, 2006.
 * 
 * This is known colloquially as the "Saidman Generator".
 * 
 * @author John P. Dickerson
 *
 */
public class SaidmanGenerator extends Generator {

	// Numbers taken from Saidman et al.'s 2006 paper "Increasing
	// the Opportunity of Live Kidney Donation..."
	private final double Pr_FEMALE = 0.4090;

	private final double Pr_SPOUSAL_DONOR = 0.4897;

	private final double Pr_LOW_PRA = 0.7019;
	private final double Pr_MED_PRA = 0.2;

	private final double Pr_LOW_PRA_INCOMPATIBILITY = 0.05;
	private final double Pr_MED_PRA_INCOMPATIBILITY = 0.45;
	private final double Pr_HIGH_PRA_INCOMPATIBILITY = 0.9;

	private final double Pr_SPOUSAL_PRA_COMPATIBILITY = 0.75;

	private final double Pr_TYPE_O = 0.4814;
	private final double Pr_TYPE_A = 0.3373;
	private final double Pr_TYPE_B = 0.1428;

	public SaidmanGenerator(Random random) {
		super(random);
	}

	/**
	 * Draws a random blood type from the US distribution 
	 * @return BloodType.{O,A,B,AB}
	 */
	private BloodType drawBloodType() {
		double r = random.nextDouble();

		if (r <= Pr_TYPE_O) { return BloodType.O; }
		if (r <= Pr_TYPE_O + Pr_TYPE_A) { return BloodType.A; }
		if (r <= Pr_TYPE_O + Pr_TYPE_A + Pr_TYPE_B) { return BloodType.B; }
		return BloodType.AB;
	}

	/**
	 * Draws a random gender from the US waitlist distribution
	 * @return true if patient is female, false otherwise
	 */
	private boolean isPatientFemale() {
		return random.nextDouble() <= Pr_FEMALE;
	}

	/**
	 * Draws a random spousal relationship between donor and patient
	 * @return true if willing donor is patient's spouse, false otherwise
	 */
	private boolean isDonorSpouse() {
		return random.nextDouble() <= Pr_SPOUSAL_DONOR;
	}

	/**
	 * Random roll to see if a patient and donor are crossmatch compatible
	 * @param pr_PraIncompatibility probability of a PRA-based incompatibility
	 * @return true is simulated positive crossmatch, false otherwise
	 */
	private boolean isPositiveCrossmatch(double pr_PraIncompatibility) {
		return random.nextDouble() <= pr_PraIncompatibility;
	}

	/**
	 * Randomly generates CPRA (Calculated Panel Reactive Antibody) for a
	 * patient-donor pair, using the Saidman method.  If the patient is the
	 * donor's wife, then CPRA is increased.
	 * @param isWifePatient is the patent the wife of the donor?
	 * @return scaled CPRA double value between 0 and 1.0
	 */
	double generatePraIncompatibility(boolean isWifePatient) {
		double pr_PraIncompatiblity;

		double r = random.nextDouble();
		if (r <= Pr_LOW_PRA) {
			pr_PraIncompatiblity = Pr_LOW_PRA_INCOMPATIBILITY;
		} else if (r <= Pr_LOW_PRA + Pr_MED_PRA) {
			pr_PraIncompatiblity = Pr_MED_PRA_INCOMPATIBILITY;
		} else {
			pr_PraIncompatiblity = Pr_HIGH_PRA_INCOMPATIBILITY;
		}

		if (!isWifePatient) { 
			return pr_PraIncompatiblity; 
		} else {
			return 1.0 - Pr_SPOUSAL_PRA_COMPATIBILITY*(1.0 - pr_PraIncompatiblity);
		}
	}	


	/**
	 * Randomly rolls a patient-donor pair (possibly compatible or incompatible)
	 * @param ID unique identifier for the vertex
	 * @return a patient-donor pair KPDVertexPair
	 */
	private KPDVertexPair generatePair(int ID) {

		// Draw blood types for patient and donor, along with spousal details and probability of PositiveXM
		BloodType bloodTypePatient = drawBloodType();
		BloodType bloodTypeDonor = drawBloodType();
		boolean isWifePatient = isPatientFemale() && isDonorSpouse();
		double patientCPRA = generatePraIncompatibility(isWifePatient);

		// Can this donor donate to his or her patient?
		boolean compatible = bloodTypeDonor.canGiveTo(bloodTypePatient)    // Donor must be blood type compatible with patient
				&& !isPositiveCrossmatch(patientCPRA);   // Crossmatch must be negative

		return new KPDVertexPair(ID, bloodTypePatient, bloodTypeDonor, isWifePatient, patientCPRA, compatible);
	}

	/**
	 * Random rolls an altruistic donor (donor with no attached patient)
	 * @param ID unique identifier for the vertex
	 * @return altruistic donor vertex KPDVertexAltruist
	 */
	private KPDVertexAltruist generateAltruist(int ID) {

		// Draw blood type for the altruist
		BloodType bloodTypeAltruist = drawBloodType();

		return new KPDVertexAltruist(ID, bloodTypeAltruist);
	}


	@Override
	public KPDPool generate(int numPairs, int numAltruists) {

		assert(numPairs > 0);
		assert(numAltruists >= 0);

		// Keep track of the three types of vertices we can generate: 
		// altruist-no_donor, patient-compatible_donor, patient-incompatible_donor
		List<KPDVertexPair> incompatiblePairs = new ArrayList<KPDVertexPair>();
		List<KPDVertexPair> compatiblePairs = new ArrayList<KPDVertexPair>();
		List<KPDVertexAltruist> altruists = new ArrayList<KPDVertexAltruist>();


		// Keep a unique identifier for each vertex
		int ID = 0;

		// Generate enough incompatible and compatible patient-donor pair vertices
		while(incompatiblePairs.size() < numPairs) {

			KPDVertexPair v = generatePair(ID++);
			if(v.isCompatible()) {
				compatiblePairs.add(v);
			} else {
				incompatiblePairs.add(v);
			}
		}


		// Only add the incompatible pairs to the pool
		KPDPool pool = new KPDPool(KPDEdge.class);
		for(KPDVertexPair pair : incompatiblePairs) {
			pool.addPair(pair);	
		}


		// Generate altruistic donor vertices
		while(altruists.size() < numAltruists) {
			KPDVertexAltruist altruist = generateAltruist(ID++);
			altruists.add(altruist);
		}


		// Add altruists to the pool
		for(KPDVertexAltruist altruist : altruists) {
			pool.addAltruist(altruist);
		}


		// Add edges between compatible donors and other patients
		for(KPDVertexPair donorPair : incompatiblePairs) {
			for(KPDVertexPair patientPair : incompatiblePairs) {

				if(donorPair.equals(patientPair)) { continue; }

				boolean compatible = donorPair.getBloodTypeDonor().canGiveTo(patientPair.getBloodTypePatient())    // Donor must be blood type compatible with patient
						&& !isPositiveCrossmatch(patientPair.getPatientCPRA());   // Crossmatch must be negative
				if(compatible) {
					KPDEdge e = pool.addEdge(donorPair, patientPair);
					pool.setEdgeWeight(e, 1.0);
				}
			}
		}




		for(KPDVertexAltruist alt : altruists) {
			for(KPDVertexPair patientPair : incompatiblePairs) {

				// Add edges from a donor to a compatible patient elsewhere
				boolean compatible = alt.getBloodTypeDonor().canGiveTo(patientPair.getBloodTypePatient())    // Donor must be blood type compatible with patient
						&& !isPositiveCrossmatch(patientPair.getPatientCPRA());   // Crossmatch must be negative
				if(compatible) {
					KPDEdge e = pool.addEdge(alt, patientPair);
					pool.setEdgeWeight(e, 1.0);
				}
				
				// Add dummy edges from a non-altruist donor to each of the altruists
				KPDEdge dummy = pool.addEdge(patientPair, alt);
				pool.setEdgeWeight(dummy, 0.0);
			}
		}

		return pool;
	}

}
