import hogwild_abstract.HogwildDataSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HogwildSGD {
    private static HogwildDataSet data;
    private static final double EPSILON = .1;
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private static double lambda;
    private static CPWeights weights;
    private static CPWeights oldWeights;
    private static int count;
    private static double step;
    private static double averageLoss;
    private static double[] tokenWeights;

    public HogwildSGD (HogwildDataSet dataSet, double step, double lambda) {
        data = dataSet;
        HogwildSGD.lambda = lambda;
        this.step = step;
        weights = new CPWeights();
        oldWeights = new CPWeights();
        tokenWeights = new double[1500000];
    }

    public CPWeights run() {
        if (data == null) {
            throw new IllegalArgumentException("HogwildSGD: data set cannot be null");
        }

        boolean converged = false;
        while (!converged) {

             threadCycle(1);
            //setup thread pool
            ExecutorService pool = Executors.newFixedThreadPool(NUM_CORES);
            for (int thread = 0; thread < 2; thread++) {
                pool.submit(new Runnable() {
                    @Override
                    public void run() {
                        threadCycle(data.getSize());
                    }
                });
            }

            pool.shutdown();

            //wait
            while (!pool.isShutdown()) {
                try {
                    pool.awaitTermination(20, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            converged = didConverge();
        }
        // preformFullRegularization(count);
        //do a full pass for delayed regularization
        //preformFullRegularization(count);
        System.out.println(averageLoss/count);
        return weights;
    }

    public static double calculateProbability(double weightProduct) {
        return Math.exp(weightProduct) / (1.0 + Math.exp(weightProduct));
    }

    public static void calculateWeight(CPDataInstance feature,
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
            //if (!weights.wTokens.containsKey(token)) {
              //  weights.wTokens.put(token, 0.0);
            //}
            double tokenWeight = tokenWeights[token]; //weights.wTokens.get(token);
            tokenWeights[token] = tokenWeight * (1 - lambda * step) + (step * gradient);
            //weights.wTokens.put(token, tokenWeight * (1 - lambda * step) + (step * gradient));
            weights.accessTime.put(token, timestamp);
        }
    }

    /**
     * Apply delayed regularization to the weights corresponding to the given tokens.
     */
    private void performDelayedRegularization(int[] tokens,
                                              int now) {

        for (int token : tokens) {
            //if (weights.accessTime.containsKey(token)) {
                /*if (!weights.wTokens.containsKey(token)) {
                    weights.wTokens.put(token, 0.0);
                }*/
                double weight = tokenWeights[token]; //weights.wTokens.get(token);
                int exponent = now - weights.accessTime.get(token) - 1;
                double regularizePow = Math.pow((1.0 - lambda * step), exponent);
                tokenWeights[token] = weight * regularizePow;
                //weights.wTokens.put(token, weight * regularizePow);
           // }
        }
    }

    private void preformFullRegularization(int now) {

        //for (int token : weights.wTokens.keySet()) {
        for (int i = 0; i < tokenWeights.length; i++) {
            double weight = tokenWeights[i];// weights.wTokens.get(token);
            int exponent = now - weights.accessTime.get(i) - 1;
            double regularizePow = Math.pow((1.0 - lambda*step), exponent);
            tokenWeights[i] = weight * regularizePow;
            //weights.wTokens.put(token, weight*regularizePow);
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
            /*if (!weights.wTokens.containsKey(token)) {
                weights.wTokens.put(token, 0.0);
            }  */

            innerProduct += tokenWeights[token]; //weights.wTokens.get(token);
        }
        return innerProduct;
    }

    private boolean didConverge() {
        double maxWeightDifference = 0;
        maxWeightDifference = Math.max(Math.abs(weights.w0 - oldWeights.w0), maxWeightDifference);
        //System.out.println(weights.w0);
        maxWeightDifference = Math.max(Math.abs(weights.wAge - oldWeights.wAge), maxWeightDifference);
        maxWeightDifference = Math.max(Math.abs(weights.wDepth - oldWeights.wDepth), maxWeightDifference);
        maxWeightDifference = Math.max(Math.abs(weights.wPosition - oldWeights.wPosition), maxWeightDifference);
        maxWeightDifference = Math.max(Math.abs(weights.wGender - oldWeights.wGender), maxWeightDifference);
        System.out.println(tokenWeights[0]);
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
    public void train() {
        CPDataInstance dataPoint = (CPDataInstance)data.getRandomInstance(true);

        //increment the current time
        count += 1;

        //perform delayed regularization
        //performDelayedRegularization(dataPoint.tokens, count);

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

    //each thread trains on 1000 cycles before terminating and checking for convergence
    private void threadCycle(int responsibility) {
        for (int i = 0; i < 10000; i++) {
            train();
        }
    }

}
