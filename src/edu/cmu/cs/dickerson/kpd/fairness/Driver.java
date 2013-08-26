package edu.cmu.cs.dickerson.kpd.fairness;

import java.util.Random;

import edu.cmu.cs.dickerson.kpd.generator.SaidmanGenerator;
import edu.cmu.cs.dickerson.kpd.structure.KPDPool;

public class Driver {

	public static void main(String args[]) {
	
		Random r = new Random();
		KPDPool pool = new SaidmanGenerator(r).generate(100, 10);
		System.out.println(pool);
	}
}
