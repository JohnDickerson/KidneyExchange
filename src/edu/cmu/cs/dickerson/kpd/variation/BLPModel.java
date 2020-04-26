package edu.cmu.cs.dickerson.kpd.variation;

import java.util.Arrays;
import java.util.TreeMap;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;

/*
 * Stores a preference model based on Berry, Levinsohn, and Pakes (1995).
 * Estimates donor's i preference for patient j in terms of
 *      a) a score (used as an edge weight)
 *      b) a rank (rank of patient j in i's preference ordering over 8 profiles)
 */
public class BLPModel {

    // Model parameters learned from Kidney Allocation Preferences (2017) survey responses
    private static final double[] MEANS = {8.18, 5.69, 3.53};
    private static final double[][] COVARIANCES = {
            {20.47, 2.54, 4.56},
            {2.54, 11.07, 1.30},
            {4.56, 1.30, 7.16}
    };
    private static final MultivariateNormalDistribution DISTRIBUTION = new MultivariateNormalDistribution(MEANS, COVARIANCES);

    // Features of each profile [Young, Rare, Healthy] (not [Old, Frequently, Cancer])
    private static final int[][] profileFeatures = {
        {1, 1, 1},    // profile 1 (YRH)
        {1, 0, 1},    // profile 2 (YFH)
        {1, 1, 0},    // profile 3 (YRC)
        {1, 0, 0},    // profile 4 (YFC)
        {0, 1, 1},    // profile 5 (ORH)
        {0, 0, 1},    // profile 6 (OFH)
        {0, 1, 0},    // profile 7 (ORC)
        {0, 0, 0}     // profile 8 (OFC)
   };

    private double[] beta;
    private TreeMap<Double, Integer> scoreMap;

    public BLPModel() {
        this.beta = sampleBeta();
        this.scoreMap = calcScoreMap(this.beta);

        System.out.println("Beta:\t" + Arrays.toString(this.beta));
        this.printScores();
    }

    private void printScores(){
        this.scoreMap.forEach((key, value) -> System.out.print(key + " : profile " + value + ";\t"));
        System.out.println("");
    }

    private static double[] sampleBeta() {
        return DISTRIBUTION.sample();
    }

    /*
     * Calculates map from score : profileID
     * score = (beta_1*Y) + (beta_2*R) + (beta_3*H)
     */
    private static TreeMap<Double, Integer> calcScoreMap(double[] beta) {
        TreeMap<Double, Integer> scoreMap = new TreeMap<>();
        for (int i = 0; i < profileFeatures.length; i++) {
            int profileId = i+1;
            int[] features = profileFeatures[i];
            double score = beta[0]*features[0] + beta[1]*features[1] + beta[2]*features[2];
            scoreMap.put(score, profileId);
        }

        if (scoreMap.keySet().size() != 8) {
            System.out.println("ERROR: scoreMap with beta " + Arrays.toString(beta) + " doesn't have 8 keys. ScoreMap:");
            scoreMap.forEach((key, value) -> System.out.print(key + " : profile " + value + ";\t"));
            System.out.println("");
        }

        TreeMap<Double, Integer> normalizedScoreMap = normalize(scoreMap);

        return normalizedScoreMap;
    }

    public double[] getBeta() {
        return this.beta;
    }

    public double getWeight(int toVertexProfileID) {
        Object[] scores = this.scoreMap.keySet().toArray();
        for (Object score : scores) {
            if (this.scoreMap.get(score).equals(toVertexProfileID)) {
                return (Double) score;
            }
        }
        System.out.println("ERROR: scoreMap does not contain score for profile ID " + Integer.toString(toVertexProfileID) + ". ScoreMap: ");
        this.printScores();
        return -1;
    }

    public int getRank(int toVertexProfileID) {
        Object[] scores = this.scoreMap.keySet().toArray();
        for (int i = 0; i < scores.length; i++) {
            Object score = scores[i];
            if (this.scoreMap.get(score).equals(toVertexProfileID)) {
                return 8 - i;
            }
        }
        System.out.println("ERROR: scoreMap does not contain score for profile ID " + Integer.toString(toVertexProfileID) + ". ScoreMap: ");
        this.printScores();
        return -1;
    }

    private static TreeMap<Double, Integer> normalize(TreeMap<Double, Integer> scoreMap) {
        Object[] scores = scoreMap.keySet().toArray();
        double max = (double) scores[scores.length-1];
        double min = (double) scores[0];
        double ratio = 1/(max - min);

        TreeMap<Double, Integer> normalized = new TreeMap();
        for (Object s : scores) {
            double score = (double) s;
            double newScore = ratio*(score - max) + 1;
            normalized.put(newScore, scoreMap.get(s));
        }
        return normalized;
    }

}
