package edu.cmu.cs.dickerson.kpd.structure.generator.factories;

import org.jgrapht.VertexFactory;

import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class AllMatchVertexPairFactory implements VertexFactory<Vertex> {

	private int currentVertID = 0;
	public AllMatchVertexPairFactory() {
		currentVertID = 0;
	}
	
	@Override
	public VertexPair createVertex() {
		return new VertexPair(currentVertID++,
				BloodType.AB,
				BloodType.O,
				false,
				0.0,
				false);
	}

}
