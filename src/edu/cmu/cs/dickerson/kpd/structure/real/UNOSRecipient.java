package edu.cmu.cs.dickerson.kpd.structure.real;

import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class UNOSRecipient implements Comparable<UNOSRecipient> {

	// This is a UNIQUE ID assigned by UNOS; if any other recipient is loaded with ID,
	// it assumed to be EQUAL to this recipient
	public final String kpdCandidateID;

	public UNOSRecipient(String kpdCandidateID) {
		this.kpdCandidateID = kpdCandidateID;
	}

	// Replicas of the headers in the original .csv file
	public String kpdPairID;
	public int age;
	public BloodType abo;
	public boolean a2Eligible, a2bEligible;
	public int a1, a2, b1, b2, dr1, dr2;
	public Set<String> unacceptableAntigens;
	public Set<String> undesirableAntigens;
	public boolean pedCandidate;
	public int numKPDMatchRuns;
	public String transplantCtr;
	public Set<String> travCenters;
	public Set<String> shipCenters;
	public int region;
	public String dsa;
	public String acceptKiType;
	public boolean priorLivingDonor;
	public int minDonorAge, maxDonorAge;
	public boolean acceptHepbPos;
	public boolean highlySensitized;
	public double minDonorCreat, maxDonorBPSystolic, maxDonorBPDiastolic, maxDonorBMI;
	public boolean acceptCMVPos, acceptEBVPos;
	public double cpra;

	public static UNOSRecipient makeUNOSRecipient(String[] line, Map<String, Integer> headers) {

		// UNOS coding
		String kpdCandidateID = line[headers.get("KPD_CANDIDATE_ID")].toUpperCase().trim();
		UNOSRecipient r = new UNOSRecipient(kpdCandidateID);
		r.kpdPairID = line[headers.get("KPD_PAIR_ID")].toUpperCase().trim();

		// Blood and tissue-type
		String aboHeader = headers.containsKey("ABO BLOOD GROUP") ? "ABO BLOOD GROUP" : "ABO";
		r.abo = BloodType.getBloodType(line[headers.get(aboHeader)]);

		r.a1 = Integer.parseInt(line[headers.get("A1")]);
		r.a2 = Integer.parseInt(line[headers.get("A2")]);
		r.b1 = Integer.parseInt(line[headers.get("B1")]);
		r.b2 = Integer.parseInt(line[headers.get("B2")]);
		r.dr1 = Integer.parseInt(line[headers.get("DR1")]);
		r.dr2 = Integer.parseInt(line[headers.get("DR2")]);
		r.a2Eligible = IOUtil.stringToBool(line[headers.get("A2_ELIGIBLE")]);
		r.a2bEligible = IOUtil.stringToBool(line[headers.get("A2B_ELIGIBLE")]);
		r.dr1 = Integer.parseInt(line[headers.get("DR1")]);
		r.dr2 = Integer.parseInt(line[headers.get("DR2")]);
		r.unacceptableAntigens = IOUtil.splitOnWhitespace(line[headers.get("UNNACCEPTABLE_ANTIGENS")]);
		r.undesirableAntigens = IOUtil.splitOnWhitespace(line[headers.get("UNDESIRABLE_ANTIGENS")]);

		// Social characteristics and preferences
		r.age = Integer.parseInt(line[headers.get("AGE")]);

		String transplantCtrHeader = headers.containsKey("CTR") ? "CTR" : "TRANSPLANT_CTR";
		r.transplantCtr = line[headers.get(transplantCtrHeader)].trim();
		String travCentersHeader = headers.containsKey("CENTERS CANDIDATE WILLING TO TRA") ? "CENTERS CANDIDATE WILLING TO TRA" : "TRAV_CENTERS";
		r.travCenters = IOUtil.splitOnWhitespace(line[headers.get(travCentersHeader)]);
		String shipCentersHeader = headers.containsKey("CENTERS CANDIDATE WILL ALLOW SHI") ? "CENTERS CANDIDATE WILL ALLOW SHI" : "SHIP_CENTERS";
		r.shipCenters = IOUtil.splitOnWhitespace(line[headers.get(shipCentersHeader)]);

		r.region = Integer.parseInt(line[headers.get("REGION")]);
		r.dsa = line[headers.get("DSA")].trim();

		r.pedCandidate = IOUtil.stringToBool(line[headers.get("PED_CANDIDATE")]);
		r.numKPDMatchRuns = Integer.parseInt(line[headers.get("NUM_KPD_MATCH_RUNS")]);

		String acceptKiTypeHeader = headers.containsKey("RIGHT/LEFT/EITHER KI ACCEPTED?") ? "RIGHT/LEFT/EITHER KI ACCEPTED?" : "ACCEPT_KI_TYPE";
		r.acceptKiType = line[headers.get(acceptKiTypeHeader)].trim();

		String priorLivingDonorHeader = headers.containsKey("PRIOR LIVING DONOR") ? "PRIOR LIVING DONOR" : "PRIOR_LIVING_DONOR";
		r.priorLivingDonor = IOUtil.stringToBool(line[headers.get(priorLivingDonorHeader)]);
		r.minDonorAge = Integer.parseInt(line[headers.get("MIN_DONOR_AGE")]);
		r.maxDonorAge = Integer.parseInt(line[headers.get("MAX_DONOR_AGE")]);
		r.acceptHepbPos = IOUtil.stringToBool(line[headers.get("ACCEPT_HEPB_POS")]);
		r.highlySensitized = IOUtil.stringToBool(line[headers.get("HIGHLY_SENSITIZED")]);
		r.minDonorCreat = Double.valueOf(line[headers.get("MIN_DONOR_CREAT")]);
		r.maxDonorBPSystolic = Double.valueOf(line[headers.get("MAX_DONOR_BP_SYSTOLIC")]);

		try {
			r.maxDonorBPDiastolic = Double.valueOf(line[headers.get("MAX_DONOR_BP_DIASTOLIC")]);
		} catch(NumberFormatException e) {
			// These aren't listed sometimes, so we'll assume no preference
			r.maxDonorBPDiastolic = Double.MAX_VALUE;
		}

		try {
			r.maxDonorBMI = Double.valueOf(line[headers.get("MAX_DONOR_BMI")]);
		} catch(NumberFormatException e) {
			r.maxDonorBMI = Double.MAX_VALUE;   // no max BMI = "**" string or similar
		}
		r.acceptCMVPos = IOUtil.stringToBool(line[headers.get("ACCEPT_CMV_POS")]);
		r.acceptEBVPos = IOUtil.stringToBool(line[headers.get("ACCEPT_EBV_POS")]);

		// UNOS only started reporting CPRA in mid-2013; if it's available, store the actual
		// CPRA and if it's not store CPRA=100 if highly-sensitized or CPRA=0 otherwise
		if(headers.containsKey("CPRA")) {
			r.cpra = Double.valueOf(line[headers.get("CPRA")]);
		} else {
			if(r.highlySensitized) {
				r.cpra = 1.0;
			} else {
				r.cpra = 0.0;
			}
		}
		return r;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((kpdCandidateID == null) ? 0 : kpdCandidateID.hashCode());
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
		UNOSRecipient other = (UNOSRecipient) obj;
		if (kpdCandidateID == null) {
			if (other.kpdCandidateID != null) {
				return false;
			}
		} else if (!kpdCandidateID.equals(other.kpdCandidateID)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(UNOSRecipient r) {
		return this.kpdCandidateID.compareTo(r.kpdCandidateID);
	}

	public int getHLA_A_Mismatch(UNOSDonor d) {
		return (
				((a1 == d.a1 || a1 == d.a2) ? 0 : 1) +
				((a2 == d.a1 || a2 == d.a2) ? 0 : 1)
				);
	}
	
	public int getHLA_B_Mismatch(UNOSDonor d) {
		return (
				((b1 == d.b1 || b1 == d.b2) ? 0 : 1) +
				((b2 == d.b1 || b2 == d.b2) ? 0 : 1)
				);
	}
	
	public int getHLA_DR_Mismatch(UNOSDonor d) {
		return (
				((dr1 == d.dr1 || dr1 == d.dr2) ? 0 : 1) +
				((dr2 == d.dr1 || dr2 == d.dr2) ? 0 : 1)
				);
	}
	
	// Returns the OPTN numeric score for an ABO mismatch between Recipient and Donor
	// 1:  identical
	// 2:  compatible
	// 3:  incompatible
	public int getABOMismatch(UNOSDonor d) {
		if(this.abo.equals(d.abo)) { return 1; }
		else if(this.abo.canGetFrom(d.abo)) { return 2; }
		else { return 3; }
	}
}
