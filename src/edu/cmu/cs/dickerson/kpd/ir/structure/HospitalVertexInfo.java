package edu.cmu.cs.dickerson.kpd.ir.structure;

/**
 * Stores information about patient-donor pairs that could be falsified by a hospital
 *
 */
public class HospitalVertexInfo {
	public int entranceTime=0;    // when did this vertex first appear?
	public int lifeExpectancy=1;  // for how many time periods will this vertex exist?
}
