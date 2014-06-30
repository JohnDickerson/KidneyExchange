package edu.cmu.cs.dickerson.kpd.dynamic.simulator;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.ir.Hospital;
import edu.cmu.cs.dickerson.kpd.ir.arrivals.UniformArrivalDistribution;

public class IRICDynamicSimulator extends DynamicSimulator {

	private Set<Hospital> hospitals;
	
	public IRICDynamicSimulator(Set<Hospital> hospitals) {
		super();
		this.hospitals = hospitals;
	}
	
	
	
	
	public static void main(String[] args) {

		// Create a set of 3 truthful hospitals, with urand arrival rates
		Set<Hospital> hospitals = new HashSet<Hospital>();
		for(int idx=0; idx<3; idx++) {
			hospitals.add( new Hospital(idx, new UniformArrivalDistribution(0,10), true) );
		}
		
		IRICDynamicSimulator sim = new IRICDynamicSimulator(hospitals);
		
	}
}
