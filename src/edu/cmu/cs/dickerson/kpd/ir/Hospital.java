package edu.cmu.cs.dickerson.kpd.ir;

import java.util.HashSet;
import java.util.Set;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import edu.cmu.cs.dickerson.kpd.ir.arrivals.ArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;

public class Hospital {

	
	private ArrivalDistribution arrivalDist;
	private final Integer ID;
	private boolean isTruthful;
	
	private Set<Vertex> vertices;

	private int numCredits;
	private int numMatched;

	public Hospital(Integer ID, ArrivalDistribution arrivalDist, boolean isTruthful) {

		this.ID = ID;
		this.arrivalDist = arrivalDist;
		this.isTruthful = isTruthful;
		
		// New hospitals have no credits, no history of matches, no patient-donor pairs, etc
		this.numCredits = 0;
		this.numMatched = 0;
		this.vertices = new HashSet<Vertex>();
	}

	/**
	 * Allows the hospital to lie about its internal set of vertices; we assume
	 * this reported set of vertices is a subset of its internal Set<Vertex> vertices
	 * @return publicly reported set of vertices belonging to hospital
	 */
	public Set<Vertex> getPublicVertexSet() {
		if(isTruthful) {
			// Truthful hospitals truthfully report their full type (set of vertices)
			return vertices;
		} else {
			// Non-truthful hospitals only report those vertices they can't match internally
			// TODO call out to a solver, get the subset of vertices this hospital can't match
			throw new UnsupportedOperationException("Need to implement greedy hospitals.");
		}
	}

	
	/**
	 * Draws an arrival rate (i.e., number of vertices to enter at this time period)
	 */
	public int drawArrival() {
		return this.arrivalDist.draw();
	}

	/**
	 * Expected number of vertices arriving per time period
	 */
	public int getExpectedArrival() {
		return this.arrivalDist.expectedDraw();
	}
	
	/**
	 * Deducts from credit balance of hospital
	 * @param credits Number of credits to deduct
	 * @return new balance of hospital
	 */
	public int removeCredits(int credits) {
		return addCredits(-credits);
	}
	
	/**
	 * Adds to credit balance of hospital
	 * @param credits Number of credits to add
	 * @return new balance of hospital
	 */
	public int addCredits(int credits) {
		this.numCredits += credits;
		return numCredits;
	}

	public ArrivalDistribution getArrivalDist() {
		return arrivalDist;
	}

	public void setArrivalDist(ArrivalDistribution arrivalDist) {
		this.arrivalDist = arrivalDist;
	}

	public Set<Vertex> getPublicAndPrivateVertices() {
		return vertices;
	}

	public void setPublicAndPrivateVertices(Set<Vertex> vertices) {
		this.vertices = vertices;
	}

	public int getNumCredits() {
		return numCredits;
	}

	public void setNumCredits(int numCredits) {
		this.numCredits = numCredits;
	}

	public int getNumMatched() {
		return numMatched;
	}

	public void setNumMatched(int numMatched) {
		this.numMatched = numMatched;
	}

	public Integer getID() {
		return ID;
	}

	public boolean isTruthful() {
		return isTruthful;
	}

	public void setTruthful(boolean isTruthful) {
		this.isTruthful = isTruthful;
	}
}
