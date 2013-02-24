package bdp;

import bdp.hogwild_abstract.HogwildDataInstance;
import bdp.util.StringUtil;

/**
 * This class represents an instance of the data.
 * 
 * @author haijieg
 * 
 */
public class CPDataInstance extends HogwildDataInstance{
	// Label
	int clicked; // 0 or 1

	// Feature of the page and ad
	int depth; // depth of the session.
	int position; // position of the ad.
	int[] tokens; // list of token ids.
	

	// Feature of the user
	int userid;
	int gender; // user gender indicator -1 for male, 1 for female
	int age;		// user age indicator '1' for (0, 12], '2' for (12, 18], '3' for
							// (18, 24], '4' for (24, 30],
							// 	'5' for (30, 40], and '6' for greater than 40.

	/**
	 * Create a DataInstance from input string.
	 * 
	 * @param line
	 * @param hasLabel
	 *            True if the input string is from training data. False
	 *            otherwise.
	 */
	public CPDataInstance(String line, boolean hasLabel) {
		String[] fields = line.split("\\|");
		int offset = 0;
		if (hasLabel) {
			clicked = Integer.valueOf(fields[0]);
			offset = 1;
		} else {
			clicked = -1;
		}
		depth = Integer.valueOf(fields[offset + 0]);
		position = Integer.valueOf(fields[offset + 1]);
		userid = Integer.valueOf(fields[offset + 2]);
		gender = Integer.valueOf(fields[offset + 3]);
		if (gender != 0) {
		    gender = (int)((gender - 1.5) * 2.0); // map gender from {1,2} to {-1, 1}
		}
		age = Integer.valueOf(fields[offset + 4]);
		tokens = StringUtil.mapArrayStrToInt(fields[offset+5].split(","));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (clicked >= 0) {
			builder.append(clicked + "|");
		}
		builder.append(depth + "|" + position + "|");
		builder.append(userid + "|" + gender + "|" + age + "|");
		builder.append(StringUtil.implode(tokens, ","));
		return builder.toString();
	}

    @Override
    public int getLabel() {
        return clicked;
    }
}
