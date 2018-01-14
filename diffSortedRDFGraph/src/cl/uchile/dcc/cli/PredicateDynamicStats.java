package cl.uchile.dcc.cli;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import cl.uchile.dcc.utils.MemStats;
import cl.uchile.dcc.utils.PredAndDyn;

class MutableTripla {
	int total = 0;
	int add = 0;
	int del = 0;

	MutableTripla(int t, int a, int d) {
		total = t;
		add = a;
		del = d;
	}

	public void increment() {
		++total;
	}

	public void incrementAdd() {
		++add;
		++total;
	}

	public void incrementDel() {
		++del;
		++total;
	}

	public int getTotal() {
		return total;
	}
}

public class PredicateDynamicStats {

	public static String TRIPLE_REGEX = "^(<[^>]+>)\\s+(<[^>]+>)\\s+(.*)\\s?.$";
	public static int TICKS = 10000000;

	public static void main(String[] args) throws IOException {
		Option inlO = new Option("l", "left input file");
		inlO.setArgs(1);
		inlO.setRequired(true);

		Option inrO = new Option("r", "right input file");
		inrO.setArgs(1);
		inrO.setRequired(true);

		Option ingzO = new Option("igz", "input file is GZipped");
		ingzO.setArgs(0);

		Option outO = new Option("o", "output file");
		outO.setArgs(1);
		outO.setRequired(true);

		Option outgzO = new Option("ogz", "output file should be GZipped");
		outgzO.setArgs(0);

		Option kO = new Option("k", "print first k lines to std out when finished");
		kO.setArgs(1);
		kO.setRequired(false);

		Option tO = new Option("t", "print first t lines to read in");
		tO.setArgs(1);
		tO.setRequired(false);

		Option helpO = new Option("h", "print help");

		Options options = new Options();
		options.addOption(inlO);
		options.addOption(inrO);
		options.addOption(ingzO);
		options.addOption(outO);
		options.addOption(outgzO);
		options.addOption(kO);
		options.addOption(tO);
		options.addOption(helpO);

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("***ERROR: " + e.getClass() + ": " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("parameters:", options);
			return;
		}

		// print help options and return
		if (cmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("parameters:", options);
			return;
		}

		// open the inputs
		String inl = cmd.getOptionValue(inlO.getOpt());
		String inr = cmd.getOptionValue(inrO.getOpt());
		boolean gzIn = cmd.hasOption(ingzO.getOpt());

		// open the output
		String out = cmd.getOptionValue(outO.getOpt());
		boolean gzOut = cmd.hasOption(outgzO.getOpt());

		// if we need to print top-k afterwards
		int k = Integer.MAX_VALUE;
		if (cmd.hasOption(kO.getOpt())) {
			k = Integer.parseInt(cmd.getOptionValue(kO.getOpt()));
		}

		// if we need to read top-t triplets
		int t = Integer.MAX_VALUE;;
		if (cmd.hasOption(tO.getOpt())) {
			t = Integer.parseInt(cmd.getOptionValue(tO.getOpt()));
		}

		// call the method that does the hard work
		// time it as well!
		long b4 = System.currentTimeMillis();
		diffGraph(inl, inr, gzIn, out, gzOut, k, t);
		

	}

	private static void diffGraph(String inl, String inr, boolean gzIn, String out, boolean gzOut, int k, int t)
			throws IOException {

		// open the input
		InputStream ils = new FileInputStream(inl);
		if (gzIn) {
			ils = new GZIPInputStream(ils);
		}
		BufferedReader inputl = new BufferedReader(new InputStreamReader(ils, "utf-8"));
		System.err.println("Reading from " + inl);

		InputStream irs = new FileInputStream(inr);
		if (gzIn) {
			irs = new GZIPInputStream(irs);
		}
		BufferedReader inputr = new BufferedReader(new InputStreamReader(irs, "utf-8"));
		System.err.println("Reading from " + inr);

		// open the output
		OutputStream os = new FileOutputStream(out);
		if (gzOut) {
			os = new GZIPOutputStream(os);
		}
		PrintWriter output = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(os), "utf-8"));
		System.err.println("Writing to " + out + "\n");

		String leftTriple = inputl.readLine();
		String rightTriple = inputr.readLine();
		int comp;
		Map<String, MutableTripla> preds = new HashMap<>();
		MutableTripla count;
		int ltripleCount = 0;
		int rtripleCount = 0;

		while (leftTriple != null || rightTriple != null) {
			if (leftTriple == null) {
				comp = 1;
			} else if (rightTriple == null) {
				comp = -1;
			} else {
				comp = leftTriple.compareTo(rightTriple);
			}

			Pattern pattern = Pattern.compile(TRIPLE_REGEX);
			if (comp < 0) {// triplet eliminado				
				Matcher lmatcher = pattern.matcher(leftTriple);
				String lpred = lmatcher.group(2);
				count = preds.get(lpred);
				if (count == null) {
					preds.put(lpred, new MutableTripla(1, 0, 1));
				} else {
					count.incrementDel();
				}
				output.println("-\t" + leftTriple);
				leftTriple = inputl.readLine();
				++ltripleCount;

			} else if (comp > 0) {// triplet agregado
				Matcher rmatcher = pattern.matcher(rightTriple);
				String rpred = rmatcher.group(2);				
				count = preds.get(rpred);
				if (count == null) {
					preds.put(rpred, new MutableTripla(1, 1, 0));
				} else {
					count.incrementAdd();
				}
				output.println("+\t" + rightTriple);
				rightTriple = inputr.readLine();
				++rtripleCount;
				
			} else {// iguales
				Matcher lmatcher = pattern.matcher(leftTriple);
				String lpred = lmatcher.group(2);
				count = preds.get(lpred);
				if (count == null) {
					preds.put(lpred, new MutableTripla(2, 0, 0));
				} else {
					count.increment();
					count.increment();
				}
				leftTriple = inputl.readLine();
				rightTriple = inputr.readLine();
				++ltripleCount;
				++rtripleCount;
			}
/*
			if ((ltripleCount + rtripleCount) % TICKS == 0) {
				System.err.println("Read" + (ltripleCount + rtripleCount) + " triples");
				System.err.println(MemStats.getMemStats() + "\n");
			}

			if ((ltripleCount + rtripleCount) >= t) {
				break;
			}
*/
		}

		System.out.println("Total de triplets in left:\t" + ltripleCount);
		System.out.println("Total de triplets in right:\t" + rtripleCount);

		TreeSet<PredAndDyn> sortBuffer = new TreeSet<>(Collections.reverseOrder());
		for (String pred : preds.keySet()) {
			MutableTripla d = preds.get(pred);
			PredAndDyn predyn = new PredAndDyn(pred, ((double) d.add) / d.total + ((double) d.del) / d.total);
			sortBuffer.add(predyn);
		}

		int i = k;
		System.out.println("\nNo.\tPredicate\t+\t-");
		while (!sortBuffer.isEmpty()) {
			PredAndDyn next = sortBuffer.pollFirst();
			String pred = next.getPred();
			if (k-- <= 0) {
				break;
			}
			System.out.println(i - k + "\t" + pred + "\t" + preds.get(pred).total + "\t" + preds.get(pred).add + "\t"
					+ preds.get(pred).del);
		}

		inputl.close();
		inputr.close();
		output.close();
	}

}
