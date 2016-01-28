package edu.cmu.cs.dickerson.kpd.structure.real;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexAltruist;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;

public class UNOSPair {

	private Set<UNOSDonor> donors;
	private UNOSRecipient recipient;
	private String pairID;
	private boolean isAltruist;
	
	private UNOSPair(Set<UNOSDonor> donors, UNOSRecipient recipient, boolean isAltruist) {
		this.donors = donors;
		this.recipient = recipient;
		this.isAltruist = isAltruist;
		
		// The ID of a vertex is either the recipient's "KPD_candidate_id" from the .csv files or,
		// if an altruist, the negative of that altruist's "KPD_donor_id"
		if(this.isAltruist) {
			this.pairID = donors.iterator().next().kpdDonorID;
		} else {
			this.pairID = recipient.kpdCandidateID;
		}
	}
	
	public static UNOSPair makeUNOSAltruist(UNOSDonor altruist) {
		Set<UNOSDonor> altruistSet = new HashSet<UNOSDonor>();
		altruistSet.add(altruist);
		return new UNOSPair(altruistSet, null, true);
	}
	
	public static UNOSPair makeUNOSPair(UNOSDonor d, UNOSRecipient r) {
		Set <UNOSDonor> donorSet = new HashSet<UNOSDonor>();
		donorSet.add(d);
		return UNOSPair.makeUNOSPair(donorSet, r);
	}
	
	public static UNOSPair makeUNOSPair(Set<UNOSDonor> donors, UNOSRecipient r) {
		return new UNOSPair(donors, r, false);
	}
	
	
	public static boolean canDrawDirectedEdge(UNOSPair src, UNOSPair dst) {
		return UNOSPair.canDrawDirectedEdge(src, dst, 0);
	}
	
	/**
	 * If the recipient at dst is compatible with at least one of the donors 
	 * at src, returns true.  If the dst vertex is an altruist and the src
	 * vertex is not an altruist, always returns true.  If both are altruists,
	 * returns false
	 * @param src
	 * @param dst
	 * @param threshold allowance of overlap between donor, patient vector -- default should be 0 (no overlap)
	 * @return
	 */
	public static boolean canDrawDirectedEdge(UNOSPair src, UNOSPair dst, int threshold) {
		
		// We assume donors cannot donate to their paired recipients
		if(dst.equals(src)) { return false; }
		
		// Always a [dummy] edge from a pair to an altruist;
		// Never an adge from an altruist to an altruist
		if(dst.isAltruist()) {
			return !src.isAltruist();
		}
		
		// If at least one donor at src can give to the recipient at dst, 
		// then return edge
		for(UNOSDonor d : src.getDonors()) {
			if(d.canDonateTo(dst.getRecipient(), threshold)) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Returns the kind of Vertex we use in the optimization problem,
	 * linked back to the underlying UNOSPair
	 * @param vertexID
	 * @return
	 */
	public Vertex toBaseVertex(Integer vertexID) {
		if(isAltruist()) {
			return new VertexAltruist(vertexID, this);
		} else {
			return new VertexPair(vertexID, this);
		}
	}
	
	
	public Set<UNOSDonor> getDonors() {
		return donors;
	}
	
	public boolean addDonor(UNOSDonor d) {
		return this.donors.add(d);
	}

	public UNOSRecipient getRecipient() {
		return recipient;
	}

	public String getPairID() {
		return pairID;
	}

	public boolean isAltruist() {
		return isAltruist;
	}
	
	
	
}
