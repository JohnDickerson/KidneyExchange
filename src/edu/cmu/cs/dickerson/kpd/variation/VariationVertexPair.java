package edu.cmu.cs.dickerson.kpd.variation;

import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSPair;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class VariationVertexPair extends VertexPair {

	//Store profile type of patient
	private int profileID;
	
	//Store profile and blood type
	private String bloodID;
	
	//"Special weight" stored
	private double weight;

	//"Ethical" demographics
	private boolean isYoung;		
	private boolean isNonalcoholic;
	private boolean isHealthy;

	//Store BLP model;
	private BLPModel blpModel;
	
	// precomputed weights from BT model
	double[] weights = {1.000000000, 0.103243396, 0.236280167, 0.035722844, 0.070045054, 0.011349772, 0.024072427, 0.002769801};

	public VariationVertexPair(int ID, BloodType bloodTypePatient, BloodType bloodTypeDonor,
							   boolean isWifePatient, double patientCPRA, boolean isCompatible, boolean isYoung,
							   boolean isNonalcoholic, boolean isHealthy, int weightsType) {
		super(ID, bloodTypePatient, bloodTypeDonor, isWifePatient, patientCPRA, isCompatible);
		this.isYoung = isYoung;
		this.isNonalcoholic = isNonalcoholic;
		this.isHealthy = isHealthy;
		this.weight = calcPatientWeight();
		setProfileID();
		setBloodID();
		this.blpModel = new BLPModel();
	}
	
	public VariationVertexPair(int ID, BloodType bloodTypePatient, BloodType bloodTypeDonor,
							   boolean isWifePatient, double patientCPRA, boolean isCompatible, boolean isYoung,
							   boolean isNonalcoholic, boolean isHealthy) {
		this(ID, bloodTypePatient, bloodTypeDonor, isWifePatient, patientCPRA, isCompatible, 
				isYoung, isNonalcoholic, isHealthy, 1);
		System.out.println("\t\t\t\t\tDEFAULT WEIGHTS USED.");
	}
	
	public VariationVertexPair(int ID, UNOSPair underlyingPair) {
		super(ID, BloodType.O, BloodType.O, false, underlyingPair.getRecipient().cpra, false);
	}
	
	// bloodID = profile ID# - patient blood type - donor blood type (ex. "1_AB_O")
	private void setBloodID() {
		String patient = this.bloodTypePatient.toString();
		String donor = this.bloodTypeDonor.toString();
		this.bloodID = profileID + "_" + patient + "_" + donor;
	}
	
	public String getBloodID() {
		return this.bloodID;
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

	/*
	 * Get edge weight when this vertex is the recipient
	 * Uses BT model
	 */
	public double getPatientWeight() {
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

	/*
	 * Get edge weight when this vertex is the donor and toVertex is the recipient
	 * Uses BLP model
	 */
	public double getDonorPatientWeight(VariationVertexPair toVertex) {
		return this.blpModel.getWeight(toVertex.profileID);
	}

	public int getRank(VariationVertexPair toVertex) {
		return this.blpModel.getRank(toVertex.profileID);
	}
}
