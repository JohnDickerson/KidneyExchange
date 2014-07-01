package edu.cmu.cs.dickerson.kpd.ir.structure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.helper.MathUtil;
import edu.cmu.cs.dickerson.kpd.ir.arrivals.ArrivalDistribution;
import edu.cmu.cs.dickerson.kpd.solver.CycleFormulationCPLEXSolver;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverException;
import edu.cmu.cs.dickerson.kpd.solver.exception.SolverRuntimeException;
import edu.cmu.cs.dickerson.kpd.solver.solution.Solution;
import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleMembership;

public class Hospital {


	private ArrivalDistribution arrivalDist;
	private final Integer ID;
	private boolean isTruthful;

	private Set<Vertex> vertices;

	private int numCredits;
	private int numMatched;

	public Hospital(Integer ID, ArrivalDistribution arrivalDist, boolean isTruthful) {

		this.ID = ID;
		this.arrivalDist = arrivalDist;
		this.isTruthful = isTruthful;

		// New hospitals have no credits, no history of matches, no patient-donor pairs, etc
		this.numCredits = 0;
		this.numMatched = 0;
		this.vertices = new HashSet<Vertex>();
	}

	public Solution doInternalMatching(Pool reducedPool, int cycleCap, int chainCap, boolean usingFailureProbabilities) throws SolverException {
		
		CycleGenerator cg = new CycleGenerator(reducedPool);
		List<Cycle> internalCycles = cg.generateCyclesAndChains(cycleCap, chainCap, false);
		Solution internalMatch =
				(new CycleFormulationCPLEXSolver(reducedPool, internalCycles, 
						new CycleMembership(reducedPool, internalCycles))).solve();
		cg = null; internalCycles = null;
		System.gc();

		if(!MathUtil.isInteger(internalMatch.getObjectiveValue())) { throw new SolverException("IRICMechanism only works for unit-weight, deterministic graphs."); }
		return internalMatch;
	}

	/**
	 * Allows the hospital to lie about its internal set of vertices; we assume
	 * this reported set of vertices is a subset of its internal Set<Vertex> vertices
	 * @return publicly reported set of vertices belonging to hospital
	 */
	public Set<Vertex> getPublicVertexSet() {
		if(isTruthful) {
			return vertices;
		} else {
			throw new UnsupportedOperationException("Must call getPublicVertexSet with pool information for non-truthful hospitals.");
		}
	}

	public Set<Vertex> getPublicVertexSet(Pool fullPool, int cycleCap, int chainCap, boolean usingFailureProbabilities) {
		if(isTruthful) {
			// Truthful hospitals truthfully report their full type (set of vertices)
			return vertices;
		} else {
			try {
				// Internally match on all vertices, not just publicly reported vertices	
				Pool realInternalPool = fullPool.makeSubPool( this.getPublicAndPrivateVertices() );
				Solution internalMatch = doInternalMatching(realInternalPool, cycleCap, chainCap, usingFailureProbabilities);
				
				// Report only those vertices that weren't matched (so AllVertices - InternallyMatchedVertices)
				Set<Vertex> usedVerts = Cycle.getConstituentVertices(internalMatch.getMatching(), realInternalPool);
				Set<Vertex> reportedVerts = new HashSet<Vertex>(this.getPublicAndPrivateVertices());
				reportedVerts.removeAll(usedVerts);
				return reportedVerts;
				
			} catch(SolverException e) {
				e.printStackTrace();
				throw new SolverRuntimeException("Unrecoverable error solving internal matching on non-truthful hospital " + this + "; experiments are bunk.\nOriginal Message: " + e.getMessage());
			}
		}
	}

	/**
	 * Draws an arrival rate (i.e., number of vertices to enter at this time period)
	 */
	public int drawArrival() {
		return this.arrivalDist.draw();
	}

	/**
	 * Expected number of vertices arriving per time period
	 */
	public int getExpectedArrival() {
		return this.arrivalDist.expectedDraw();
	}

	/**
	 * Deducts from credit balance of hospital
	 * @param credits Number of credits to deduct
	 * @return new balance of hospital
	 */
	public int removeCredits(int credits) {
		return addCredits(-credits);
	}

	/**
	 * Adds to credit balance of hospital
	 * @param credits Number of credits to add
	 * @return new balance of hospital
	 */
	public int addCredits(int credits) {
		this.numCredits += credits;
		return numCredits;
	}

	public ArrivalDistribution getArrivalDist() {
		return arrivalDist;
	}

	public void setArrivalDist(ArrivalDistribution arrivalDist) {
		this.arrivalDist = arrivalDist;
	}

	public Set<Vertex> getPublicAndPrivateVertices() {
		return vertices;
	}

	public void setPublicAndPrivateVertices(Set<Vertex> vertices) {
		this.vertices = vertices;
	}

	public int getNumCredits() {
		return numCredits;
	}

	public void setNumCredits(int numCredits) {
		this.numCredits = numCredits;
	}

	public int getNumMatched() {
		return numMatched;
	}

	public void setNumMatched(int numMatched) {
		this.numMatched = numMatched;
	}

	public Integer getID() {
		return ID;
	}

	public boolean isTruthful() {
		return isTruthful;
	}

	public void setTruthful(boolean isTruthful) {
		this.isTruthful = isTruthful;
	}
	
	@Override
	public String toString() {
		return "H" + ID + "<" + isTruthful + ", " + arrivalDist + ", |V|=" + vertices.size() + ", Cred=" + numCredits + ">";
	}
}
