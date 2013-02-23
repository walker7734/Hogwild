package util;

public class HashUtil {

	public static int hashToRange(String s, int upper) {
		int hashval = s.hashCode() % upper;
		if (hashval < 0)
			hashval = upper + hashval;
		return hashval;
	}

	public static int hashToSign(String s) {
		if (s.hashCode() % 2 == 0)
			return -1;
		else
			return 1;
	}
}
