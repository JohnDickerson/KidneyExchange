package edu.cmu.cs.dickerson.kpd.helper;

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
}
