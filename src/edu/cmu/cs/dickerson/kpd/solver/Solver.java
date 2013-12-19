package edu.cmu.cs.dickerson.kpd.solver;

import edu.cmu.cs.dickerson.kpd.structure.Pool;

public abstract class Solver {

	protected Pool pool;
	
	protected int maxCPUThreads = 0;
	protected double maxSolveSeconds = 0;
	
	public Solver(Pool pool) {
		this.pool = pool;
	}
	
	public int getMaxCPUThreads() {
		return maxCPUThreads;
	}

	public void setMaxCPUThreads(int maxCPUThreads) {
		this.maxCPUThreads = maxCPUThreads;
	}

	public double getMaxSolveSeconds() {
		return maxSolveSeconds;
	}

	public void setMaxSolveSeconds(double maxSolveSeconds) {
		this.maxSolveSeconds = maxSolveSeconds;
	}

	public abstract String getID();
}
