import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;


public class HogwildMain {
    public static void main(String[] args) throws IOException {
    	Class loader = HogwildMain.class.getClass();
    	URL url = loader.getResource("/data/train.txt");
    	System.out.println(url);
        CPDataSet ds = new CPDataSet("data/train.txt", true);
        HogwildSGD sgd = new HogwildSGD(ds, .01, .0);
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