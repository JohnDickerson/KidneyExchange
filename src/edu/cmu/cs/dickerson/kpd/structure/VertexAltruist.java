package edu.cmu.cs.dickerson.kpd.structure;

import edu.cmu.cs.dickerson.kpd.structure.real.UNOSPair;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class VertexAltruist extends Vertex {

	// Altruist's blood type
	private final BloodType bloodTypeDonor;

	
	/**
	 * Constructor for SIMULATED data (Saidman, Heterogeneous, etc)
	 * @param ID
	 * @param bloodTypeDonor
	 */
	public VertexAltruist(int ID, BloodType bloodTypeDonor) {
		super(ID);
		this.bloodTypeDonor = bloodTypeDonor;
	}
	
	/**
	 * Constructor for drawing from REAL data (UNOS, non-simulated)
	 * @param ID
	 * @param underlyingPair
	 */
	public VertexAltruist(int ID, UNOSPair underlyingPair) {
		super(ID, underlyingPair);
		this.bloodTypeDonor = BloodType.O;
	}
	
	@Override
	public boolean isAltruist() {
		return true;
	}

	public BloodType getBloodTypeDonor() {
		return bloodTypeDonor;
	}
}
