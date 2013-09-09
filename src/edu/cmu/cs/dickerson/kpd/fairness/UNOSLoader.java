package edu.cmu.cs.dickerson.kpd.fairness;

import java.io.FileReader;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

public class UNOSLoader {

	private char delim = ',';
	
	public enum DonorIdx {
		PAIR_ID(0), CANDIDATE_ID(1), DONOR_ID(2), NDD(5), ABO(6);
		private int index;
		private DonorIdx(int index) { this.index = index; }
		public int idx() { return index; }
	}
	
	public enum RecipientIdx {
		PAIR_ID(0), CANDIDATE_ID(1), ABO(5), HIGHLY_SENSITIZED(28);
		private int index;
		private RecipientIdx(int index) { this.index = index; }
		public int idx() { return index; }
	}
	
	public enum EdgeWeightIdx {
		CANDIDATE_ID(1), CANDIDATE_PAIR_ID(2), DONOR_ID(3), DONOR_PAIR_ID(4), EDGEWEIGHT(5);
		private int index;
		private EdgeWeightIdx(int index) { this.index = index; }
		public int idx() { return index; }
	}
	
	public UNOSLoader(char delim) {
		this.delim = delim;
	}
	
	public Pool loadFromFile(String donorFilePath, String recipientFilePath, String edgeFilePath) {
		
		Pool pool = new Pool(Edge.class);
		
		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(recipientFilePath), delim);
			
			String[] line;
			while((line = reader.readNext()) != null) {
				Integer ID = Integer.valueOf(line[RecipientIdx.CANDIDATE_ID.idx()]);
				BloodType bloodType = BloodType.getBloodType(line[RecipientIdx.ABO.idx()]);
				Boolean isHighlySensitized = IOUtil.stringToBool(line[RecipientIdx.HIGHLY_SENSITIZED.idx()]);
				
				double patientCPRA = isHighlySensitized ? 1.0 : 0.0;
				VertexPair vp = new VertexPair(ID, bloodType, bloodType, false, patientCPRA, false);
			}
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		} finally { 
			IOUtil.closeIgnoreExceptions(reader);
		}
		
		// TODO load recipient and donor data
		
		
		return pool;
	}
}
