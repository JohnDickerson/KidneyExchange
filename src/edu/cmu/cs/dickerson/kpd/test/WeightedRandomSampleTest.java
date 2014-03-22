package edu.cmu.cs.dickerson.kpd.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.helper.WeightedRandomSample;

public class WeightedRandomSampleTest {

	private WeightedRandomSample<String> createTestSet() {

		WeightedRandomSample<String> S = new WeightedRandomSample<String>();
		S.add(1, "Very Rare");
		S.add(10, "Rare");
		S.add(100, "Common");

		return S;
	}

	@Test
	public void test() {

		WeightedRandomSample<String> S = createTestSet();

		// Sample from this weighted set with replacement a bunch of times, 
		// and count how many times each element is sampled
		Map<String, Integer> withReplacementCounts = new HashMap<String, Integer>();
		withReplacementCounts.put("Very Rare", 0);
		withReplacementCounts.put("Rare", 0);
		withReplacementCounts.put("Common", 0);
		for(int idx=0; idx<10000; idx++) {
			String sample = S.sampleWithReplacement();
			withReplacementCounts.put(sample, withReplacementCounts.get(sample)+1);
		}
		IOUtil.dPrintln(this.getClass().getSimpleName(), withReplacementCounts);

		// If the weighted sample counts are off, fail.  This can happen randomly rarely, but whatever
		if(withReplacementCounts.get("Very Rare") > withReplacementCounts.get("Rare") ||
				withReplacementCounts.get("Rare") > withReplacementCounts.get("Common")) {
			fail("Random with replacement sample ordering looked fishy.  Try re-running.");
		}


		// Sample from this weighted set without replacement, and count how many times
		// each ordering is sampled
		Map<String, Integer> withoutReplacementCounts = new HashMap<String, Integer>();
		withoutReplacementCounts.put("CRV", 0);
		withoutReplacementCounts.put("CVR", 0);
		withoutReplacementCounts.put("RCV", 0);
		withoutReplacementCounts.put("RVC", 0);
		withoutReplacementCounts.put("VCR", 0);
		withoutReplacementCounts.put("VRC", 0);
		for(int idx=0; idx<100000; idx++) {
			S = createTestSet();
			String first = S.sampleWithoutReplacement();
			String second = S.sampleWithoutReplacement();
			String third = S.sampleWithoutReplacement();

			if(null==first || null==second || null==third) {
				fail("Null sample when sample should not have been null");
			}

			if(first.equals(second) || second.equals(third) || first.equals(third)) {
				fail("Sampling without replacement sampled the same item twice.\nfirst=" + first + ", second=" + second + ", third=" + third);
			}

			String ordering = first.equals("Common") ? (second.equals("Rare") ? "CRV" : "CVR")
					: (first.equals("Rare") ? (second.equals("Common") ? "RCV" : "RVC")
							: (second.equals("Common") ? "VCR" : "VRC")
							);
			withoutReplacementCounts.put(ordering, withoutReplacementCounts.get(ordering)+1);
		}
		IOUtil.dPrintln(this.getClass().getSimpleName(), withoutReplacementCounts);

		if(withoutReplacementCounts.get("CRV") <= withoutReplacementCounts.get("VRC") ||
				withoutReplacementCounts.get("CRV") <= withoutReplacementCounts.get("RVC")) {
			fail("Random without replacement sample ordering looked fishy.  Try re-running.");
		}
	}

}
