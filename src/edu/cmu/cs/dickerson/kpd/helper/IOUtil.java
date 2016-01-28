package edu.cmu.cs.dickerson.kpd.helper;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.dickerson.kpd.structure.Cycle;

public final class IOUtil {

	private IOUtil() {}

	public static final boolean DEBUG = true;
	public static void dPrint(Object o) { 
		if(DEBUG) { System.err.print(o.toString()); }
	}
	public static void dPrintln(Object o) { 
		if(DEBUG) { System.err.println(o.toString()); }
	}
	public static void dPrint(Object o1, Object o2) { 
		dPrint("[" + o1.toString() + "]: " + o2.toString());
	}
	public static void dPrintln(Object o1, Object o2) { 
		dPrintln("[" + o1.toString() + "]: " + o2.toString());
	}

	/**
	 * Closes a data stream, ignoring any IOExceptions thrown
	 * @param c some closeable stream (CSVWriter/Readers in this project)
	 * @return true if everything closed, false if IOException thrown
	 */
	public static boolean closeIgnoreExceptions(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/**
	 * (John Dickerson-specific)
	 * @return root directory path of UNOS files
	 */
	public static String getBaseUNOSFilePath() {
		if(new File("/Users/spook").exists()) {
			return "/Users/spook/amem/kpd/files_real_runs/zips";
		} else if(new File("/home/spook").exists()) {
			return "/home/spook/amem/kpd/files_real_runs/zips";	
		} else if(new File("/usr0/home/jpdicker").exists()) {
			return "/usr0/home/jpdicker/amem/kpd/files_real_runs/zips";	
		} else {
			System.err.println("Can't find path to UNOS files!");
			throw new RuntimeException();
		}
	}


	/**
	 * Makes an educated guess at converting a string to a boolean.
	 * @param s String that looks like a Boolean, like "Y" or "fAlse"
	 * @return true if we think s converts to true, false if we think it converts
	 * to false, and IllegalArgumentException if we can't figure it out
	 */
	public static boolean stringToBool(String raw) {

		String s = raw.trim().toUpperCase();
		if(s.equals("Y") || s.equals("YES") || s.equals("T") || s.equals("TRUE") || s.equals("1")) {
			return true;
		} else if(s.equals("N") || s.equals("NO") || s.equals("F") || s.equals("FALSE") || s.equals("0")) {
			return false;
		} else {
			throw new IllegalArgumentException("Could not convert " + s + " to a boolean value.");
		}
	}


	/**
	 * Takes a String[] and returns a dictionary of "string" -> integer index in array
	 * @param headers String[]
	 * @return 
	 */
	public static Map<String, Integer> stringArrToHeaders(String[] headers) {
		if(null == headers || headers.length < 1) {
			return new HashMap<String, Integer>();
		}

		Map<String, Integer> headerMap = new HashMap<String, Integer>();
		for(int idx=0; idx<headers.length; ++idx) {
			headerMap.put(headers[idx].toUpperCase().trim(), idx);
		}
		return headerMap;
	}

	public static Set<String> splitOnWhitespace(String s) {
		Set<String> split = new HashSet<String>();
		split.addAll(Arrays.asList( s.split("\\s+") ));
		return split;
	}

	/**
	 * Some of our experiments want to record data in terrible, cryptic files.  This
	 * writes a collection of toStringable()s to a file at filePath, one value per line
	 * @param filePath output filepath
	 * @param headers iterable collection of header values, printed before the vals
	 * @param indexMap <cycle index, LP relaxed value of that cycle decvar>
	 * @params cycles list of cycle objects to be index into (so we can grab, e.g., cycle weights)
	 */
	public static <T1, T2> void writeValuesToFile(String filePath, Collection<T1> headers, Map<Integer,Double> indexMap, List<Cycle> cycles) {
		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "utf-8"));
			try {
				for(T1 header : headers) { writer.write(header.toString()+",0.0\n"); }
				for(Map.Entry<Integer, Double> cycleEntry : indexMap.entrySet()) { 
					writer.write(cycleEntry.getValue().toString()+","+cycles.get(cycleEntry.getKey()).getWeight()+"\n"); 
				}
			} finally {
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
