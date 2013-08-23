package edu.cmu.cs.dickerson.kpd.generator;

import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.KPDPool;

public abstract class Generator {

	protected Random random;
	
	public Generator(Random random) {
		this.random = random;
	}
	
	public abstract KPDPool generate(int numPairs, int numAltruists);
	
	// TODO write to a file, specifically in our UNOS KPD format
	// TODO include compatible pairs
}
