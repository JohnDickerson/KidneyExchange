package edu.cmu.cs.dickerson.kpd.structure.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexAltruist;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

/**
 * Compatibility graph generator based on the following paper:
 * <i>Increasing the Opportunity of Live Kidney Donation by Matching for Two and Three Way Exchanges.</i>
 * S. L. Saidman, Alvin Roth, Tayfun Sonmez, Utku Unver, Frank Delmonico.
 * <b>Transplantation</b>.  Volume 81, Number 5, March 15, 2006.
 * 
 * This is known colloquially as the "Saidman Generator".
 * 
 * @author John P. Dickerson
 *
 */
public class SaidmanPoolGenerator extends PoolGenerator {

	// Numbers taken from Saidman et al.'s 2006 paper "Increasing
	// the Opportunity of Live Kidney Donation..."
	protected double Pr_FEMALE = 0.4090;

	protected double Pr_SPOUSAL_DONOR = 0.4897;

	protected double Pr_LOW_PRA = 0.7019;
	protected double Pr_MED_PRA = 0.2;

	protected double Pr_LOW_PRA_INCOMPATIBILITY = 0.05;
	protected double Pr_MED_PRA_INCOMPATIBILITY = 0.45;
	protected double Pr_HIGH_PRA_INCOMPATIBILITY = 0.90;

	protected double Pr_SPOUSAL_PRA_COMPATIBILITY = 0.75;

	protected double Pr_PATIENT_TYPE_O = 0.4814;
	protected double Pr_PATIENT_TYPE_A = 0.3373;
	protected double Pr_PATIENT_TYPE_B = 0.1428;

	protected double Pr_DONOR_TYPE_O = 0.4814;
	protected double Pr_DONOR_TYPE_A = 0.3373;
	protected double Pr_DONOR_TYPE_B = 0.1428;
	
	// Current unused vertex ID for optimization graphs
	private int currentVertexID;

	public SaidmanPoolGenerator(Random random) {
		super(random);
		this.currentVertexID = 0;
	}

	/**
	 * Draws a random patient's blood type from the US distribution 
	 * @return BloodType.{O,A,B,AB}
	 */
	private BloodType drawPatientBloodType() {
		double r = random.nextDouble();

		if (r <= Pr_PATIENT_TYPE_O) { return BloodType.O; }
		if (r <= Pr_PATIENT_TYPE_O + Pr_PATIENT_TYPE_A) { return BloodType.A; }
		if (r <= Pr_PATIENT_TYPE_O + Pr_PATIENT_TYPE_A + Pr_PATIENT_TYPE_B) { return BloodType.B; }
		return BloodType.AB;
	}

	/**
	 * Draws a random donor's blood type from the US distribution 
	 * @return BloodType.{O,A,B,AB}
	 */
	private BloodType drawDonorBloodType() {
		double r = random.nextDouble();

		if (r <= Pr_DONOR_TYPE_O) { return BloodType.O; }
		if (r <= Pr_DONOR_TYPE_O + Pr_DONOR_TYPE_A) { return BloodType.A; }
		if (r <= Pr_DONOR_TYPE_O + Pr_DONOR_TYPE_A + Pr_DONOR_TYPE_B) { return BloodType.B; }
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
	private VertexPair generatePair(int ID) {

		// Draw blood types for patient and donor, along with spousal details and probability of PositiveXM
		BloodType bloodTypePatient = drawPatientBloodType();
		BloodType bloodTypeDonor = drawDonorBloodType();
		boolean isWifePatient = isPatientFemale() && isDonorSpouse();
		double patientCPRA = generatePraIncompatibility(isWifePatient);

		// Can this donor donate to his or her patient?
		boolean compatible = bloodTypeDonor.canGiveTo(bloodTypePatient)    // Donor must be blood type compatible with patient
				&& !isPositiveCrossmatch(patientCPRA);   // Crossmatch must be negative

		return new VertexPair(ID, bloodTypePatient, bloodTypeDonor, isWifePatient, patientCPRA, compatible);
	}

	/**
	 * Random rolls an altruistic donor (donor with no attached patient)
	 * @param ID unique identifier for the vertex
	 * @return altruistic donor vertex KPDVertexAltruist
	 */
	private VertexAltruist generateAltruist(int ID) {

		// Draw blood type for the altruist
		BloodType bloodTypeAltruist = drawDonorBloodType();

		return new VertexAltruist(ID, bloodTypeAltruist);
	}


	public boolean isCompatible(VertexPair donor, VertexPair patient) { 
		boolean compatible = donor.getBloodTypeDonor().canGiveTo(patient.getBloodTypePatient())    // Donor must be blood type compatible with patient
				&& !isPositiveCrossmatch(patient.getPatientCPRA());   // Crossmatch must be negative
		return compatible;
	}
	
	public boolean isCompatible(VertexAltruist alt, VertexPair patient) { 
		boolean compatible = alt.getBloodTypeDonor().canGiveTo(patient.getBloodTypePatient())    // Donor must be blood type compatible with patient
				&& !isPositiveCrossmatch(patient.getPatientCPRA());   // Crossmatch must be negative
		return compatible;
	}
	
	
	
	@Override
	public Pool generate(int numPairs, int numAltruists) {

		assert(numPairs > 0);
		assert(numAltruists >= 0);

		// Keep track of the three types of vertices we can generate: 
		// altruist-no_donor, patient-compatible_donor, patient-incompatible_donor
		List<VertexPair> incompatiblePairs = new ArrayList<VertexPair>();
		List<VertexPair> compatiblePairs = new ArrayList<VertexPair>();
		List<VertexAltruist> altruists = new ArrayList<VertexAltruist>();

		// Generate enough incompatible and compatible patient-donor pair vertices
		while(incompatiblePairs.size() < numPairs) {

			VertexPair v = generatePair(currentVertexID++);
			if(v.isCompatible()) {
				compatiblePairs.add(v);  // we don't do anything with these
				currentVertexID--;       // throw away compatible pair; reuse the ID
			} else {
				incompatiblePairs.add(v);
			}
		}

		// Generate altruistic donor vertices
		while(altruists.size() < numAltruists) {
			VertexAltruist altruist = generateAltruist(currentVertexID++);
			altruists.add(altruist);
		}

		
		

		// Only add the incompatible pairs to the pool
		Pool pool = new Pool(Edge.class);
		for(VertexPair pair : incompatiblePairs) {
			pool.addPair(pair);	
		}


		// Add altruists to the pool
		for(VertexAltruist altruist : altruists) {
			pool.addAltruist(altruist);
		}


		// Add edges between compatible donors and other patients
		for(VertexPair donorPair : incompatiblePairs) {
			for(VertexPair patientPair : incompatiblePairs) {

				if(donorPair.equals(patientPair)) { continue; }

				if(isCompatible(donorPair, patientPair)) {
					Edge e = pool.addEdge(donorPair, patientPair);
					pool.setEdgeWeight(e, 1.0);
				}
			}
		}




		for(VertexAltruist alt : altruists) {
			for(VertexPair patientPair : incompatiblePairs) {

				// Add edges from a donor to a compatible patient elsewhere
				if(isCompatible(alt, patientPair)) {
					Edge e = pool.addEdge(alt, patientPair);
					pool.setEdgeWeight(e, 1.0);
				}
				
				// Add dummy edges from a non-altruist donor to each of the altruists
				Edge dummy = pool.addEdge(patientPair, alt);
				pool.setEdgeWeight(dummy, 0.0);
			}
		}

		return pool;
	}

	
	@Override
	public Set<Vertex> addVerticesToPool(Pool pool, int numPairs, int numAltruists) {
		
		// Generate new vertices
		Pool more = this.generate(numPairs, numAltruists);
		
		// Add edges from/to the new vertices
		for(VertexPair v : more.getPairs()) { pool.addPair(v); }
		for(VertexPair vN : more.getPairs()) {
			for(VertexPair vO : pool.getPairs()) {
				if(vN.equals(vO)) { continue; }  // Don't add self-edges
				
				// Donate from new vertex to other vertex
				if(isCompatible(vN, vO) && !pool.containsEdge(vN, vO)) {
					pool.setEdgeWeight(pool.addEdge(vN, vO), 1.0);
				}
				// Donate from other vertex to new vertex
				if(isCompatible(vO, vN)&& !pool.containsEdge(vO, vN)) {
					pool.setEdgeWeight(pool.addEdge(vO, vN), 1.0);
				}
			}
			
			// Adds edges from old altruists to new vertices
			for(VertexAltruist altO : pool.getAltruists()) {
				if(isCompatible(altO, vN)) {
					pool.setEdgeWeight(pool.addEdge(altO, vN), 1.0);
				}
				// Add dummy edges from a non-altruist donor to each of the altruists
				pool.setEdgeWeight(pool.addEdge(vN, altO), 0.0);
			}
		}
		
		
		// Add edges from/to the new altruists from all (old+new) vertices
		for(VertexAltruist a : more.getAltruists()) { pool.addAltruist(a); }
		for(VertexAltruist altN : more.getAltruists()) {
			// No edges between altruists
			for(VertexPair v : pool.getPairs()) {
				if(isCompatible(altN, v)) {
					pool.setEdgeWeight(pool.addEdge(altN, v), 1.0);
				}
				
				// Add dummy edges from a non-altruist donor to each of the altruists
				pool.setEdgeWeight(pool.addEdge(v, altN), 0.0);
			}
		}
		
		// Return only the new vertices that were generated
		return more.vertexSet();
	}

}
