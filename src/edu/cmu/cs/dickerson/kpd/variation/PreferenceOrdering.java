package edu.cmu.cs.dickerson.kpd.variation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class PreferenceOrdering {
    private int[] ordering;

    public PreferenceOrdering(int[] ordering) {
        if (ordering.length != 8) {
            System.out.println("ERROR: preference ordering must contain 8 profiles, but ordering " + Arrays.toString(this.ordering) + " contains only " + ordering.length);
        }
        else {
            this.ordering = ordering;
        }
    }

    public int preferred(int profileA, int profileB) {
        for (int profile : this.ordering) {
            if (profile == profileA) {
                return profileA;
            }
            else if (profile == profileB) {
                return profileB;
            }
        }
        System.out.println("ERROR: profiles " + profileA + " and " + profileB + " not in preference ordering: " + Arrays.toString(this.ordering));
        return -1;
    }

    // give rank of supplied profile (1 is best, 8 is worst)
    public int rank(int profile) {
        for (int i = 0; i < this.ordering.length; i++) {
            if (this.ordering[i] == profile) {
                return i+1;
            }
        }
        System.out.println("ERROR: profile " + profile + " not in preference ordering: " + Arrays.toString(this.ordering));
        return -1;
    }

    public void print(){
        System.out.println("Preference Ordering: " + Arrays.toString(this.ordering));
    }

    public static void main(String[] args) {
        // read in preference orderings
        List<List<Integer>> orderings = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(VariationDriver.INPUT_PATH + "preference_orderings.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",");
                orderings.add(Arrays.asList(vals).stream()
                        .map(s -> Integer.parseInt(s))
                        .collect(Collectors.toList()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // sample one ordering from list
        int r = new Random().nextInt(orderings.size());
        List<Integer> ordering_list = orderings.get(r);
        int[] ordering = ordering_list.stream().mapToInt(i->i).toArray();

        // initialize PreferenceOrdering with that ordering
        PreferenceOrdering preferenceOrdering = new PreferenceOrdering(ordering);
        preferenceOrdering.print();
        System.out.println(r);

    }
}
