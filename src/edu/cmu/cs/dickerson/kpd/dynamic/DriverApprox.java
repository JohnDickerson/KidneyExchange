package edu.cmu.cs.dickerson.kpd.dynamic;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSGenerator;

public class DriverApprox {

	public static void main(String[] args) {

		// Generate draws from all UNOS match runs currently on the machine
		Random r = new Random();
		UNOSGenerator gen = UNOSGenerator.makeAndInitialize(IOUtil.getBaseUNOSFilePath(), ',', r);

		// list of |V|s we'll iterate over
		List<Integer> graphSizeList = Arrays.asList(new Integer[] {100});
		int numGraphReps = 16; 
		
		for(int graphSize : graphSizeList) {
			for(int graphRep=0; graphRep<numGraphReps; graphRep++) {

				IOUtil.dPrintln("Graph (|V|=" + graphSize + ", #" + graphRep + "/" + numGraphReps + ")");
				
				Pool pool = gen.generatePool(graphSize);
				
				
			} // graphRep : numGraphReps
		} // graphSize : graphSizeList
	}

}
