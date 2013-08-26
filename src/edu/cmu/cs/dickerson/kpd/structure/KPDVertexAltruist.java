package edu.cmu.cs.dickerson.kpd.structure;

import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class KPDVertexAltruist extends KPDVertex {

	// Altruist's blood type
	private final BloodType bloodTypeDonor;

	public KPDVertexAltruist(int ID, BloodType bloodTypeDonor) {
		super(ID);
		this.bloodTypeDonor = bloodTypeDonor;
	}
	
	@Override
	public boolean isAltruist() {
		return true;
	}

	public BloodType getBloodTypeDonor() {
		return bloodTypeDonor;
	}
}
