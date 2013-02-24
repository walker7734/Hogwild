package bdp;

import bdp.hogwild_abstract.HogwildDataInstance;
import bdp.hogwild_abstract.HogwildDataSet;

import bdp.HogwildMain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HogwildSGD {
    private static HogwildDataSet data;
    private static HogwildDataSet tdata;
    private static String testLabel;
    private static final double EPSILON = .1;
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private static double lambda;
    private static CPWeights weights;
    private static CPWeights oldWeights;
    private static int count;
    private static double step;
    private static double averageLoss;

    public HogwildSGD (HogwildDataSet trainDataSet,
    		HogwildDataSet testDataSet, 
    		String testLabels, double step, double lambda) {
        data = trainDataSet;
        tdata = testDataSet;
        testLabel = testLabels;
        HogwildSGD.lambda = lambda;
        HogwildSGD.step = step;
        weights = new CPWeights();
        oldWeights = new CPWeights();
    }

    public CPWeights run() throws IOException, InterruptedException {
        if (data == null) {
            throw new IllegalArgumentException("HogwildSGD: data set cannot be null");
        }

        long startTime = System.currentTimeMillis();
        boolean converged = false;
        while (!converged) {

            //setup thread pool
            ExecutorService pool = Executors.newFixedThreadPool(NUM_CORES);
            System.out.println(NUM_CORES);
            for (int thread = 0; thread < 4; thread++) {
                pool.submit(new Runnable() {
                    @Override
                    public void run() {
                        int count = 0;
                        for (int i = 0; i < data.getSize(); i++) {
                            count++;
                            sampleAndUpdate();
                        }
                        System.out.println(count);
                    }
                });
            }

            pool.shutdown();

            //wait
            pool.awaitTermination(120, TimeUnit.HOURS);
            converged = true;
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Time to completion: " + (endTime - startTime)/1000 + "s");
        ReportResults.printRMSE(predict(tdata), testLabel);
        return weights;
    }

    public double calculateProbability(double weightProduct) {
        return Math.exp(weightProduct) / (1.0 + Math.exp(weightProduct));
    }

    public void calculateWeight(CPDataInstance feature,
                                       double gradient,
                                       int timestamp) {

        weights.w0 = weights.w0 + step*gradient;
        weights.wAge = weights.wAge*(1 - lambda*step) + step*(feature.age*gradient);
        weights.wGender = weights.wGender*(1 - lambda*step) + step*(feature.gender*gradient);
        weights.wPosition = weights.wPosition*(1 - lambda*step) + step*(feature.position*gradient);
        weights.wDepth = weights.wDepth*(1 - lambda*step) + step*(feature.depth*gradient);

        //update tokens
        int[] tokens = feature.tokens;
        for (int token : tokens) {
            double tokenWeight = weights.wTokens[token];
            weights.wTokens[token] =tokenWeight * (1 - lambda * step) + (step * gradient);
        }
    }

    /**
     * Apply delayed regularization to the weights corresponding to the given tokens.
     */
    private void performDelayedRegularization(int[] tokens,
                                              int now) {

        for (int token : tokens) {
            if (weights.accessTime.containsKey(token)) {
                double weight = weights.wTokens[token];
                int exponent = now - weights.accessTime.get(token) - 1;
                double regularizePow = Math.pow((1.0 - lambda * step), exponent);
                weights.wTokens[token] = weight * regularizePow;
            }
        }
    }

    private void preformFullRegularization(int now) {

        for (int i = 0; i < weights.wTokens.length; i++) {
            double weight = weights.wTokens[i];
            int exponent = now - weights.accessTime.get(i) - 1;
            double regularizePow = Math.pow((1.0 - lambda*step), exponent);
            weights.wTokens[i] = weight * regularizePow;
        }
    }

    /**
     * Helper function to compute inner product w^Tx.
     */
    private double computeWeightFeatureProduct(CPDataInstance instance) {
        double innerProduct;
        innerProduct = weights.w0 + instance.depth * weights.wDepth +
                instance.position * weights.wPosition;
        innerProduct += instance.gender * weights.wGender;
        innerProduct += instance.age * weights.wAge;
        int[] tokens = instance.tokens;
        for (int token : tokens) {

            innerProduct += weights.wTokens[token];
        }
        return innerProduct;
    }

    public ArrayList<Double> predict(HogwildDataSet dataset) {
        ArrayList<Double> prediction = new ArrayList<Double>();
        for (int i = 0; i < dataset.getSize(); i++) {
            CPDataInstance instance = (CPDataInstance)dataset.getInstanceAt(i);
            double weightProduct = computeWeightFeatureProduct(instance);
            prediction.add(calculateProbability(weightProduct));
        }
        return prediction;
    }

    private boolean didConverge() {
        double maxWeightDifference = 0;
        maxWeightDifference = Math.max(Math.abs(weights.w0 - oldWeights.w0), maxWeightDifference);
        maxWeightDifference = Math.max(Math.abs(weights.wAge - oldWeights.wAge), maxWeightDifference);
        maxWeightDifference = Math.max(Math.abs(weights.wDepth - oldWeights.wDepth), maxWeightDifference);
        maxWeightDifference = Math.max(Math.abs(weights.wPosition - oldWeights.wPosition), maxWeightDifference);
        maxWeightDifference = Math.max(Math.abs(weights.wGender - oldWeights.wGender), maxWeightDifference);
        return maxWeightDifference < EPSILON;
    }

    public static double calculateAverageLoss(double currentLoss, double prediction, int clicked) {
        int predictedY = (prediction >= .5) ? 1 : 0;
        currentLoss = (currentLoss + Math.pow(predictedY - clicked, 2));
        return currentLoss;
    }

    /**
     * Train the logistic regression model using the training data and the
     * hyperparameters. Return the weights, and record the cumulative loss.
     */
    public void sampleAndUpdate() {
        boolean  withReplacement = false;
        CPDataInstance dataPoint = (CPDataInstance)data.getRandomInstance(withReplacement);

        //increment the current time
        count += 1;

        //calculate the probability
        double innerProduct = computeWeightFeatureProduct(dataPoint);
        double prob = calculateProbability(innerProduct);

        //calculate the gradient
        int y = dataPoint.getLabel();
        double gradient = (y - prob);

        // calculate averageLoss
        averageLoss = calculateAverageLoss(averageLoss, prob, y);

        //update weights
        calculateWeight(dataPoint, gradient, count);

    }
}
