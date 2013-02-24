import hogwild_abstract.HogwildWeights;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;


public class CPWeights extends HogwildWeights{
    double w0;
    /*
     * query.get("123") will return the weight for the feature:
     * "token 123 in the query field".
     */
    Map<Integer, Double> wTokens;
    double wPosition;
    double wDepth;
    double wAge;
    double wGender;
    
    Map<Integer, Integer> accessTime; // keep track of the access timestamp of feature weights.
                                     // Using this to do delayed regularization.
    
    public CPWeights() {
        w0 = wAge = wGender = wDepth = wPosition = 0.0;
        wTokens = new HashMap<Integer, Double>();
        accessTime = new HashMap<Integer, Integer>();
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
        for (double w : wTokens.values())
            l2 += w * w;
        return Math.sqrt(l2);
    }

    /**
     * @return the l0 norm of this weight vector.
     */
    public int l0norm() {
        return 4 + wTokens.size();
    }
}
