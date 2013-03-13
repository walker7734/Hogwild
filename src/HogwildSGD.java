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

        resetFields();

        //setup thread pool
        ExecutorService pool = Executors.newFixedThreadPool(coresToUse);

        //initialize each thread, should be equal to max number of cores for max
        //efficiency
        for (int thread = 0; thread < coresToUse; thread++) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    if (TYPE == AlgorithmType.SINGLE_THREAD && updatingThread == 0) {
                        updatingThread = Thread.currentThread().getId();
                    }

                    CPWeights result = new CPWeights();
                    for (int i = 0; i < data.getSize() * 4/ coresToUse; i++) {
                        if (TYPE == AlgorithmType.REPLICATE) {
                            sampleAndUpdate(result);

                        } else {
                            sampleAndUpdate(null);
                        }
                    }

                    if (TYPE == AlgorithmType.REPLICATE) {
                        updateSharedWeights(result, coresToUse);
                    }
                }
            });
        }

        pool.shutdown();

        //block until all threads complete
        pool.awaitTermination(120, TimeUnit.HOURS);
        return weights;
    }

    //update weights by averaging the weights returned from each thread.
    private synchronized void updateSharedWeights(CPWeights threadWeights, int threadCount) {
        System.err.println("Replicated Weights");
        System.err.println(threadWeights);
        weights.w0 += threadWeights.w0/threadCount;
        weights.wAge += threadWeights.wAge/threadCount;
        weights.wDepth += threadWeights.wDepth/threadCount;
        weights.wGender += threadWeights.wGender/threadCount;
        weights.wPosition += threadWeights.wPosition/threadCount;
    }


    private void resetFields() {
        updatingThread = 0;

        //clear weights and start from fresh
        weights = new CPWeights();

        averageLoss = 0;
    }

    public double calculateProbability(double weightProduct) {
        return Math.exp(weightProduct) / (1.0 + Math.exp(weightProduct));
    }

    public void calculateWeight(CPDataInstance feature,
                                       double gradient,
                                       CPWeights threadWeights) {


        //determine method for updating non-sparse data
        switch (TYPE) {
            case RANDOM: randomizeNonSparseUpdate(gradient, feature);
                         break;
            case LOCK: lockNonSparseUpdate(gradient, feature);
                        break;
            case SINGLE_THREAD:
                        oneThreadUpdate(gradient, feature);
                        break;
            case REPLICATE:
                        replicateUpdate(gradient, feature, threadWeights);
                        break;
            default:    normalNonSparseUpdate(gradient, feature, null);
                        break;
        }


        //update tokens, sparse data
        int[] tokens = feature.tokens;
        for (int token : tokens) {
            double tokenWeight = weights.wTokens[token];
            weights.wTokens[token] =tokenWeight * (1 - lambda * step) + (step * gradient);
        }
    }

    private void normalNonSparseUpdate(double gradient, CPDataInstance feature, CPWeights threadWeights) {
        CPWeights weightsToUse;
        if (threadWeights == null) {
            weightsToUse = weights;
        } else {
            weightsToUse = threadWeights;
        }

        weightsToUse.w0 = weightsToUse.w0 + step*gradient;
        weightsToUse.wAge = weightsToUse.wAge*(1 - lambda*step) + step*(feature.age*gradient);
        weightsToUse.wGender = weightsToUse.wGender*(1 - lambda*step) + step*(feature.gender*gradient);
        weightsToUse.wPosition = weightsToUse.wPosition*(1 - lambda*step) + step*(feature.position*gradient);
        weightsToUse.wDepth = weightsToUse.wDepth*(1 - lambda*step) + step*(feature.depth*gradient);
    }

    private synchronized void lockNonSparseUpdate(double gradient, CPDataInstance feature) {
        normalNonSparseUpdate(gradient, feature, null);
    }

    //randomizes the updates of the non sparse data so that the data is treated as sparse even if it is not.
    //This minimizes data collisions that would otherwise occur on every iteration.  However, this is likely
    //to decrease accuracy. The probability of each thread updating any particular weight can be adjusted by
    //changing the UPDATE_FREQ field.
    private void randomizeNonSparseUpdate(double gradient, CPDataInstance feature) {
        if (randy.nextInt(UPDATE_FREQ) == 0) {
           normalNonSparseUpdate(gradient, feature, null);
        }
    }

    private void oneThreadUpdate(double gradient, CPDataInstance feature) {
        if (Thread.currentThread().getId() == updatingThread) {
            normalNonSparseUpdate(gradient, feature, null);
        }
    }

    private void replicateUpdate(double gradient, CPDataInstance instance, CPWeights toUpdate) {
        normalNonSparseUpdate(gradient, instance, toUpdate);
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
    private double computeWeightFeatureProduct(CPWeights threadWeight, CPDataInstance instance) {
        CPWeights weightsToUse;
        if (threadWeight == null) {
            weightsToUse = weights;
        } else {
            weightsToUse = threadWeight;
        }
        double innerProduct;
        innerProduct = weightsToUse.w0 + instance.depth * weightsToUse.wDepth +
                instance.position * weightsToUse.wPosition;
        innerProduct += instance.gender * weightsToUse.wGender;
        innerProduct += instance.age * weightsToUse.wAge;
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
        ArrayList<Double> prediction = new ArrayList<Double>();
        for (int i = 0; i < dataset.getSize(); i++) {
            CPDataInstance instance = (CPDataInstance)dataset.getInstanceAt(i);
            double weightProduct = computeWeightFeatureProduct(predictionWeights, instance);
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
    public CPWeights sampleAndUpdate(CPWeights threadWeights) {
        boolean  withReplacement = false;
        CPDataInstance dataPoint = (CPDataInstance)data.getRandomInstance(withReplacement);

        //calculate the probability
        double innerProduct = computeWeightFeatureProduct(threadWeights, dataPoint);
        double prob = calculateProbability(innerProduct);

        //calculate the gradient
        int y = dataPoint.getLabel();
        double gradient = (y - prob);

        // calculate averageLoss
        averageLoss = calculateAverageLoss(averageLoss, prob, y);

        //update weights
        calculateWeight(dataPoint, gradient, threadWeights);

        return threadWeights;

    }
}