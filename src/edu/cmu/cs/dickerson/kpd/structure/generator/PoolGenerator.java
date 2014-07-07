package edu.cmu.cs.dickerson.kpd.structure.generator;

import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.Pool;

public abstract class PoolGenerator {

	protected Random random;
	
	public PoolGenerator(Random random) {
		this.random = random;
	}
	
	public abstract Pool generate(int numPairs, int numAltruists);
	
	public abstract void addVerticesToPool(Pool pool, int numPairs, int numAltruists);
	
	// TODO write to a file, specifically in our UNOS KPD format
	// TODO include compatible pairs
}
