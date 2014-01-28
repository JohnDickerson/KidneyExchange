package edu.cmu.cs.dickerson.kpd.structure.real;

import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;


public class UNOSDonor implements Comparable<UNOSDonor> {

	// This is a UNIQUE ID assigned by UNOS; if any other donor is loaded with ID,
	// it assumed to be EQUAL to this donor
	public final String kpdDonorID;
	
	// Replicas of the headers in the original .csv file
	public String kpdPairID;
	public String kpdCandidateID;
	public boolean nonDirectedDonor;
	public BloodType abo;
	public int a1, a2, b1, b2, cw1, cw2, dq1, dq2, dr1, dr2, dp1, dp2;
	public boolean bw4, bw6, dr51, dr52, dr53;
	public int age;
	public String homeCtr;
	public Set<String> travCenters;
	public int region;
	public String dsa;
	public String donKiType;
	public boolean hbcAndHBSAG;
	public double creatBSA;
	public double bpSystolic;
	public double bpDiastolic;
	public double bmi;
	public boolean cmv, ebv;
	public boolean donKiShip;
	public int maxPairsCycle, maxPairsChain;
	
	public UNOSDonor(String kpd_donor_id) {
		this.kpdDonorID = kpd_donor_id;
	}
	
	public boolean canDonateTo(UNOSRecipient recipient) {
		// TODO
		return false;
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((kpdDonorID == null) ? 0 : kpdDonorID.hashCode());
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
		if (kpdDonorID == null) {
			if (other.kpdDonorID != null) {
				return false;
			}
		} else if (!kpdDonorID.equals(other.kpdDonorID)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(UNOSDonor d) {
		return this.kpdDonorID.compareTo(d.kpdDonorID);
	}

	public static UNOSDonor makeUNOSDonor(String[] line, Map<String, Integer> headers) {
		
		String kpdDonorID = line[headers.get("KPD_DONOR_ID")].toUpperCase().trim();
		UNOSDonor donor = new UNOSDonor(kpdDonorID);
		
		// UNOS coding
		donor.kpdPairID = line[headers.get("KPD_PAIR_ID")].toUpperCase().trim();
		donor.nonDirectedDonor = IOUtil.stringToBool(line[headers.get("NON_DIRECTED_DONOR")]);
		
		if(donor.nonDirectedDonor) {
			donor.kpdCandidateID = null;
		} else {
			donor.kpdCandidateID = line[headers.get("KPD_CANDIDATE_ID")].toUpperCase().trim();
		}
		
		// Blood and tissue-type
		donor.abo = BloodType.getBloodType(line[headers.get("ABO")]);
		donor.a1 = UNOSDonor.parseAntigen(line[headers.get("A1")]);
		donor.a2 = UNOSDonor.parseAntigen(line[headers.get("A2")]);
		donor.b1 = UNOSDonor.parseAntigen(line[headers.get("B1")]);
		donor.b2 = UNOSDonor.parseAntigen(line[headers.get("B2")]);
		donor.bw4 = IOUtil.stringToBool(line[headers.get("BW4")]);
		donor.bw6 = IOUtil.stringToBool(line[headers.get("BW6")]);
		donor.cw1 = UNOSDonor.parseAntigen(line[headers.get("CW1")]);
		donor.cw2 = UNOSDonor.parseAntigen(line[headers.get("CW2")]);
		donor.dq1 = UNOSDonor.parseAntigen(line[headers.get("DQ1")]);
		donor.dq2 = UNOSDonor.parseAntigen(line[headers.get("DQ2")]);
		donor.dr1 = UNOSDonor.parseAntigen(line[headers.get("DR1")]);
		donor.dr2 = UNOSDonor.parseAntigen(line[headers.get("DR2")]);
		donor.dr51 = IOUtil.stringToBool(line[headers.get("DR51")]);
		donor.dr52 = IOUtil.stringToBool(line[headers.get("DR52")]);
		donor.dr53 = IOUtil.stringToBool(line[headers.get("DR53")]);
		
		// Sometimes these aren't specified (show up as "Not specif" or "No second") {
		try {
			donor.dp1 = Integer.valueOf(line[headers.get("DP1")]);
		} catch(NumberFormatException e) {
			donor.dp1 = -1;
		}
		try {
			donor.dp2 = Integer.valueOf(line[headers.get("DP2")]);
		} catch(NumberFormatException e) {
			donor.dp2 = -1;
		}
		
		
		// Social characteristics and preferences
		donor.age = Integer.valueOf(line[headers.get("AGE")]);
		donor.homeCtr = line[headers.get("HOME_CTR")].trim();
		
		String travCentersHeader = headers.containsKey("CENTERS WHERE DONOR IS WILLING T") ? "CENTERS WHERE DONOR IS WILLING T" : "TRAV_CENTERS";
		donor.travCenters = IOUtil.splitOnWhitespace(line[headers.get(travCentersHeader)]);
		donor.region = Integer.valueOf(line[headers.get("REGION")]);
		donor.dsa = line[headers.get("DSA")].trim();
		
		// More health characteristics
		String donKiTypeHeader = headers.containsKey("DONOR KIDNEY R/L/E") ? "DONOR KIDNEY R/L/E" : "DON_KI_TYPE";
		donor.donKiType = line[headers.get(donKiTypeHeader)].toUpperCase().trim();
		String hbcAndHBSAGHeader = headers.containsKey("HEP B CORE + AND HEB B SA -") ? "HEP B CORE + AND HEB B SA -" : "HBC_AND_HBSAG";
		donor.hbcAndHBSAG = IOUtil.stringToBool(line[headers.get(hbcAndHBSAGHeader)]);
		donor.creatBSA = Double.valueOf(line[headers.get("CREAT_BSA")]);
		donor.bpSystolic = Double.valueOf(line[headers.get("BP_SYSTOLIC")]);
		donor.bpDiastolic = Double.valueOf(line[headers.get("BP_DIASTOLIC")]);
		donor.bmi = Double.valueOf(line[headers.get("BMI")]);
		
		// Sometimes, CMV and EBV are not known.  In these cases, we play it safe and set
		// CMV=true or EBV=true
		try {
			donor.cmv = IOUtil.stringToBool(line[headers.get("CMV")]);
		} catch( IllegalArgumentException e ) {
			donor.cmv = true;
		}
		try {
			donor.ebv = IOUtil.stringToBool(line[headers.get("EBV")]);	
		} catch( IllegalArgumentException e ) {
			donor.ebv = true;
		}
		
		String donKiShipHeader = headers.containsKey("WILLING TO SHIP KIDNEY") ? "WILLING TO SHIP KIDNEY" : "DON_KI_SHIP";
		donor.donKiShip = IOUtil.stringToBool(line[headers.get(donKiShipHeader)]);
		
		// Chain & cycle preferences -- UNOS reports these differently in 2010/2011 than they do now
		if(headers.containsKey("THREE_WAY_MATCH")) {
			if( IOUtil.stringToBool(line[headers.get("THREE_WAY_MATCH")]) ) {
				donor.maxPairsCycle = 3;   // allow three-way matches
			} else {
				donor.maxPairsCycle = 2;   // only allow two-way matches
			}
		} else {
			donor.maxPairsCycle = Integer.valueOf(line[headers.get("MAX_PAIRS_CYCLE")]);
		}
		
		if(headers.containsKey("BRIDGE_MATCH")) {
			if( IOUtil.stringToBool(line[headers.get("BRIDGE_MATCH")]) ) {
				donor.maxPairsChain = 4;   // okay with bridge donors = can do UNOS cap=4 chains
			} else {
				donor.maxPairsChain = 0;
			}
		} else {
			donor.maxPairsChain = Integer.valueOf(line[headers.get("MAX_PAIRS_CHAIN")]);
		}
		
		
		return donor;
	}
	
	private static Integer parseAntigen(String s) {
		return Integer.valueOf(s.replaceAll("[^\\d.]",  ""));
	}
}
