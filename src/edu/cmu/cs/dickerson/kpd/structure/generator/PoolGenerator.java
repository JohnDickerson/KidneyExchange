package edu.cmu.cs.dickerson.kpd.structure.generator;

import java.util.Random;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;

public abstract class PoolGenerator {

	protected Random random;
	
	public PoolGenerator(Random random) {
		this.random = random;
	}
	
	public abstract Pool generate(int numPairs, int numAltruists);
	
	public abstract Set<Vertex> addVerticesToPool(Pool pool, int numPairs, int numAltruists);
	

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
	
	// TODO write to a file, specifically in our UNOS KPD format
	// TODO include compatible pairs
}
