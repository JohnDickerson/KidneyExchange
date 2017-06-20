package edu.cmu.cs.dickerson.kpd.Ethics;

import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSPair;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class EthicalVertexPair extends VertexPair {
	
	private int profileID;
	
	//"Special weight" stored
	private double weight;
	
	//"Ethical" demographics
	private boolean isYoung;		
	private boolean isNonalcoholic;
	private boolean isHealthy;
	
	//Set to true to use directly estimated patient weights, set to false to derive from characteristic weights
	private boolean usePatientWeights = true;
	
	//Patient weights estimated by Bradley-Terry model (in R script)
	double weight_000 = 1;//1.000000000;	//young, sober, healthy
	double weight_001 = 1;//0.236280167;	//young, sober, cancer
	double weight_010 = 1;//0.103243396;	//young, alcohol, healthy
	double weight_100 = 1;//0.070045054;	//old, sober, healthy
	double weight_011 = 1;//0.035722844;	//young, alcohol, cancer
	double weight_101 = 1;//0.024072427;	//old, sober, cancer
	double weight_110 = 1;//0.011349772;	//old, alcohol, healthy
	double weight_111 = 1;//0.002769801;	//old, alcohol, cancer
	
	//Characteristic weights estimated by Bradley-Terry model (in R script)
	double weight_age = -2.419075;
	double weight_alcohol = -2.026236;
	double weight_cancer = -1.234208;
	
	//Track number of iterations spent in pool before matched
	int timeInPool;
	
	public EthicalVertexPair(int ID, BloodType bloodTypePatient, BloodType bloodTypeDonor, 
			boolean isWifePatient, double patientCPRA, boolean isCompatible, boolean isYoung,
			boolean isNonalcoholic, boolean isHealthy) {
		super(ID, bloodTypePatient, bloodTypeDonor, isWifePatient, patientCPRA, isCompatible);
		this.isYoung = isYoung;
		this.isNonalcoholic = isNonalcoholic;
		this.isHealthy = isHealthy;
		this.weight = calcWeight();
		this.timeInPool = 0;
		setProfileID();
		
	}
	
	public EthicalVertexPair(int ID, UNOSPair underlyingPair) {
		super(ID, BloodType.O, BloodType.O, false, underlyingPair.getRecipient().cpra, false);
	}
	
	/**
	 * Calculates the special weight of the EthicalVertexPair, based
	 * on their "ethical" characteristics (age, alcohol, health)
	 * @return vertex weight 
	 */
	private double calcWeight() {
		if (this.usePatientWeights) {
			return calcPatientWeight();
		}
		return calcCharacteristicWeight();
	}
	
	/**
	 * Returns weight corresponding to patient profile
	 * @return vertex weight 
	 */
	private double calcPatientWeight() {
		if (this.isYoung) {
			if (this.isNonalcoholic) {
				if (this.isHealthy) {
					return this.weight_000;
				} else {
					return this.weight_001;
				}
			} else {
				if (this.isHealthy) {
					return this.weight_010;
				} else {
					return this.weight_011;
				}
			}
		} else {
			if (this.isNonalcoholic) {
				if (this.isHealthy) {
					return this.weight_100;
				} else {
					return this.weight_101;
				}
			} else {
				if (this.isHealthy) {
					return this.weight_110;
				} else {
					return this.weight_111;
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
					this.profileID = 1;
				} else {
					this.profileID = 3;
				}
			} else {
				if (this.isHealthy) {
					this.profileID = 2;
				} else {
					this.profileID = 4;
				}
			}
		} else {
			if (this.isNonalcoholic) {
				if (this.isHealthy) {
					this.profileID = 5;
				} else {
					this.profileID = 7;
				}
			} else {
				if (this.isHealthy) {
					this.profileID = 6;
				} else {
					this.profileID = 8;
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
		EthicalVertexPair v1 = new EthicalVertexPair(1, null, null, true, 0.5, true,
				true, true, true);
		double p1 = v1.calcPatientWeight();
		double c1 = v1.calcCharacteristicWeight();
		System.out.println("\nPatient 1");
		System.out.println("\tProfile weight: "+p1);
		System.out.println("\tCharacteristic weight: "+c1);
		
		EthicalVertexPair v2 = new EthicalVertexPair(1, null, null, true, 0.5, true,
				true, false, true);
		double p = v2.calcPatientWeight();
		double c = v2.calcCharacteristicWeight();
		System.out.println("\nPatient 2");
		System.out.println("\tProfile weight: "+p);
		System.out.println("\tCharacteristic weight: "+c);
		
		EthicalVertexPair v3 = new EthicalVertexPair(1, null, null, true, 0.5, true,
				true, true, false);
		p = v3.calcPatientWeight();
		c = v3.calcCharacteristicWeight();
		System.out.println("\nPatient 3");
		System.out.println("\tProfile weight: "+p);
		System.out.println("\tCharacteristic weight: "+c);
		
		EthicalVertexPair v4 = new EthicalVertexPair(1, null, null, true, 0.5, true,
				true, false, false);
		p = v4.calcPatientWeight();
		c = v4.calcCharacteristicWeight();
		System.out.println("\nPatient 4");
		System.out.println("\tProfile weight: "+p);
		System.out.println("\tCharacteristic weight: "+c);
		
		EthicalVertexPair v5 = new EthicalVertexPair(1, null, null, true, 0.5, true,
				false, true, true);
		p = v5.calcPatientWeight();
		c = v5.calcCharacteristicWeight();
		System.out.println("\nPatient 5");
		System.out.println("\tProfile weight: "+p);
		System.out.println("\tCharacteristic weight: "+c);
		
		EthicalVertexPair v6 = new EthicalVertexPair(1, null, null, true, 0.5, true,
				false, false, true);
		p = v6.calcPatientWeight();
		c = v6.calcCharacteristicWeight();
		System.out.println("\nPatient 6");
		System.out.println("\tProfile weight: "+p);
		System.out.println("\tCharacteristic weight: "+c);
		
		EthicalVertexPair v7 = new EthicalVertexPair(1, null, null, true, 0.5, true,
				false, true, false);
		p = v7.calcPatientWeight();
		c = v7.calcCharacteristicWeight();
		System.out.println("\nPatient 7");
		System.out.println("\tProfile weight: "+p);
		System.out.println("\tCharacteristic weight: "+c);
		
		EthicalVertexPair v8 = new EthicalVertexPair(1, null, null, true, 0.5, true,
				false, false, false);
		p = v8.calcPatientWeight();
		c = v8.calcCharacteristicWeight();
		System.out.println("\nPatient 8");
		System.out.println("\tProfile weight: "+p);
		System.out.println("\tCharacteristic weight: "+c);
	}
}
