package bdp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

public class HogwildMain {

	public String trainUrl() {
		URL url = getClass().getResource("/resources/data/train.txt");
    	return url.getFile().toString().substring(1);
	}

	public String testUrl() {
		URL url = getClass().getResource("/resources/data/test.txt");
    	return url.getFile().toString().substring(1);
	}
	
	public String testLabelUrl() {
		URL url = getClass().getResource("/resources/data/test_label.txt");
    	return url.getFile().toString().substring(1);
	}
	
    public static void main(String[] args) throws IOException, InterruptedException {    	
        CPDataSet ds = new CPDataSet(new HogwildMain().trainUrl(), true);
        CPDataSet ts = new CPDataSet(new HogwildMain().testUrl(), false);
        String labs = new HogwildMain().testLabelUrl();
        HogwildSGD sgd = new HogwildSGD(ds, ts, labs, 0.001, 0.0);
        CPWeights weights =   sgd.run();
        printResults(weights);
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
