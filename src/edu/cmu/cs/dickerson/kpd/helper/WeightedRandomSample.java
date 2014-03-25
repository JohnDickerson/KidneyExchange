package edu.cmu.cs.dickerson.kpd.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * 
 * Idea: http://stackoverflow.com/questions/6409652/random-weighted-selection-java-framework
 * @author spook
 *
 * @param <E>
 */
public class WeightedRandomSample<E> {

	private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
	private final Random r;
	private double total = 0;

	public WeightedRandomSample() {
		this(new Random());
	}
	public WeightedRandomSample(Random r) {
		this.r = r;
	}

	public void add(double weight, E elem) {
		if (weight <= 0) return;
		total += weight;
		map.put(total, elem);
	}

	/**
	 * Takes a random weighted sample from the collection with replacement
	 * @return
	 */
	public E sampleWithReplacement() {
		return map.ceilingEntry(r.nextDouble() * total).getValue();
	}

	/**
	 * Takes a random weighted sample from the collection without replacement.  This
	 * method is dumb right now; every removal requires O(n) updates to the underlying
	 * collection, so O(n^2) to create a random weighted complete ordering.  Can do this
	 * with a heap in like O(n logn)-ish time if this becomes an issue
	 * @return
	 */
	public E sampleWithoutReplacement() {

		double rVal = r.nextDouble() * total;
		// Take a weighted random sample
		Entry<Double, E> sample = map.ceilingEntry(rVal);
		E sampleValue = sample.getValue();

		// Compute the mass that this sample added to collection and remove it
		Entry<Double, E> neighbor = map.floorEntry(rVal);
		double adjustment;
		if(null==neighbor) {
			// Sampled the first element in the list; key is also the weight
			adjustment = sample.getKey();
		} else {
			// Weight of sample is key minus the closest (lower) neighbor's key
			adjustment = (sample.getKey() - neighbor.getKey());
		}
		this.total -= adjustment;

		// Remove the sample and update the weights of all elements after the sample
		NavigableMap<Double, E> updatedElems = new TreeMap<Double, E>();

		Iterator<Map.Entry<Double,E>> needUpdatedElemsIt = map.tailMap(sample.getKey(), true).entrySet().iterator();
		boolean first=true;
		while(needUpdatedElemsIt.hasNext()) {
			
			Map.Entry<Double, E> entry = needUpdatedElemsIt.next();
			if(first) { 
				// Always remove the element we sampled
				needUpdatedElemsIt.remove(); 
				first=false;
			} else {
				// Move elements' keys down by the weight of the sample
				updatedElems.put(entry.getKey()-adjustment, entry.getValue());
				needUpdatedElemsIt.remove();
			}
		}
		map.putAll(updatedElems);

		return sampleValue;
	}
	
	public List<E> weightedPermutation() {
		List<E> permutation = new ArrayList<E>();
		while(!map.isEmpty()) {
			permutation.add(sampleWithoutReplacement());
		}
		return permutation;
	}
	
	public int size() {
		return map.size();
	}

	/**
	 * Reset this set (clears underlying map)
	 */
	public void clear() {
		this.map.clear();
		this.total = 0.0;
	}
	
	@Override
	public String toString() {
		return "WeightedRandomSample [map=" + map + ", total=" + total + "]";
	}
}
