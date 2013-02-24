

import java.util.HashMap;
import java.util.Map;

import util.HashUtil;

public class HashedDataInstance {
	// Label
	int clicked; // 0 or 1

	// Feature of the page and ad
	int depth; // depth of the session.
	int position; // position of the ad.

	// Feature of the user
	int userid;
	int gender; // user gender indicator -1 for male, 1 for female
	int age;		// user age indicator '1' for (0, 12], '2' for (12, 18], '3' for
							// (18, 24], '4' for (24, 30],
							// 	'5' for (30, 40], and '6' for greater than 40.
	
	int featuredim;
	Map<Integer, Integer> hashedTextFeature; // map hashed feature key to its value;

	public HashedDataInstance(String line, boolean hasLabel, int dim,
			boolean personal) {
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
		hashedTextFeature = new HashMap<Integer, Integer>(); 
		
		String[] tokens = fields[offset+5].split(",");

		/**
		 * Fill in your code here to create a hashedTextFeature.
		 */
		
		
		for (int i = 0; i < tokens.length; i++) {
            updateFeature(tokens[i], dim);
        }   
		
		if (personal) {
			/**
			 * Extra credit Fill in your code here to for create a hashedFeature
			 * with personalization.
			 */

		    for (int i = 0; i < tokens.length; i++) {
		        updateFeature(tokens[i] + ":user" + userid, dim);
		    }
		    
		} 
	}

	/**
	 * Helper function. Updates the feature hashmap with a given key and value.
	 * You can use HashUtil.hashToRange as h, and HashUtil.hashToSign as \xi. 
	 * @param key
	 * @param val
	 */
	private void updateFeature(String key, int val) {
		// Fill in your code here
	    int index = HashUtil.hashToRange(key, val);
        int value = 0;
        if (hashedTextFeature.containsKey(index)) {
            value = hashedTextFeature.get(index);
        }
        value += HashUtil.hashToSign(key);
        hashedTextFeature.put(index, value);
	}
}
