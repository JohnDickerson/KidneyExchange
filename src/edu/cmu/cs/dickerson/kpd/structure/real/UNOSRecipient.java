package edu.cmu.cs.dickerson.kpd.structure.real;

import java.util.Map;

import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class UNOSRecipient {

	// This is a UNIQUE ID assigned by UNOS; if any other recipient is loaded with ID,
	// it assumed to be EQUAL to this recipient
	private final Integer kpdCandidateID;
		
	public UNOSRecipient(Integer kpdCandidateID) {
		this.kpdCandidateID = kpdCandidateID;
	}
	
	// Replicas of the headers in the original .csv file
	private Integer kpdPairID;
	private int age;
	private BloodType abo;
	private double cpra;
	
	
	
	public Integer getKPDPairID() {
		return kpdPairID;
	}
	public void setKPDPairID(Integer kpdPairID) {
		this.kpdPairID = kpdPairID;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public BloodType getABO() {
		return abo;
	}
	public void setABO(BloodType abo) {
		this.abo = abo;
	}
	public double getCPRA() {
		return cpra;
	}
	public void setCPRA(double cpra) {
		this.cpra = cpra;
	}
	public Integer getKPDCandidateID() {
		return kpdCandidateID;
	}
	
	public static UNOSRecipient makeUNOSRecipient(String[] line, Map<String, Integer> headers) {
		// TODO Auto-generated method stub
		return null;
	}
		
	
}
