import util.EvalUtil;

import java.util.ArrayList;


/**
 * Created with IntelliJ IDEA.
 * User: bdwalker
 * Date: 2/23/13
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReportResults {

    /**
     * Simple static method to print out the RMSE.
     * @param predictions ArrayList<Double> containing the probabilities for each of
     *                    of the test data elements.
     *
     * @param solnFile String representing the file path to obtain the labels that
     *                 correspond to the test data elements
     */
    public static void printRMSE(ArrayList<Double> predictions, String solnFile) {
        System.out.println("RMSE: " + EvalUtil.eval(solnFile, predictions));
    }

    public static void printRMSE(ArrayList<Double> predictions, ArrayList<Double> actual) {
        System.out.println("RMSE: " + EvalUtil.eval(actual, predictions));
    }

    public static double getRMSE(ArrayList<Double> predictions, String solnFile) {
        return EvalUtil.eval(solnFile, predictions);
    }

    public static double getRMSE(ArrayList<Double> predictions, ArrayList<Double> actual) {
        return EvalUtil.eval(actual, predictions);
    }

}
