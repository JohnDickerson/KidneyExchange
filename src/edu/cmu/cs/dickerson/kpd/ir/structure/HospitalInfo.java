package edu.cmu.cs.dickerson.kpd.ir.structure;

import edu.cmu.cs.dickerson.kpd.structure.Pool;


/**
 * Stores intermediary hospital information (reported vertex sets,
 * size of maximum internal matching given reported vertex set, etc), so we don't
 * have to keep asking for it.
 */
public class HospitalInfo {
	public Pool reportedInternalPool;        // reported type
	public int maxReportedInternalMatchSize; // max #pairs matched given reported type
	public int maxPossibleInternalMatchSize; // max #pairs that could've been matched internally
	public int numMatchedInternally;         // #pairs matched internally by hospital
	public int actualMechanismMatchSize;     // #pairs that were actually matched by mechanism
	public int privateVertexCt;              // #pairs held privately+publicly
	public int publicVertexCt;               // #pairs held publicly
	public int minRequiredNumPairs;          // min #pairs that must be matched  [changes during mechanism]
	public int exactRequiredNumPairs = -1;   // exact #pairs that must be matched  [changes during mechanism]
	
	@Override
	public String toString() {
		return (null!=reportedInternalPool ? reportedInternalPool.vertexSet().size() : "0") + ", " + maxReportedInternalMatchSize + ", " + minRequiredNumPairs + ", " + exactRequiredNumPairs;
	}
}
