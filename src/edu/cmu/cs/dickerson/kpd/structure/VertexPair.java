package edu.cmu.cs.dickerson.kpd.structure;

import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class VertexPair extends Vertex {
	
	// Blood types for the patient and donor in the pair
	private final BloodType bloodTypePatient;
	private final BloodType bloodTypeDonor;
	
	// Patient's calculated probability of positive crossmatch, scaled [0,1]
	private final double patientCPRA;
	
	// Patient is wife of the donor (affects HLA)
	private final boolean isWifePatient;

	// Is the donor compatible with the patient
	private final boolean isCompatible;
	
	public VertexPair(int ID, BloodType bloodTypePatient, BloodType bloodTypeDonor, boolean isWifePatient, double patientCPRA, boolean isCompatible) {
		super(ID);
		this.bloodTypePatient = bloodTypePatient;
		this.bloodTypeDonor = bloodTypeDonor;
		this.isWifePatient = isWifePatient;
		this.patientCPRA = patientCPRA;
		this.isCompatible = isCompatible;
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
}
