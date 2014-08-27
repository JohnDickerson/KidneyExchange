package edu.cmu.cs.dickerson.kpd.ir;

import java.util.Set;

import edu.cmu.cs.dickerson.kpd.ir.structure.Hospital;

public class ICEfficientMechanism extends ICMechanism {

	public ICEfficientMechanism(Set<Hospital> hospitals) {
		this(hospitals, 3, 0, 1);   // default to 3-cycles and 0-chains
	}

	public ICEfficientMechanism(Set<Hospital> hospitals, int cycleCap, int chainCap, int meanLifeExpectancy) {
		super(hospitals, cycleCap, chainCap, meanLifeExpectancy);
		this.setEnforcingIRConstraints(false);
	}

}
