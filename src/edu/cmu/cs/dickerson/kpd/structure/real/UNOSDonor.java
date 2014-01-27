package edu.cmu.cs.dickerson.kpd.structure.real;

import java.util.Map;

import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;


public class UNOSDonor implements Comparable<UNOSDonor> {

	// This is a UNIQUE ID assigned by UNOS; if any other donor is loaded with ID,
	// it assumed to be EQUAL to this donor
	private final Integer kpdDonorID;
	
	// Replicas of the headers in the original .csv file
	private Integer kpdPairID;
	private Integer kpdCandidateID;
	private boolean nonDirectedDonor;
	private BloodType abo;
	
	public UNOSDonor(Integer kpd_donor_id) {
		this.kpdDonorID = kpd_donor_id;
	}
	
	public boolean canDonateTo(UNOSRecipient recipient) {
		return false;
	}

	
	public Integer getKPDPairID() {
		return kpdPairID;
	}

	public void setKPDPairID(Integer kpdPairID) {
		this.kpdPairID = kpdPairID;
	}

	public Integer getKPDCandidateID() {
		return kpdCandidateID;
	}

	public void setKPDCandidateID(Integer kpdCandidateID) {
		this.kpdCandidateID = kpdCandidateID;
	}

	public boolean isNonDirectedDonor() {
		return nonDirectedDonor;
	}

	public void setNonDirectedDonor(boolean nonDirectedDonor) {
		this.nonDirectedDonor = nonDirectedDonor;
	}

	public BloodType getABO() {
		return abo;
	}

	public void setABO(BloodType abo) {
		this.abo = abo;
	}

	public Integer getKPDDonorID() {
		return kpdDonorID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + kpdDonorID;
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
		if (kpdDonorID != other.kpdDonorID) {
			return false;
		}
		return true;
	}
	
	@Override
	public int compareTo(UNOSDonor d) {
		return this.getKPDDonorID().compareTo(d.getKPDDonorID());
	}

	public static UNOSDonor makeUNOSDonor(String[] line, Map<String, Integer> headers) {
		// TODO Auto-generated method stub
		return null;
	}
}
