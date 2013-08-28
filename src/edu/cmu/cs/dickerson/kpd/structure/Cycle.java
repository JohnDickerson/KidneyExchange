package edu.cmu.cs.dickerson.kpd.structure;

import java.util.ArrayList;
import java.util.List;

public class Cycle {

	private List<Vertex> vertices;
	private double weight;
	
	private Cycle(List<Vertex> vertices, double weight) {
		this.vertices = vertices;
		this.weight = weight;
	}

	public static Cycle makeCycle(List<Vertex> vertices, double weight) {
		List<Vertex> verticesCopy = new ArrayList<Vertex>(vertices);
		return new Cycle(verticesCopy, weight);
	}
	
	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	
}
