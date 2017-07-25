package edu.cmu.cs.dickerson.kpd.Ethics;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexAltruist;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;


public class EthicalPoolGenerator extends PoolGenerator {
	
	//Which weights version from EthicalVertexPair to use
	int weightsVersion;
	
	//"Ethically relevant" demographics
	protected double Pr_YOUNG = 0.275;
	protected double Pr_YOUNG_NONALCOHOLIC = 0.728;
	protected double Pr_OLD_NONALCOHOLIC = 0.828;
	protected double Pr_YOUNG_HEALTHY = 0.9999063;
	protected double Pr_OLD_HEALTHY = 0.999275;
	
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

	public EthicalPoolGenerator(Random random, int weightsVersion) {
		super(random);
		this.currentVertexID = 0;
		this.weightsVersion = weightsVersion;
		
		//Initialize output file
		try {
			this.out = new PrintWriter("vertices_" + weightsVersion + ".csv");
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
	private EthicalVertexPair generatePair(int ID) {

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
		
		EthicalVertexPair v = new EthicalVertexPair(ID, bloodTypePatient, bloodTypeDonor, isWifePatient, patientCPRA, 
				compatible, isYoung, isNonalcoholic, isHealthy, this.weightsVersion);
		
		//Write generated vertex to output file
		this.out.println(this.vertexCount+","+v.getBloodID()+","+rAge+","+rAlcohol+","+rHealth);
		this.vertexCount++;
		
		return v;
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
		List<EthicalVertexPair> incompatiblePairs = new ArrayList<EthicalVertexPair>();
		List<EthicalVertexPair> compatiblePairs = new ArrayList<EthicalVertexPair>();
		List<VertexAltruist> altruists = new ArrayList<VertexAltruist>();

		// Generate enough incompatible and compatible patient-donor pair vertices
		while(incompatiblePairs.size() < numPairs) {

			EthicalVertexPair v = generatePair(currentVertexID++);
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
		for(EthicalVertexPair pair : incompatiblePairs) {
			pool.addPair(pair);	
		}

		// Add altruists to the pool
		for(VertexAltruist altruist : altruists) {
			pool.addAltruist(altruist);
		}

		// Add edges between compatible donors and other patients
		for(EthicalVertexPair donorPair : incompatiblePairs) {
			for(EthicalVertexPair patientPair : incompatiblePairs) {

				if(donorPair.equals(patientPair)) { continue; }

				if(isCompatible(donorPair, patientPair)) {
					Edge e = pool.addEdge(donorPair, patientPair);
					pool.setEdgeWeight(e, 1.0);			//All edges have weight 1.0
				}
			}
		}

		for(VertexAltruist alt : altruists) {
			for(EthicalVertexPair patientPair : incompatiblePairs) {

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

	// Adds vertices with edge weights of 1.0
	public Set<Vertex> addVerticesToPool(Pool pool, int numPairs, int numAltruists) {
		
		//Weights of all non-dummy edges = 1
		double weight = 1;
		
		// Generate new vertices
		Pool more = this.generate(numPairs, numAltruists);
		
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
			
			// Adds edges from old altruists to new vertices
			for(VertexAltruist altO : pool.getAltruists()) {
				if(isCompatible(altO, vN)) {
					pool.setEdgeWeight(pool.addEdge(altO, vN), weight);
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
					pool.setEdgeWeight(pool.addEdge(altN, v), weight);
				}
				
				// Add dummy edges from a non-altruist donor to each of the altruists
				pool.setEdgeWeight(pool.addEdge(v, altN), 0.0);
			}
		}
		
		// Return only the new vertices that were generated
		return more.vertexSet();
	}

	
	// Adds vertices with "ethical" edge weights
	public Set<Vertex> addSpecialVerticesToPool(Pool pool, int numPairs, int numAltruists) {
		
		// Generate new vertices
		Pool more = this.generate(numPairs, numAltruists);
		
		// Add edges from/to the new vertices
		for(VertexPair v : more.getPairs()) { pool.addPair(v); }
		for(VertexPair vN : more.getPairs()) {
			for(VertexPair vO : pool.getPairs()) {
				if(vN.equals(vO)) { continue; }  // Don't add self-edges
				
				// Donate from new vertex to other vertex
				if(isCompatible(vN, vO) && !pool.containsEdge(vN, vO)) {
					double weight = ((EthicalVertexPair) vO).getWeight();
					pool.setEdgeWeight(pool.addEdge(vN, vO), weight);
				}
				// Donate from other vertex to new vertex
				if(isCompatible(vO, vN)&& !pool.containsEdge(vO, vN)) {
					double weight = ((EthicalVertexPair) vN).getWeight();
					pool.setEdgeWeight(pool.addEdge(vO, vN), weight);
				}
			}
			
			// Adds edges from old altruists to new vertices
			for(VertexAltruist altO : pool.getAltruists()) {
				if(isCompatible(altO, vN)) {
					double weight = ((EthicalVertexPair) vN).getWeight();
					pool.setEdgeWeight(pool.addEdge(altO, vN), weight);
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
					double weight = ((EthicalVertexPair) v).getWeight();
					pool.setEdgeWeight(pool.addEdge(altN, v), weight);
				}
				
				// Add dummy edges from a non-altruist donor to each of the altruists
				pool.setEdgeWeight(pool.addEdge(v, altN), 0.0);
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