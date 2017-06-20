package edu.cmu.cs.dickerson.kpd.structure;

import edu.cmu.cs.dickerson.kpd.structure.real.UNOSPair;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class VertexPair extends Vertex {
	
	// Blood types for the patient and donor in the pair
	protected BloodType bloodTypePatient;
	protected BloodType bloodTypeDonor;
	
	// Patient's calculated probability of positive crossmatch, scaled [0,1]
	protected final double patientCPRA;
	
	// Patient is wife of the donor (affects HLA)
	protected final boolean isWifePatient;

	// Is the donor compatible with the patient
	protected final boolean isCompatible;
	
	public VertexPair(int ID, BloodType bloodTypePatient, BloodType bloodTypeDonor, boolean isWifePatient, double patientCPRA, boolean isCompatible) {
		super(ID);
		this.bloodTypePatient = bloodTypePatient;
		this.bloodTypeDonor = bloodTypeDonor;
		this.isWifePatient = isWifePatient;
		this.patientCPRA = patientCPRA;
		this.isCompatible = isCompatible;
	}
	
	public VertexPair(int ID, UNOSPair underlyingPair) {
		super(ID, underlyingPair);
		this.bloodTypePatient = BloodType.O;
		this.bloodTypeDonor = BloodType.O;
		this.isWifePatient = false;
		this.patientCPRA = underlyingPair.getRecipient().cpra;
		this.isCompatible = false;
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
	
	
}
