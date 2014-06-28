package edu.cmu.cs.dickerson.kpd.ir;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.ir.arrivals.ArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;

public class Hospital {

	private ArrivalDistribution arrivalDist;
	private Integer ID;
	
	private Set<Vertex> vertices;
	
	private int numCredits;
	private int numMatched;
	
	public Hospital(Integer ID, ArrivalDistribution arrivalDist) {
		
		this.ID = ID;
		this.arrivalDist = arrivalDist;
		
		// New hospitals have no credits, no history of matches, no patient-donor pairs, etc
		this.numCredits = 0;
		this.numMatched = 0;
		this.vertices = new HashSet<Vertex>();
	}
	
}
