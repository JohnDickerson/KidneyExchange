package edu.cmu.cs.dickerson.kpd.structure.real;

import java.util.Map;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
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
		
		// HOME_CTR	trav_centers	region	dsa	DON_KI_TYPE	HBC_AND_HBSAG	CREAT_BSA	BP_SYSTOLIC	BP_diastolic	BMI	CMV	EBV	DON_KI_SHIP	MAX_PAIRS_CYCLE	MAX_PAIRS_CHAIN"
		// TODO
		
		// UNOS coding
		Integer kpdPairID = Integer.valueOf(line[headers.get("KPD_PAIR_ID")]);
		Integer kpdCandidateID = Integer.valueOf(line[headers.get("KPD_CANDIDATE_ID")]);
		Integer kpdDonorID = Integer.valueOf(line[headers.get("KPD_DONOR_ID")]);
		boolean nonDirectedDonor = IOUtil.stringToBool(line[headers.get("NON_DIRECTED_DONOR")]);
		
		// Blood and tissue-type
		BloodType abo = BloodType.getBloodType(line[headers.get("ABO")]);
		int a1 = Integer.valueOf(line[headers.get("A1")]);
		int a2 = Integer.valueOf(line[headers.get("A2")]);
		int b1 = Integer.valueOf(line[headers.get("B1")]);
		int b2 = Integer.valueOf(line[headers.get("B2")]);
		boolean bw4 = IOUtil.stringToBool(line[headers.get("BW4")]);
		boolean bw6 = IOUtil.stringToBool(line[headers.get("BW6")]);
		int cw1 = Integer.valueOf(line[headers.get("CW1")]);
		int cw2 = Integer.valueOf(line[headers.get("CW2")]);
		int dq1 = Integer.valueOf(line[headers.get("DQ1")]);
		int dq2 = Integer.valueOf(line[headers.get("DQ2")]);
		int dr1 = Integer.valueOf(line[headers.get("DR1")]);
		int dr2 = Integer.valueOf(line[headers.get("DR2")]);
		boolean dr51 = IOUtil.stringToBool(line[headers.get("DR51")]);
		boolean dr52 = IOUtil.stringToBool(line[headers.get("DR52")]);
		boolean dr53 = IOUtil.stringToBool(line[headers.get("DR53")]);
		int dp1 = Integer.valueOf(line[headers.get("DP1")]);
		int dp2 = Integer.valueOf(line[headers.get("DP2")]);
		
		// Social characteristics and preferences
		int age = Integer.valueOf(line[headers.get("AGE")]);
		
		return null;
	}
}
