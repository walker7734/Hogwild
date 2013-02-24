package bdp;

import bdp.util.EvalUtil;

import java.util.ArrayList;


/**
 * Created with IntelliJ IDEA.
 * User: bdwalker
 * Date: 2/23/13
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReportResults {
    public static void printWeights(double[] weight) {
        for (int i = 0; i < 500; i++) {
            if (weight[i] != 0) {
                System.out.println("token " + i + " weight:" + weight[i]);
            }
        }
    }

    public static void printRMSE(ArrayList<Double> predictions, String solnFile) {
        System.out.println("RMSE: " + EvalUtil.eval(solnFile, predictions));
    }

}
