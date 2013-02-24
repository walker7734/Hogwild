package preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import util.StringUtil;

/**
 * The program to join the training and testing data with the additional feature
 * file. Path to the training/testing data is hard coded.
 * 
 * @author haijieg
 * 
 */
/*
 * The original data schema (click) | (Impression) | DisplayURL | AdID |
 * AdvertiserID | Depth | Position | QueryID | KeywordID | TitleID |
 * DescriptionID | UserID 1. click: number of clicks. 2. Impression: number of
 * impressions. 3. DisplayURL: a property of the ad. The URL is shown together
 * with the title and description of an ad. It is usually the shortened landing
 * page URL of the ad, but not always. In the data file, this URL is hashed for
 * anonymity. 4. AdID: unique identifier of the ad. 5. AdvertiserID: a property
 * of the ad. Some advertisers consistently optimize their ads, so the title and
 * description of their ads are more attractive than those of others. 6.Depth: a
 * property of the session: Number of total ads impressed. 7. Position: a
 * property of an ad in a session: the rank. 8. QueryID: id of the query. This
 * id is a zero-based integer value. It is the key of the data file
 * 'queryid_tokensid.txt'. 9. KeywordID: a property of ads. This is the key of
 * 'purchasedkeyword_tokensid.txt'. 10. TitleID: a property of ads. This is the
 * key of 'titleid_tokensid.txt'. 11. DescriptionID: a property of ads. This is
 * the key of 'descriptionid_tokensid.txt'. 12. UserID This is the key of
 * 'userid_profile.txt'. When we cannot identify the user, this field has a
 * special value of 0.
 * 
 * The filtered data schema after join: click | impression | depth | position |
 * query tokens | keyword tokens | title tokens | description tokens | user
 * tokens
 */
public class JoinFeatureWithData {

	public static ArrayList<String> processLine(String line, boolean hasLabel) {
		StringTokenizer tokenizer = new StringTokenizer(line, "\t");
		ArrayList<String> filteredTokens = new ArrayList<String>();
		if (hasLabel) {
			filteredTokens.add(tokenizer.nextToken()); // push click
			filteredTokens.add(tokenizer.nextToken()); // push impression
		}
		tokenizer.nextToken(); // ignore displayURL
		tokenizer.nextToken(); // ignore AdID
		tokenizer.nextToken(); // ignore AdvertiserID
		filteredTokens.add(tokenizer.nextToken()); // push depth
		filteredTokens.add(tokenizer.nextToken()); // push position

		// replace query id with query tokens
		int queryid = Integer.valueOf(tokenizer.nextToken().trim());
		if (queryid < Features.queryFeature.size()) {
			filteredTokens.add(StringUtil.implode(
					Features.queryFeature.get(queryid).iterator(), ","));
		} else {
			filteredTokens.add("-1");
			System.err.println("Warning: unknown queryid=" + queryid);
			System.err.println("Line: " + line);
		}

		// replace keyword id with keyword tokens
		int keywordid = Integer.valueOf(tokenizer.nextToken().trim());
		if (keywordid < Features.keywordFeature.size()) {
			filteredTokens.add(StringUtil.implode(
					Features.keywordFeature.get(keywordid).iterator(), ","));
		} else {
			filteredTokens.add("-1");
			System.err.println("Warning: unknown keyword id=" + keywordid);
			System.err.println("Line: " + line);
		}

		// replace title id with title tokens
		int titleid = Integer.valueOf(tokenizer.nextToken().trim());
		if (titleid < Features.titleFeature.size()) {
			filteredTokens.add(StringUtil.implode(
					Features.titleFeature.get(titleid).iterator(), ","));
		} else {
			filteredTokens.add("-1");
			System.err.println("Warning: unknown title id=" + titleid);
			System.err.println("Line: " + line);
		}

		// replace description id with description tokens.
		int descriptionid = Integer.valueOf(tokenizer.nextToken().trim());
		if (descriptionid < Features.descriptionFeature.size()) {
			filteredTokens.add(StringUtil.implode(Features.descriptionFeature
					.get(descriptionid).iterator(), ","));
		} else {
			filteredTokens.add("-1");
			System.err.println("Warning: unknown descriptionid="
					+ descriptionid);
			System.err.println("Line: " + line);
		}

		// replace description id with description tokens
		int userid = Integer.valueOf(tokenizer.nextToken().trim());
		if (userid < Features.userFeature.size()) {
			filteredTokens
					.add(userid
							+ ","
							+ StringUtil.implode(
									Features.userFeature.get(userid), ","));
		} else {
			filteredTokens.add(userid + ",0,0");
			System.err.println("Warning: unknown userid=" + userid);
			System.err.println("Line: " + line);
		}
		return filteredTokens;
	}

	public static void main(String args[]) throws FileNotFoundException,
			IOException {
		Features.loadAllFeatures();
		String trainpath = "/usr1/haijieg/kdd/small/data/train.txt";
		@SuppressWarnings("resource")
        Scanner sc = new Scanner(new BufferedReader(new FileReader(trainpath)));
		FileWriter ftrainstream = new FileWriter(
				"/usr1/haijieg/kdd/small/datawithfeature/train.txt");
		BufferedWriter trainout = new BufferedWriter(ftrainstream);
		System.err.println("Processing input data from: " + trainpath);
		int count = 0;
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			ArrayList<String> filteredline = processLine(line, true);
			String output = StringUtil.implode(filteredline.iterator(), "|")
					+ "\n";
			trainout.write(output);
			count++;
			if (count % 100000 == 0)
				System.err.println("Processed " + count + " lines");
		}
		System.err.println("Done");
		trainout.close();

		String testpath = "/usr1/haijieg/kdd/small/data/test.txt";
		sc = new Scanner(new BufferedReader(new FileReader(testpath)));
		FileWriter fteststream = new FileWriter(
				"/usr1/haijieg/kdd/small/datawithfeature/test.txt");
		BufferedWriter testout = new BufferedWriter(fteststream);
		System.err.println("Processing input data from: " + testpath);
		count = 0;
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			ArrayList<String> filteredline = processLine(line, false);
			String output = StringUtil.implode(filteredline.iterator(), "|")
					+ "\n";
			testout.write(output);
			count++;
			if (count % 100000 == 0)
				System.err.println("Processed " + count + " lines");
		}
		System.err.println("Done");
		testout.close();
	}
}
