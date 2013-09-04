package edu.cmu.cs.dickerson.kpd.structure.generator;

import java.util.Random;

/**
 * A tweak to the published Saidman generator; distributions VERY ROUGHLY
 * mimic the UNOS pool as of April 15, 2013.  Data taken from the KPD Work
 * Group Data Analysis - CMR - June 2013 report.
 * 
 * @author John P. Dickerson
 *
 */
public class SparseUNOSSaidmanPoolGenerator extends SaidmanPoolGenerator {

	public SparseUNOSSaidmanPoolGenerator(Random random) {
		super(random);

		Pr_LOW_PRA = 0.216;  // 0.007 + 0.001 + 0.016 + 0.192 = 0.216
		Pr_MED_PRA = 0.16;   // 0.018 + 0.001 + 0.025 + 0.116 = 0.16
		// Pr_HIGH_PRA = 1.0 - 0.216 - 0.16 = 0.624
		
		Pr_LOW_PRA_INCOMPATIBILITY = 0.50;
		Pr_MED_PRA_INCOMPATIBILITY = 0.80;
		Pr_HIGH_PRA_INCOMPATIBILITY = 0.98;

		Pr_PATIENT_TYPE_O = 0.651;
		Pr_PATIENT_TYPE_A = 0.200;
		Pr_PATIENT_TYPE_B = 0.124;
		// Pr_PATIENT_TYPE_AB = 1.0 - 0.651 - 0.200 - 0.124 = 0.025
		
		Pr_DONOR_TYPE_O = 0.345;
		Pr_DONOR_TYPE_A = 0.459;  // 0.402 + 0.016 + 0.04 + 0.001 = 0.459
		Pr_DONOR_TYPE_B = 0.197;
		// Pr_DONOR_TYPE_AB = 1.0 - 0.345 - 0.459 - 0.197 = 
	}
}
