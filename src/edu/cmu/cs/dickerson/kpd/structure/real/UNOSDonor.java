package edu.cmu.cs.dickerson.kpd.structure.real;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	public boolean canDonateTo(UNOSRecipient r) {
		return this.canDonateTo(r, 0);  // no overlap allowed, threshold = 0
	}
	/**
	 * 
	 * @param r possible recipient of donor's organ
	 * @param threshold budget of "mismatches" allowed before no edge; real life = 0
	 * @return true if this donor passes all donation tests (except at most threshold) to recipient
	 */
	public boolean canDonateTo(UNOSRecipient r, int threshold) {

		assert threshold >= 0;
		
		// To donate successfully, the donor and recipient must pass
		// AT MOST threshold tests used.  Many of these are from USNA codebase.
		int mismatches = 0;
		mismatches += bloodTypeTest(r);
		mismatches += unacceptableAntigenTest(r);
		mismatches += whichKidneyTest(r);
		mismatches +=  hepBTest(r);
		mismatches +=  creatinineTest(r);
		mismatches +=  bloodPressureTest(r);
		mismatches += bmiTest(r);
		mismatches += cmvTest(r);
		mismatches +=  ebvTest(r);
		mismatches += ageTest(r);
		mismatches += travelAndShippingTest(r);
		return mismatches <= threshold;
	}

	private int bloodTypeTest(UNOSRecipient r) {
		return this.abo.canGiveTo(r.abo) ? 0 : 1;
	}

	private int whichKidneyTest(UNOSRecipient r) {
		// Possible options are "L" = left, "R" = right, or "E" = either
		// Must be "E" for the donor or "E" for the recipient or a donor/recipient match
		return (this.donKiType.equals("E") || r.acceptKiType.equals("E") || this.donKiType.equals(r.acceptKiType)) ? 0 : 1;
	}

	private int hepBTest(UNOSRecipient r) {
		return (!this.hbcAndHBSAG || r.acceptHepbPos) ? 0 : 1;
	}

	private int creatinineTest(UNOSRecipient r) {
		return (this.creatBSA >= r.minDonorCreat) ? 0 : 1;
	}

	private int bloodPressureTest(UNOSRecipient r) {
		int mismatches = 0;
		mismatches += (this.bpSystolic <= r.maxDonorBPSystolic) ? 0 : 1;
		mismatches += (this.bpDiastolic <= r.maxDonorBPDiastolic) ? 0 : 1;
		return mismatches;
	}

	private int bmiTest(UNOSRecipient r) {
		return (this.bmi <= r.maxDonorBMI) ? 0 : 1;
	}

	private int cmvTest(UNOSRecipient r) {
		return (!this.cmv || r.acceptCMVPos) ? 0 : 1;
	}

	private int ebvTest(UNOSRecipient r) {
		return (!this.ebv || r.acceptEBVPos) ? 0 : 1;
	}

	private int ageTest(UNOSRecipient r) {
		int mismatches = 0;
		mismatches += (this.age <= r.maxDonorAge) ? 0 : 1;
		mismatches += (this.age >= r.minDonorAge) ? 0 : 1;
		return mismatches;
	}

	private int travelAndShippingTest(UNOSRecipient r) {

		int mismatches = 0;
		// Donor and recipient are at the same center
		if(this.homeCtr.equals(r.transplantCtr)) { mismatches += 0; }
		// Donor will travel to patient
		else if(this.travCenters.contains(r.transplantCtr)) { mismatches += 0; }
		// Patient will travel to donor
		else if(r.travCenters.contains(this.homeCtr)) { mismatches += 0; }
		// If we're happy to travel but couldn't, count a mismatch
		else { mismatches += 1; }
		
		// Patient will accept a shipped organ from the donor
		if(this.donKiShip) {
			if(r.shipCenters.contains(this.homeCtr)) { mismatches = 0; }  // set to zero again -- this is a legal match
		} else {
			if(r.shipCenters.contains(this.homeCtr)) { mismatches += 1; }   // if donor allows shipping, would be good
		}
		
		return mismatches;
	}


	private static Pattern antigenPattern = Pattern.compile("^([a-zA-Z]{1,3})([0-9]{1,4})$");
	private int unacceptableAntigenTest(UNOSRecipient r) {

		int mismatches = 0;
		// TODO -- store antigens in a more sane way ...

		// Antigens look like NNDD, where NN is the type and DD is a number
		// e.g., A12 is type A, number 12
		// If there is ANY match between recipient's unacceptables and a donor, fail
		for(String ant : r.unacceptableAntigens) {

			// Ignore or fix mistyped input
			if(ant.toUpperCase().trim().equals("N/A") || ant.trim().length() < 1) { continue; }
			ant = ant.replace("*", "");


			Matcher m = antigenPattern.matcher(ant);
			if(m.find()) {
				String antType = m.group(1).toUpperCase();
				int antNum = Integer.parseInt(m.group(2));

				if(antType.equals("A")) {
					if( this.a1 == antNum || this.a2 == antNum) { mismatches += 1; }
				} else if(antType.equals("B")) {
					if( this.b1 == antNum || this.b2 == antNum) { mismatches += 1; }
				} else if(antType.equals("BW")) {
					if( this.bw4 && antNum == 4 ) { mismatches += 1; }
					if( this.bw6 && antNum == 6 ) { mismatches += 1; }
				} else if(antType.equals("CW")) {
					if( this.cw1 == antNum || this.cw2 == antNum) { mismatches += 1; }
				} else if(antType.equals("DQ")) {
					if( this.dq1 == antNum || this.dq2 == antNum) { mismatches += 1; }	
				} else if(antType.equals("DR")) {
					if( this.dr1 == antNum || this.dr2 == antNum) { mismatches += 1; }	
					if( this.dr51 && antNum == 51) { mismatches += 1; }
					if( this.dr52 && antNum == 52) { mismatches += 1; }
					if( this.dr53 && antNum == 53) { mismatches += 1; }
				} else if(antType.equals("DP") || antType.equals("DPW")) {
					if( this.dp1 == antNum || this.dp2 == antNum) { mismatches += 1; }	
				} else {
					// We don't have enough data to deal with DQA yet
					//IOUtil.dPrintln("ERROR: " + ant);
				}
			} else {
				IOUtil.dPrintln("Could not parse raw antigen string: " + ant);
			}
		}

		//if( (this.bw4 && r.)

		return mismatches;
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
			donor.dp1 = Integer.parseInt(line[headers.get("DP1")]);
		} catch(NumberFormatException e) {
			donor.dp1 = -1;
		}
		try {
			donor.dp2 = Integer.parseInt(line[headers.get("DP2")]);
		} catch(NumberFormatException e) {
			donor.dp2 = -1;
		}


		// Social characteristics and preferences
		donor.age = Integer.parseInt(line[headers.get("AGE")]);
		donor.homeCtr = line[headers.get("HOME_CTR")].trim();

		String travCentersHeader = headers.containsKey("CENTERS WHERE DONOR IS WILLING T") ? "CENTERS WHERE DONOR IS WILLING T" : "TRAV_CENTERS";
		donor.travCenters = IOUtil.splitOnWhitespace(line[headers.get(travCentersHeader)]);

		try {
			donor.region = Integer.parseInt(line[headers.get("REGION")]);
		} catch(NumberFormatException e) {
			// Sometimes given as "not reported"; we don't use this, so whatever
			donor.region = -1;
		}
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
			donor.maxPairsCycle = Integer.parseInt(line[headers.get("MAX_PAIRS_CYCLE")]);
		}

		if(headers.containsKey("BRIDGE_MATCH")) {
			String bridgeMatchAllowed = line[headers.get("BRIDGE_MATCH")].trim();
			if( !bridgeMatchAllowed.isEmpty() && IOUtil.stringToBool(bridgeMatchAllowed) ) {
				donor.maxPairsChain = 4;   // okay with bridge donors = can do UNOS cap=4 chains
			} else {
				donor.maxPairsChain = 0;
			}
		} else {
			donor.maxPairsChain = Integer.parseInt(line[headers.get("MAX_PAIRS_CHAIN")]);
		}


		return donor;
	}

	private static Integer parseAntigen(String s) {
		return Integer.valueOf(s.replaceAll("[^\\d.]",  ""));
	}

	public int getHLA_A_Mismatch(UNOSRecipient r) { return r.getHLA_A_Mismatch(this); }
	public int getHLA_B_Mismatch(UNOSRecipient r) { return r.getHLA_B_Mismatch(this); }
	public int getHLA_DR_Mismatch(UNOSRecipient r) { return r.getHLA_DR_Mismatch(this); }
	public int getABOMismatch(UNOSRecipient r) { return r.getABOMismatch(this); }

}
