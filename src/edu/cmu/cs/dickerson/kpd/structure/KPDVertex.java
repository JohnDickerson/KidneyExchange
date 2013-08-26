package edu.cmu.cs.dickerson.kpd.structure;

public abstract class KPDVertex implements Comparable<KPDVertex> {

	protected final Integer ID;
	
	public KPDVertex(Integer ID) {
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
		
		KPDVertex other = (KPDVertex) obj;
		if (ID != other.ID) {
			return false;
		}
		return true;
	}
	
	@Override
	public int compareTo(KPDVertex v) {
		return this.getID().compareTo(v.getID());
	}
}
