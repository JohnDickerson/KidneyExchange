package edu.cmu.cs.dickerson.kpd.Ethics;

import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSPair;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class EthicalVertexPair extends VertexPair {
	
	//Store profile type
	private int profileID;
	
	//Store profile and blood type
	private String bloodID;
	
	//"Special weight" stored
	private double weight;
	
	//"Ethical" demographics
	private boolean isYoung;		
	private boolean isNonalcoholic;
	private boolean isHealthy;
	
	//Set to true to use directly estimated patient weights, set to false to derive from characteristic weights (weights2)
	private boolean usePatientWeights = true;
	
	//Weights used by "special" algorithm to prioritize pairs
	double[] weights;
	//default
	double[] weights1 = {1.000000000, 0.103243396, 0.236280167, 0.035722844, 0.070045054, 0.011349772, 0.024072427, 0.002769801};
	
	//alternates
	double[] weights0 = {1, 1, 1, 1, 1, 1, 1, 1};
	double[] weights3 = {1, 0.998, 0.999, 0.996, 0.997, 0.994, 0.995, 0.993};
	double[] weights4 = {0.8, 0.6, 0.4, 0.3, 1, 0.7, 0.9, 0.5};
	double[] weights5 = {0.5, 0.7, 0.9, 1, 0.3, 0.6, 0.4, 0.8};
	double[] weights6 = {1, 0.8, 0.9, 0.6, 0.7, 0.4, 0.5, 0.3};
	double[] weights7 = {1, 0.978697374, 0.991420029, 0.932724747, 0.967046957, 0.766489634, 0.899526405, 0.002769801};
	
	//Characteristic weights estimated by Bradley-Terry model (in R script)
	double weight_age = -2.419075;
	double weight_alcohol = -2.026236;
	double weight_cancer = -1.234208;
	
	public EthicalVertexPair(int ID, BloodType bloodTypePatient, BloodType bloodTypeDonor, 
			boolean isWifePatient, double patientCPRA, boolean isCompatible, boolean isYoung,
			boolean isNonalcoholic, boolean isHealthy, int weightsType) {
		super(ID, bloodTypePatient, bloodTypeDonor, isWifePatient, patientCPRA, isCompatible);
		this.isYoung = isYoung;
		this.isNonalcoholic = isNonalcoholic;
		this.isHealthy = isHealthy;
		this.weight = calcWeight(weightsType);
		setProfileID();
		setBloodID();
		//System.out.println("ID: "+this.profileID+" age: "+this.isYoung+" alc: "+this.isNonalcoholic+" hlth: "+this.isHealthy);
	}
	
	public EthicalVertexPair(int ID, BloodType bloodTypePatient, BloodType bloodTypeDonor, 
			boolean isWifePatient, double patientCPRA, boolean isCompatible, boolean isYoung,
			boolean isNonalcoholic, boolean isHealthy) {
		this(ID, bloodTypePatient, bloodTypeDonor, isWifePatient, patientCPRA, isCompatible, 
				isYoung, isNonalcoholic, isHealthy, 1);
		System.out.println("\t\t\t\t\tDEFAULT WEIGHTS USED.");
	}
	
	public EthicalVertexPair(int ID, UNOSPair underlyingPair) {
		super(ID, BloodType.O, BloodType.O, false, underlyingPair.getRecipient().cpra, false);
	}
	
	// bloodID = profile ID# - patient blood type - donor blood type (ex. "1_AB_O")
	private void setBloodID() {
		String patient = this.bloodTypePatient.toString();
		String donor = this.bloodTypeDonor.toString();
		this.bloodID = profileID + "_" + patient + "_" + donor;
		//System.out.print("\t\t\t\tBlood ID: "+this.bloodID);
		//if (!this.isCompatible) { System.out.println("  (incompatible)"); }
		//else { System.out.println(""); }
	}
	
	public String getBloodID() {
		return this.bloodID;
	}
	
	/**
	 * Calculates the special weight of the EthicalVertexPair, based
	 * on their "ethical" characteristics (age, alcohol, health)
	 * @return vertex weight 
	 */
	private double calcWeight(int weightsType) {
		if (weightsType == 2) { return calcCharacteristicWeight(); }
		if (weightsType == 0) { this.weights = this.weights0; }
		if (weightsType == 1) { this.weights = this.weights1; }
		if (weightsType == 3) { this.weights = this.weights3; }
		if (weightsType == 4) { this.weights = this.weights4; }
		if (weightsType == 5) { this.weights = this.weights5; }
		if (weightsType == 6) { this.weights = this.weights6; }
		if (weightsType == 7) { this.weights = this.weights7; }
		return calcPatientWeight();
	}
	
	/**
	 * Returns weight corresponding to patient profile
	 * @return vertex weight 
	 */
	private double calcPatientWeight() {
		if (this.isYoung) {
			if (this.isNonalcoholic) {
				if (this.isHealthy) {
					return this.weights[0];		// 1 - 000
				} else {
					return this.weights[2];		// 3 - 001
				}
			} else {
				if (this.isHealthy) {
					return this.weights[1];		// 2 - 010
				} else {
					return this.weights[3];		// 4 - 011
				}
			}
		} else {
			if (this.isNonalcoholic) {
				if (this.isHealthy) {
					return this.weights[4];		// 5 - 100
				} else {
					return this.weights[6];		// 7 - 101
				}
			} else {
				if (this.isHealthy) {
					return this.weights[5];		// 6 - 110
				} else {
					return this.weights[7];		// 8 - 111
				}
			}
		}
	}
	
	/**
	 * Calculates vertex weight as a function of the patient's characteristics
	 * True = 1, False = 0
	 * Patient weight = e^(weight_age*isYoung + weight_alcohol*isNonalcoholic + weight_cancer*isHealthy)
	 * @return vertex weight 
	 */
	private double calcCharacteristicWeight() {
		double log_score = 0;
		if (!isYoung) {
			log_score += weight_age;
		}
		if (!isNonalcoholic) {
			log_score += weight_alcohol;
		}
		if (!isHealthy) {
			log_score += weight_cancer;
		}
		return Math.exp(log_score);
	}
	
	private void setProfileID() {
		if (this.isYoung) {
			if (this.isNonalcoholic) {
				if (this.isHealthy) {
					this.profileID = 1;		//000
				} else {
					this.profileID = 3;		//001
				}
			} else {
				if (this.isHealthy) {
					this.profileID = 2;		//010
				} else {
					this.profileID = 4;		//011
				}
			}
		} else {
			if (this.isNonalcoholic) {
				if (this.isHealthy) {
					this.profileID = 5;		//100
				} else {
					this.profileID = 7;		//101
				}
			} else {
				if (this.isHealthy) {
					this.profileID = 6;		//110
				} else {
					this.profileID = 8;		//111
				}
			}
		}
	}
	
	public int getProfileID() {
		return this.profileID;
	}
	
	public double getWeight() {
		return this.weight;
	}
	
	@Override
	public boolean isAltruist() {
		return false;
	}

	public BloodType getBloodTypePatient() {
		return bloodTypePatient;
	}

	public BloodType getBloodTypeDonor() {
		return bloodTypeDonor;
	}

	public double getPatientCPRA() {
		return patientCPRA;
	}

	public boolean isWifePatient() {
		return isWifePatient;
	}

	public boolean isCompatible() {
		return isCompatible;
	}

	public void setBloodTypePatient(BloodType bloodTypePatient) {
		this.bloodTypePatient = bloodTypePatient;
	}

	public void setBloodTypeDonor(BloodType bloodTypeDonor) {
		this.bloodTypeDonor = bloodTypeDonor;
	}
	
	public static void main(String[] args) {

		boolean[] profs = {true, true, true, true, false, true, true, true, false,
				true, false, false, false, true, true, false, false, true,
				false, true, false, false, false, false};
		System.out.println("\t\t\t1\t\t2\t\t3\t\t4\t\t5\t\t6\t\t7\t\t8");
		for (int weightsType = 0; weightsType < 6; weightsType++) {
			System.out.print("Weights type: "+weightsType+"\t\t");
			for (int prof = 1; prof < 9; prof++) {
				EthicalVertexPair v = new EthicalVertexPair(1, null, null, true, 0.5, true,
						profs[(3*(prof-1))], profs[(3*(prof-1))+1], profs[(3*(prof-1))+2], weightsType);
				//System.out.println((3*(prof-1))+" "+((3*(prof-1))+1)+" "+((3*(prof-1))+2));
				System.out.printf("%.3f", v.getWeight());
				System.out.print("\t\t");
			} System.out.println("");
		}
	}
}
