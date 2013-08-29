package edu.cmu.cs.dickerson.kpd.fairness;

import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.structure.Cycle;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.alg.CycleGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;

public class Driver {

	public static void main(String args[]) {
	
		Random r = new Random();
		Pool pool = new SaidmanPoolGenerator(r).generate(100, 10);
		System.out.println(pool);
		
		CycleGenerator cg = new CycleGenerator(pool);
		List<Cycle> allCycles = cg.generateCyclesAndChains(3, 3);
		System.out.println(allCycles.size());
		
	}
}
