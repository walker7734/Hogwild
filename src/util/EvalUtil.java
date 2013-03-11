package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

public class EvalUtil {
	/**
	 * Evaluates the model by computing the weighted rooted mean square error of
	 * between the prediction and the true labels.
	 * 
	 * @param pathToSol
	 * @param ctr_prediction
	 * @return the weighted rooted mean square error of between the prediction
	 *         and the true labels.
	 */
	public static double eval(String pathToSol, ArrayList<Double> ctr_prediction) {
		try {
			@SuppressWarnings("resource")
            Scanner sc = new Scanner(new BufferedReader(new FileReader(
					pathToSol)));
			int size = ctr_prediction.size();
			double wmse = 0.0;
			for (int i = 0; i < size; i++) {
				double ctr = Double.parseDouble(sc.nextLine());
				wmse += Math.pow((ctr - ctr_prediction.get(i)), 2);
			}
			wmse /= size;
			return Math.sqrt(wmse);
		} catch (Exception e) {
			e.printStackTrace();
			return Double.MAX_VALUE;
		}
	}

    public static double eval(ArrayList<Double> ctr, ArrayList<Double> ctr_prediction) {
        try {
            int size = ctr_prediction.size();
            double wmse = 0.0;
            for (int i = 0; i < size; i++) {
                wmse += Math.pow((ctr.get(i) - ctr_prediction.get(i)), 2);
            }
            wmse /= size;
            return Math.sqrt(wmse);
        } catch (Exception e) {
            e.printStackTrace();
            return Double.MAX_VALUE;
        }
    }
	
	/**
	 * Evaluates the model by computing the weighted rooted mean square error of
	 * between the prediction and the true labels.
	 * 
	 * @param pathToSol
	 * @return the weighted rooted mean square error of between the prediction
	 *         and the true labels.
	 */
	public static double eval(String pathToSol, String pathToPrediction) {
		ArrayList<Double> ctr_prediction = new ArrayList<Double>();
		try {
			@SuppressWarnings("resource")
            Scanner sc = new Scanner(new BufferedReader(new FileReader(pathToPrediction)));
			while (sc.hasNextLine()) {
				try {
					ctr_prediction.add(Double.parseDouble(sc.nextLine()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Double.MAX_VALUE;
		}
		
		try {
			@SuppressWarnings("resource")
            Scanner sc = new Scanner(new BufferedReader(new FileReader(
					pathToSol)));
			int size = ctr_prediction.size();
			double wmse = 0.0;
			for (int i = 0; i < size; i++) {
				double ctr = Double.parseDouble(sc.nextLine());
				wmse += Math.pow((ctr - ctr_prediction.get(i)), 2);
			}
			wmse /= size;
			return Math.sqrt(wmse);
		} catch (Exception e) {
			e.printStackTrace();
			return Double.MAX_VALUE;
		}
	}
	
	
	/**
	 * Evaluates the model by computing the weighted rooted mean square error of
	 * between the prediction and the true labels.
	 * 
	 * @param pathToSol
	 * @return the weighted rooted mean square error of between the prediction
	 *         and the true labels.
	 */
	public static double evalBaseLine(String pathToSol, double avg_ctr) {
		try {
			@SuppressWarnings("resource")
            Scanner sc = new Scanner(new BufferedReader(new FileReader(
					pathToSol)));
			double rmse = 0.0;
			int count = 0;
			while(sc.hasNextLine()) {
				double ctr = Double.parseDouble(sc.nextLine());
				rmse += Math.pow(ctr - avg_ctr, 2);
				count++;
			}
			rmse /= count;
			return Math.sqrt(rmse);
		} catch (Exception e) {
			e.printStackTrace();
			return Double.MAX_VALUE;
		}
	}
	
	/**
	 * Evaluates the model by computing the weighted rooted mean square error of
	 * between the prediction and the true labels, using the including list to decide whether the each
	 * test data point should be included in evaluation.
	 * 
	 * This is useful for evaluating the RMSE on a subset of test data (with common users ...).
	 * @param pathToSol
	 * @param ctr_prediction
	 * @param includingList
	 * @return
	 */
	public static double evalWithIncludingList(String pathToSol, ArrayList<Double> ctr_prediction, ArrayList<Boolean> includingList) {
		try {
			@SuppressWarnings("resource")
            Scanner sc = new Scanner(new BufferedReader(new FileReader(
					pathToSol)));
			int size = ctr_prediction.size();
			double wmse = 0.0;
			int total = 0;
			for (int i = 0; i < size; i++) {
				String str = sc.nextLine();
				if (!includingList.get(i))
					continue;				
				
				double ctr = Double.parseDouble(str);
				wmse += Math.pow((ctr - ctr_prediction.get(i)), 2);
				total++;
			}
			wmse /= total;
			return Math.sqrt(wmse);
		} catch (Exception e) {
			e.printStackTrace();
			return Double.MAX_VALUE;
		}
	}

}
