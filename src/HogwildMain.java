import util.EvalUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


public class HogwildMain {
    private static final int iterations = 10;

    public static void main(String[] args) throws IOException, InterruptedException {
        CPDataSet ds = new CPDataSet("data/train.txt", true);
        CPDataSet testData = new CPDataSet("data/test.txt", false);

        HogwildSGD sgd = new HogwildSGD(ds, .001, .0, AlgorithmType.NORMAL);
        double averageError = 0;
        double averageRuntime = 0;
        for (int i = 0; i < iterations; i++) {
            double start = System.currentTimeMillis();
            CPWeights weights = sgd.run(2);
            double end = System.currentTimeMillis();
            printResults(weights);
            ArrayList<Double> predictions = sgd.predict(testData);
            double error = ReportResults.getRMSE(predictions, "data/test_label.txt");
            double runtime = end - start;
            averageRuntime += runtime/iterations;
            averageError += error/iterations;
            System.out.println(4 + "," + ReportResults.getRMSE(predictions, "data/test_label.txt") + "," + runtime);
        }
        System.out.println("Average Error For " + iterations + " Iterations: " + averageError);
        System.out.println("Average Runtime For " + iterations + " Iterations: " + averageRuntime);
    }

    public static void printResults(CPWeights weights) {
        System.out.println("Weight Results");
        System.out.println("Intercept: \t" + weights.w0);
        System.out.println("Age: \t\t" + weights.wAge);
        System.out.println("Position: \t\t" + weights.wPosition);
        System.out.println("Depth: \t\t" + weights.wDepth);
        System.out.println("Gender: \t\t" + weights.wGender);
    }

}
