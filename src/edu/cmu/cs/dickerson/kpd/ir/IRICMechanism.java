package edu.cmu.cs.dickerson.kpd.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;

public class IRICMechanism {

	private Set<Hospital> hospitals;

	public IRICMechanism(Set<Hospital> hospitals) {
		this.hospitals = hospitals;	
		for(Hospital hospital : hospitals) { hospital.setNumCredits(0); }
	}
	
	public Solution doMatching() {
		
		// TODO
		
		// Initial credit balance update
		for(Hospital hospital : hospitals) {
			
		}
		
		// Get maximum matching subject to each hospital getting at least as many matches
		// as it could've gotten if had only matched its reported pairs alone
		
		
		// Random permutation of hospitals
		List<Hospital> shuffledHospitals = new ArrayList<Hospital>( this.hospitals );
		Collections.shuffle(shuffledHospitals);
		
		// Build constraints based on this ordering
		for(Hospital hospital : shuffledHospitals) {
			
		}
		
		
		return null;
	}
}
