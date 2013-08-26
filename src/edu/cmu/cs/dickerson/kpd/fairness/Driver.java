package edu.cmu.cs.dickerson.kpd.fairness;

import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class Driver {

	public static void main(String args[]) {
		
		System.out.println(BloodType.O.canGiveTo(BloodType.O));
		System.out.println(BloodType.O.canGiveTo(BloodType.A));
		System.out.println(BloodType.A.canGiveTo(BloodType.A));
		System.out.println(BloodType.A.canGiveTo(BloodType.A));
		System.out.println(BloodType.B.canGiveTo(BloodType.A));
		System.out.println(BloodType.B.canGiveTo(BloodType.B));
		System.out.println(BloodType.AB.canGiveTo(BloodType.O));
		System.out.println(BloodType.AB.canGiveTo(BloodType.AB));
		
	}
}
