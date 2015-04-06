package edu.cmu.cs.dickerson.kpd.dynamic;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.dynamic.simulator.CompetitiveDynamicSimulator;
import edu.cmu.cs.dickerson.kpd.structure.generator.PoolGenerator;
import edu.cmu.cs.dickerson.kpd.structure.generator.SaidmanPoolGenerator;

public class DriverCompetitive {
	
	public static void main(String[] args) {

		Random r = new Random();

		// List of generators we want to use
		List<PoolGenerator> genList = Arrays.asList(new PoolGenerator[] {
				new SaidmanPoolGenerator(r),
				//UNOSGenerator.makeAndInitialize(IOUtil.getBaseUNOSFilePath(), ',', r),	
		});
		
		
		double gamma = 0.5;
		double alpha = 0.5;
		double m = 2;
		double lambda = 2;
		
		CompetitiveDynamicSimulator sim = new CompetitiveDynamicSimulator(gamma, alpha, m, lambda, r);
		sim.run(10);
		
		
	}
}
