import util.EvalUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class HogwildMain {
    private static final int iterations = 10;

    public static void main(String[] args) throws IOException, InterruptedException {
        CPDataSet ds = new CPDataSet("data/train.txt", true);
        CPDataSet testData = new CPDataSet("data/test.txt", false);
        ArrayList<Double> ctr = parseCTR("data/test_label.txt");
        System.err.println("Finished loading data...");

        HogwildSGD sgd = new HogwildSGD(ds, .001, .0, AlgorithmType.REPLICATE);
        double averageError = 0;
        double averageRuntime = 0;
        List<CPWeights> weightVector = new ArrayList<CPWeights>();
        List<Double> runtimeVector = new ArrayList<Double>();

        //calculate weights
        for (int i = 0; i < iterations; i++) {
            System.err.println("running iteration " + i);
            double start = System.currentTimeMillis();
            weightVector.add(sgd.run(4));
            double end = System.currentTimeMillis();
            printResults(weightVector.get(weightVector.size() - 1));
            runtimeVector.add(end - start);
            System.err.println("runtime: " + (end - start));
            averageRuntime += runtimeVector.get(runtimeVector.size() - 1)/iterations;
        }

        //calculate predictions
        for (int i = 0; i < iterations; i++) {
            ArrayList<Double> predictions = sgd.predict(testData, weightVector.get(i));
            double error = ReportResults.getRMSE(predictions, ctr);
            averageError += error/iterations;
            System.out.println(4 + "," + error + "," + runtimeVector.get(i));
        }

        //report averages
        System.out.println("Average Error For " + iterations + " Iterations: " + averageError);
        System.out.println("Average Runtime For " + iterations + " Iterations: " + averageRuntime);
    }

    public static ArrayList<Double> parseCTR(String path) {
        try {
            Scanner input = new Scanner(new BufferedReader(new FileReader(path)));
            ArrayList<Double> ctr = new ArrayList<Double>();
            while (input.hasNextDouble()) {
                ctr.add(input.nextDouble());
            }
            return ctr;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
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
