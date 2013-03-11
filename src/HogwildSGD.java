import hogwild_abstract.HogwildDataInstance;
import hogwild_abstract.HogwildDataSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HogwildSGD {
    private static HogwildDataSet data;
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private static double lambda;
    private static CPWeights weights;
    private static AlgorithmType TYPE;
    private static final Random randy = new Random();
    private static int UPDATE_FREQ;
    private static double step;
    private static final Object lock = new Object();
    private static double averageLoss;
    private static long updatingThread;

    public HogwildSGD (HogwildDataSet dataSet, double step, double lambda, AlgorithmType type) {
        data = dataSet;
        HogwildSGD.lambda = lambda;
        HogwildSGD.step = step;
        TYPE = type;
        weights = new CPWeights();
    }

    public CPWeights run(int cores) throws IOException, InterruptedException {
        if (data == null) {
            throw new IllegalArgumentException("HogwildSGD: data set cannot be null");
        }

        final int coresToUse = (cores < 1 || cores > NUM_CORES) ? NUM_CORES : cores;
        UPDATE_FREQ = coresToUse;

        //clear weights and start from fresh
        weights = new CPWeights();

        //not currently checking for specific convergence
        //TODO figure out how to check for convergence

        //setup thread pool
        ExecutorService pool = Executors.newFixedThreadPool(coresToUse);

        //initialize each thread, should be equal to max number of cores for max
        //efficiency
        for (int thread = 0; thread < coresToUse; thread++) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < data.getSize() * 1 / coresToUse; i++) {
                        sampleAndUpdate();
                    }
                }
            });
        }

        pool.shutdown();

        //block until all threads complete
        pool.awaitTermination(120, TimeUnit.HOURS);

        return weights;
    }

    public double calculateProbability(double weightProduct) {
        return Math.exp(weightProduct) / (1.0 + Math.exp(weightProduct));
    }

    public void calculateWeight(CPDataInstance feature,
                                       double gradient) {


        //determine method for updating non-sparse data
        switch (TYPE) {
            case RANDOM: randomizeNonSparseUpdate(gradient, feature);
                         break;
            case LOCK: lockNonSparseUpdate(gradient, feature);
                        break;
            case SINGLE_THREAD:
                        oneThreadUpdate(gradient, feature);
                        break;
            default:    normalNonSparseUpdate(gradient, feature);
                        break;
        }


        //update tokens, sparse data
        int[] tokens = feature.tokens;
        for (int token : tokens) {
            double tokenWeight = weights.wTokens[token];
            weights.wTokens[token] =tokenWeight * (1 - lambda * step) + (step * gradient);
        }
    }


    private void normalNonSparseUpdate(double gradient, CPDataInstance feature) {
        weights.w0 = weights.w0 + step*gradient;
        weights.wAge = weights.wAge*(1 - lambda*step) + step*(feature.age*gradient);
        weights.wGender = weights.wGender*(1 - lambda*step) + step*(feature.gender*gradient);
        weights.wPosition = weights.wPosition*(1 - lambda*step) + step*(feature.position*gradient);
        weights.wDepth = weights.wDepth*(1 - lambda*step) + step*(feature.depth*gradient);
    }

    private synchronized void lockNonSparseUpdate(double gradient, CPDataInstance feature) {
        normalNonSparseUpdate(gradient, feature);
    }

    //randomizes the updates of the non sparse data so that the data is treated as sparse even if it is not.
    //This minimizes data collisions that would otherwise occur on every iteration.  However, this is likely
    //to decrease accuracy. The probability of each thread updating any particular weight can be adjusted by
    //changing the UPDATE_FREQ field.
    private void randomizeNonSparseUpdate(double gradient, CPDataInstance feature) {
        if (randy.nextInt(UPDATE_FREQ) == 1) {
           normalNonSparseUpdate(gradient, feature);
        }
    }

    private void oneThreadUpdate(double gradient, CPDataInstance feature) {
        if (updatingThread == 0) updatingThread = Thread.currentThread().getId();
        if (Thread.currentThread().getId() == updatingThread) {
            normalNonSparseUpdate(gradient, feature);
        }
    }

    /**
     * Not currently being used
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

    /**
     *
     * Not currently being used
     */
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

    /**
     * Predicts the probability of a click based on the training data
     */
    public ArrayList<Double> predict(HogwildDataSet dataset, CPWeights predictionWeights) {
        weights = predictionWeights;
        ArrayList<Double> prediction = new ArrayList<Double>();
        for (int i = 0; i < dataset.getSize(); i++) {
            CPDataInstance instance = (CPDataInstance)dataset.getInstanceAt(i);
            double weightProduct = computeWeightFeatureProduct(instance);
            prediction.add(calculateProbability(weightProduct));
        }
        return prediction;
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

        //calculate the probability
        double innerProduct = computeWeightFeatureProduct(dataPoint);
        double prob = calculateProbability(innerProduct);

        //calculate the gradient
        int y = dataPoint.getLabel();
        double gradient = (y - prob);

        // calculate averageLoss
        averageLoss = calculateAverageLoss(averageLoss, prob, y);

        //update weights
        calculateWeight(dataPoint, gradient);

    }
}
