package edu.cmu.cs.dickerson.kpd.structure;

public abstract class Vertex implements Comparable<Vertex> {

	protected final Integer ID;
	
	public Vertex(Integer ID) {
		this.ID = ID;
	}
	
	public abstract boolean isAltruist();
	
	public Integer getID() {
		return ID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		Vertex other = (Vertex) obj;
		if (ID != other.ID) {
			return false;
		}
		return true;
	}
	
	@Override
	public int compareTo(Vertex v) {
		return this.getID().compareTo(v.getID());
	}
	
	@Override
	public String toString() {
		return getID().toString();
	}
}
