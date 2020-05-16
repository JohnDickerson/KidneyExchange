package edu.cmu.cs.dickerson.kpd.variation;

import edu.cmu.cs.dickerson.kpd.structure.*;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static edu.cmu.cs.dickerson.kpd.variation.VariationDriver.OUTPUT_PATH;


public class VariationPoolGenerator extends PoolGenerator {
	
	//"Ethically relevant" demographics
	protected double Pr_YOUNG = 0.275;
	protected double Pr_YOUNG_NONALCOHOLIC = 0.728;
	protected double Pr_OLD_NONALCOHOLIC = 0.828;

	// Multiplied actual demographics by 0.8 to make these profiles appear more often
	protected double MULTIPLIER = 0.8;
	protected double Pr_YOUNG_HEALTHY = MULTIPLIER*0.9999063;
	protected double Pr_OLD_HEALTHY = MULTIPLIER*0.999275;
	
	//Blood type demographics
	protected double Pr_PATIENT_TYPE_O = 0.4814;
	protected double Pr_PATIENT_TYPE_A = 0.3373;
	protected double Pr_PATIENT_TYPE_B = 0.1428;

	protected double Pr_DONOR_TYPE_O = 0.4814;
	protected double Pr_DONOR_TYPE_A = 0.3373;
	protected double Pr_DONOR_TYPE_B = 0.1428;

	//Do I care about these demographics?
	protected double Pr_FEMALE = 0.4090;

	protected double Pr_SPOUSAL_DONOR = 0.4897;

	protected double Pr_LOW_PRA = 0.7019;
	protected double Pr_MED_PRA = 0.2;

	protected double Pr_LOW_PRA_INCOMPATIBILITY = 0.05;
	protected double Pr_MED_PRA_INCOMPATIBILITY = 0.45;
	protected double Pr_HIGH_PRA_INCOMPATIBILITY = 0.90;

	protected double Pr_SPOUSAL_PRA_COMPATIBILITY = 0.75;
	
	protected PrintWriter out;
	protected int vertexCount;

	// Current unused vertex ID for optimization graphs
	private int currentVertexID;

	public VariationPoolGenerator(Random random) {
		super(random);
		this.currentVertexID = 0;

		//Initialize output file
		try {
			this.out = new PrintWriter(OUTPUT_PATH + "vertices.csv");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		this.out.println("#,bloodID,rAge,rAlcohol,rHealth");
		this.vertexCount = 1;
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
	 * @return a patient-donor pair w/"ethical" demographics included
	 */
	private VariationVertexPair generatePair(int ID) {

		// Draw blood types for patient and donor, along with spousal details and probability of PositiveXM
		BloodType bloodTypePatient = drawPatientBloodType();
		BloodType bloodTypeDonor = drawDonorBloodType();
		boolean isWifePatient = isPatientFemale() && isDonorSpouse();
		double patientCPRA = generatePraIncompatibility(isWifePatient);

		// Can this donor donate to his or her patient?
		boolean compatible = bloodTypeDonor.canGiveTo(bloodTypePatient)    // Donor must be blood type compatible with patient
				&& !isPositiveCrossmatch(patientCPRA);   // Crossmatch must be negative

		//Generate "ethical" patient demographics
		boolean isYoung = false;
		boolean isNonalcoholic = false;
		boolean isHealthy = false;
		double rAge = random.nextDouble();
		double rAlcohol = random.nextDouble();
		double rHealth = random.nextDouble();
		if (rAge <= this.Pr_YOUNG) {
			isYoung = true;
			if (rAlcohol <= this.Pr_YOUNG_NONALCOHOLIC) {
				isNonalcoholic = true;
			}
			if (rHealth <= this.Pr_YOUNG_HEALTHY) {
				isHealthy = true;
			}
		} else {
			if (rAlcohol <= this.Pr_OLD_NONALCOHOLIC) {
				isNonalcoholic = true;
			}
			if (rHealth <= this.Pr_OLD_HEALTHY) {
				isHealthy = true;
			}
		}

		VariationVertexPair v = new VariationVertexPair(ID, bloodTypePatient, bloodTypeDonor, isWifePatient, patientCPRA,
				compatible, isYoung, isNonalcoholic, isHealthy);

		//Write generated vertex to output file
		this.out.println(this.vertexCount+","+v.getBloodID()+","+rAge+","+rAlcohol+","+rHealth);
		this.vertexCount++;

		return v;
	}

	public boolean isCompatible(VertexPair donorPair, VertexPair patientPair) {
		boolean compatible = donorPair.getBloodTypeDonor().canGiveTo(patientPair.getBloodTypePatient())    // Donor must be blood type compatible with patient
				&& !isPositiveCrossmatch(patientPair.getPatientCPRA());   // Crossmatch must be negative
		return compatible;
	}

	public boolean isCompatible(VertexAltruist alt, VertexPair patient) {
		boolean compatible = alt.getBloodTypeDonor().canGiveTo(patient.getBloodTypePatient())    // Donor must be blood type compatible with patient
				&& !isPositiveCrossmatch(patient.getPatientCPRA());   // Crossmatch must be negative
		return compatible;
	}

	@Override
	public Pool generate(int numPairs, int numAltruists) {

		assert(numPairs >= 0);
		assert(numAltruists >= 0);

		// Keep track of the three types of vertices we can generate:
		// altruist-no_donor, patient-compatible_donor, patient-incompatible_donor
		List<VariationVertexPair> incompatiblePairs = new ArrayList<VariationVertexPair>();
		List<VariationVertexPair> compatiblePairs = new ArrayList<VariationVertexPair>();

		// Generate enough incompatible and compatible patient-donor pair vertices
		while(incompatiblePairs.size() < numPairs) {

			VariationVertexPair v = generatePair(currentVertexID++);
			if(v.isCompatible()) {
				compatiblePairs.add(v);  // we don't do anything with these
				currentVertexID--;       // throw away compatible pair; reuse the ID
			} else {
				incompatiblePairs.add(v);
			}
		}

		// Only add the incompatible pairs to the pool
		Pool pool = new Pool(Edge.class);
		for(VariationVertexPair pair : incompatiblePairs) {
			pool.addPair(pair);
		}

		// Add edges between compatible donors and other patients
		for(VariationVertexPair donorPair : incompatiblePairs) {
			for(VariationVertexPair patientPair : incompatiblePairs) {

				if(donorPair.equals(patientPair)) { continue; }

				if(isCompatible(donorPair, patientPair)) {
					Edge e = pool.addEdge(donorPair, patientPair);
					pool.setEdgeWeight(e, 1.0);			//All edges have weight 1.0
				}
			}
		}

		return pool;
	}

	// Adds vertices with edge weights of 1.0
	public Set<Vertex> addVerticesToPool(Pool pool, int numPairs, int numAltruists) {
		System.out.println("ERROR: Variation package does not support altruists.");
		return addVerticesToPool(pool, numPairs);
	}

	// Adds vertices with "equal" edge weights
	public Set<Vertex> addVerticesToPool(Pool pool, int numPairs) {

		//Weights of all non-dummy edges = 1
		double weight = 1;

		// Generate new vertices
		Pool more = this.generate(numPairs, 0);

		// Add edges from/to the new vertices
		for(VertexPair v : more.getPairs()) { pool.addPair(v); }
		for(VertexPair vN : more.getPairs()) {

			for(VertexPair vO : pool.getPairs()) {
				if(vN.equals(vO)) { continue; }  // Don't add self-edges

				// Donate from new vertex to other vertex
				if(isCompatible(vN, vO) && !pool.containsEdge(vN, vO)) {
					pool.setEdgeWeight(pool.addEdge(vN, vO), weight);
				}
				// Donate from other vertex to new vertex
				if(isCompatible(vO, vN)&& !pool.containsEdge(vO, vN)) {
					pool.setEdgeWeight(pool.addEdge(vO, vN), weight);
				}
			}
		}

		// Return only the new vertices that were generated
		return more.vertexSet();
	}


	// Adds vertices with "ethical" edge weights
	public Set<Vertex> addVerticesWithPatientEdgeWeights(Pool pool, int numPairs) {

		// Generate new vertices
		Pool more = this.generate(numPairs, 0);

		// Add edges from/to the new vertices
		for(VertexPair v : more.getPairs()) { pool.addPair(v); }
		for(VertexPair vN : more.getPairs()) {
			for(VertexPair vO : pool.getPairs()) {
				if(vN.equals(vO)) { continue; }  // Don't add self-edges

				// Donate from new vertex to other vertex
				if(isCompatible(vN, vO) && !pool.containsEdge(vN, vO)) {
					double weight = ((VariationVertexPair) vO).getPatientWeight();
					pool.setEdgeWeight(pool.addEdge(vN, vO), weight);
				}
				// Donate from other vertex to new vertex
				if(isCompatible(vO, vN)&& !pool.containsEdge(vO, vN)) {
					double weight = ((VariationVertexPair) vN).getPatientWeight();
					pool.setEdgeWeight(pool.addEdge(vO, vN), weight);
				}
			}
		}
		
		// Return only the new vertices that were generated
		return more.vertexSet();
	}

	// Adds vertices with "variation" edge weights
	public Set<Vertex> addVerticesWithDonorPatientEdgeWeights(Pool pool, int numPairs) {

		// Generate new vertices
		Pool more = this.generate(numPairs, 0);

		// Add edges from/to the new vertices
		for(VertexPair v : more.getPairs()) { pool.addPair(v); }
		for(VertexPair vN : more.getPairs()) {
			for(VertexPair vO : pool.getPairs()) {
				if(vN.equals(vO)) { continue; }  // Don't add self-edges

				// Donate from new vertex to other vertex
				if(isCompatible(vN, vO) && !pool.containsEdge(vN, vO)) {
					double weight = ((VariationVertexPair) vN).getDonorPatientWeight((VariationVertexPair) vO);
					pool.setEdgeWeight(pool.addEdge(vN, vO), weight);
				}
				// Donate from other vertex to new vertex
				if(isCompatible(vO, vN)&& !pool.containsEdge(vO, vN)) {
					double weight = ((VariationVertexPair) vO).getDonorPatientWeight((VariationVertexPair) vN);
					pool.setEdgeWeight(pool.addEdge(vO, vN), weight);
				}
			}
		}

		// Return only the new vertices that were generated
		return more.vertexSet();
	}

	public void close(long seed) {
		this.out.println("SEED: "+seed+",,,");
		this.out.close();
	}

}