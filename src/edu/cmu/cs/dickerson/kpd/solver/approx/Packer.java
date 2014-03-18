package edu.cmu.cs.dickerson.kpd.solver.approx;

import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;

public abstract class Packer {
	
	public Solution pack() { return pack(Double.MAX_VALUE); }
	public abstract Solution pack(double upperBound);
}
