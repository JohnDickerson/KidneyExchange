package edu.cmu.cs.dickerson.kpd.structure.real;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.VertexAltruist;
import edu.cmu.cs.dickerson.kpd.structure.VertexPair;
import edu.cmu.cs.dickerson.kpd.structure.real.exception.LoaderException;
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

	private void loadRecipients(String recipientFilePath, Pool pool, Map<Integer, Set<Integer>> candToDonors, Map<Integer, Vertex> idToVertex) {
		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(recipientFilePath), delim);
			reader.readNext();  // skip headers
			
			String[] line;
			while((line = reader.readNext()) != null) {
				Integer ID = Integer.valueOf(line[RecipientIdx.CANDIDATE_ID.idx()]);
				BloodType bloodType = BloodType.getBloodType(line[RecipientIdx.ABO.idx()]);
				Boolean isHighlySensitized = IOUtil.stringToBool(line[RecipientIdx.HIGHLY_SENSITIZED.idx()]);

				double patientCPRA = isHighlySensitized ? 1.0 : 0.0;
				VertexPair vp = new VertexPair(ID, bloodType, bloodType, false, patientCPRA, false);
				pool.addPair(vp);

				candToDonors.put(ID, new HashSet<Integer>());
				idToVertex.put(ID, vp);
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally { 
			IOUtil.closeIgnoreExceptions(reader);
		}
	}

	@SuppressWarnings("resource")
	private Set<Integer> loadDonors(String donorFilePath, Pool pool, Map<Integer, Set<Integer>> candToDonors, Map<Integer, Vertex> idToVertex) throws LoaderException {

		CSVReader reader = null;
		Set<Integer> altruistIDs = new HashSet<Integer>();
		try {
			reader = new CSVReader(new FileReader(donorFilePath), delim);
			reader.readNext(); // skip headers
			
			String[] line;
			while((line = reader.readNext()) != null) {
				Integer candidateID = Integer.valueOf(line[DonorIdx.CANDIDATE_ID.idx()]);
				Integer donorID = Integer.valueOf(line[DonorIdx.DONOR_ID.idx()]);
				BloodType donorBloodType = BloodType.getBloodType(line[DonorIdx.ABO.idx()]);
				Boolean isNonDirectedDonor = IOUtil.stringToBool(line[DonorIdx.NDD.idx()]);

				if(isNonDirectedDonor) {
					// If the donor is an altruist, add to the graph (this is our first time seeing him/her)
					VertexAltruist altruist = new VertexAltruist(donorID, donorBloodType);
					pool.addAltruist(altruist);
					altruistIDs.add(donorID);
					idToVertex.put(donorID, altruist);
				} else if(candToDonors.containsKey(candidateID)) {
					// If the donor is paired, add to the respective candidate's donor list
					candToDonors.get(candidateID).add(donorID);
				} else {
					throw new LoaderException("Found donor paired with an unknown candidate " + candidateID);
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		} finally { 
			IOUtil.closeIgnoreExceptions(reader);
		}
		return altruistIDs;
	}

	@SuppressWarnings("resource")
	private void loadEdges(String edgeFilePath, Pool pool, Map<Integer, Set<Integer>> candToDonors, Map<Integer, Vertex> idToVertex, Set<Integer> altruistIDs) throws LoaderException {
		
		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(edgeFilePath), delim);
			reader.readNext();  // skip headers
			
			String[] line;
			while((line = reader.readNext()) != null) {
	
				Integer candidateID = Integer.valueOf(line[EdgeWeightIdx.CANDIDATE_ID.idx()]);
				Integer donorID = Integer.valueOf(line[EdgeWeightIdx.DONOR_ID.idx()]);
				double edgeWeight = Double.valueOf(line[EdgeWeightIdx.EDGEWEIGHT.idx()].trim());
				
				Vertex from = idToVertex.get(donorID);
				Vertex to = idToVertex.get(candidateID);
				if(null == from || null == to) {
					throw new LoaderException("Loaded an edge between one or both of a nonexistant donor " + donorID + " and candidate " + candidateID);
				}
				pool.setEdgeWeight(pool.addEdge(from, to), edgeWeight);
			}
			
		} catch(IOException e) {
			e.printStackTrace();
		} finally { 
			IOUtil.closeIgnoreExceptions(reader);
		}
	}
	
	public Pool loadFromFile(String donorFilePath, String recipientFilePath, String edgeFilePath) throws LoaderException {

		IOUtil.dPrintln("Loading UNOS graph (donor file: " + donorFilePath + ")");
		Pool pool = new Pool(Edge.class);

		// Read in the recipients, make vertex pairs for each of them (note: no altruists until donor file read)
		Map<Integer, Set<Integer>> candToDonors = new HashMap<Integer, Set<Integer>>();
		Map<Integer, Vertex> idToVertex = new HashMap<Integer, Vertex>();
		loadRecipients(recipientFilePath, pool, candToDonors, idToVertex);

		// Load donors (either paired with patients, or altruists)
		Set<Integer> altruistIDs = loadDonors(donorFilePath, pool, candToDonors, idToVertex);

		// Load the edges and weights, draw them in the Pool
		loadEdges(edgeFilePath, pool, candToDonors, idToVertex, altruistIDs);

		IOUtil.dPrintln("Loaded UNOS graph with " + pool.vertexSet().size() + " vertices and "+ pool.edgeSet().size() + " edges.");
		return pool;
	}
}
