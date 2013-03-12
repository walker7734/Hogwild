import hogwild_abstract.HogwildWeights;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CPWeights extends HogwildWeights{

    //volatile to allow for atomic updates
    double w0;
    double[] wTokens;
    double wPosition;
    double wDepth;
    double wAge;
    double wGender;
    
    Map<Integer, Integer> accessTime; // keep track of the access timestamp of feature weights.
                                     // Using this to do delayed regularization.
    
    public CPWeights() {
        w0 = wAge = wGender = wDepth = wPosition = 0.0;
        wTokens = new double[2000000];
        accessTime = new ConcurrentHashMap<Integer, Integer>();
    }

    @Override
    public String toString() {
        DecimalFormat myFormatter = new DecimalFormat("###.##");
        StringBuilder builder = new StringBuilder();
        builder.append("Intercept: " + myFormatter.format(w0) + "\n");
        builder.append("Depth: " + myFormatter.format(wDepth) + "\n");
        builder.append("Position: " + myFormatter.format(wPosition) + "\n");
        builder.append("Gender: " + myFormatter.format(wGender) + "\n");
        builder.append("Age: " + myFormatter.format(wAge) + "\n");
        builder.append("Tokens: " + wTokens.toString() + "\n");
        return builder.toString();
    }

    /**
     * @return the l2 norm of this weight vector.
     */
    public double l2norm() {
        double l2 = w0 * w0 + wAge * wAge + wGender * wGender
                                + wDepth*wDepth + wPosition*wPosition;
        for (double w : wTokens)
            l2 += w * w;
        return Math.sqrt(l2);
    }

    /**
     * @return the l0 norm of this weight vector.
     */
    public int l0norm() {
        return 4 + wTokens.length;
    }
}
