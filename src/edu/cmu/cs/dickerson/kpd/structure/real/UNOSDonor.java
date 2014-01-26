package edu.cmu.cs.dickerson.kpd.structure.real;


public class UNOSDonor implements Comparable<UNOSDonor> {

	// This is a UNIQUE ID assigned by UNOS; if any other donor is loaded with ID,
	// it assumed to be EQUAL to this donor
	protected final Integer ID;
	
	public UNOSDonor(Integer ID) {
		this.ID = ID;
	}
	
	public boolean canDonateTo(UNOSRecipient recipient) {
		return false;
	}

	public Integer getDonorID() {
		return ID;
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		UNOSDonor other = (UNOSDonor) obj;
		if (ID != other.ID) {
			return false;
		}
		return true;
	}
	
	@Override
	public int compareTo(UNOSDonor d) {
		return this.getDonorID().compareTo(d.getDonorID());
	}
}
