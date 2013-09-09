package edu.cmu.cs.dickerson.kpd.structure.types;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public enum BloodType {

	O("O"),
	A("A"),
	B("B"),
	AB("AB");
	
	private final String dispName;
	
	// Want to do string to enum lookups when loading data files
	private static final Map<String, BloodType> strToBlood = new HashMap<String, BloodType>();
	static {
		for(BloodType bt : BloodType.values()) {
			strToBlood.put(bt.dispName, bt);
		}
	}
	
	private BloodType(String dispName) {
		this.dispName = dispName;
	}
	
	/**
	 * Is this blood type donor-compatible with the candidate's blood type?
	 * @param cand BloodType of person receiving a kidney 
	 * @return true if blood type compatible with candidate, false otherwise
	 */
	public boolean canGiveTo(BloodType cand) {
		
		if(this.equals(BloodType.O)  // O can donate to {O,A,B,AB}
				|| cand.equals(BloodType.AB)  // AB can receive from {O,A,B,AB}
				|| this.equals(cand)) {  // A gives to {A,AB}, B gives to {B,AB}
			return true;
		} else {
			// O cannot receive from {A,B,AB}
			// A cannot receive from {B,AB}
			// B cannot receive from {A,AB}
			return false;
		}
	}
	
	/**
	 * Can a person with this blood type receive a kidney of the donor's blood type?
	 * @param donor BloodType of person donating a kidney
	 * @return true if donor is blood-donor type compatible, false otherwise
	 */
	public boolean canGetFrom(BloodType donor) {
		return donor.canGiveTo(this);
	}
	
	/**
	 * Translates a String blood type (like "AB") to a BloodType object (AB).
	 * Currently maps subtypes to ABO groups (e.g., A1 -> A, A2B -> AB, ...)
	 * @param name a readable identifier (typically "A", "B", "O", or "AB")
	 * @return BloodType corresponding to this string, or NoSuchElementException
	 * for unrecognized String inputs
	 */
	public static BloodType getBloodType(String name) {
		
		String key = name.trim().replaceAll("[0-9]","").toUpperCase();
		if(strToBlood.containsKey(key)) {
			return strToBlood.get(key);
		} else {
			throw new NoSuchElementException(key + " not a recognized blood type.");
		}
	}
	
	@Override
	public String toString() {
		return dispName;
	}
}
