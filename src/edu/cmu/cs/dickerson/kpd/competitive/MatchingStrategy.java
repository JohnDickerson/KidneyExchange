package edu.cmu.cs.dickerson.kpd.competitive;

import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;

public abstract class MatchingStrategy {
	public abstract Cycle match(Vertex v, Pool pool);

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
