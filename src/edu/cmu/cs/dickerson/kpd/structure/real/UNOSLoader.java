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
		PAIR_ID(0), CANDIDATE_ID(1), AGE(4), ABO(5), HIGHLY_SENSITIZED(28);
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

	private int loadRecipients(String recipientFilePath, Pool pool, Map<Integer, Vertex> idToVertex, Map<String, Integer> strIDtoIntID) {
		CSVReader reader = null;
		int ID = 0;
		try {
			reader = new CSVReader(new FileReader(recipientFilePath), delim);
			reader.readNext();  // skip headers

			String[] line;
			while((line = reader.readNext()) != null) {
				String candidateID = line[RecipientIdx.CANDIDATE_ID.idx()].trim().toUpperCase();
				//Integer age = Integer.valueOf(line[RecipientIdx.AGE.idx()]);
				BloodType bloodType = BloodType.getBloodType(line[RecipientIdx.ABO.idx()]);
				Boolean isHighlySensitized = IOUtil.stringToBool(line[RecipientIdx.HIGHLY_SENSITIZED.idx()]);

				//double patientCPRA = (isHighlySensitized || age <= 18) ? 1.0 : 0.0;  // for KDD-2014 sub
				double patientCPRA = isHighlySensitized ? 1.0 : 0.0;
				VertexPair vp = new VertexPair(ID, bloodType, bloodType, false, patientCPRA, false);
				pool.addPair(vp);

				idToVertex.put(ID, vp);
				strIDtoIntID.put(candidateID, ID);
				ID++;
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally { 
			IOUtil.closeIgnoreExceptions(reader);
		}
		return ID;
	}

	private Set<Integer> loadDonors(String donorFilePath, Pool pool, Map<String, String> donorToCand, Map<Integer, Vertex> idToVertex, Map<String, Integer> strIDtoIntID, int ID) throws LoaderException {

		CSVReader reader = null;
		Set<Integer> altruistIDs = new HashSet<Integer>();
		try {
			reader = new CSVReader(new FileReader(donorFilePath), delim);
			reader.readNext(); // skip headers

			String[] line;
			while((line = reader.readNext()) != null) {
				Boolean isNonDirectedDonor = IOUtil.stringToBool(line[DonorIdx.NDD.idx()]);


				String donorID = line[DonorIdx.DONOR_ID.idx()].trim().toUpperCase();
				BloodType donorBloodType = BloodType.getBloodType(line[DonorIdx.ABO.idx()]);

				if(isNonDirectedDonor) {
					// If the donor is an altruist, add to the graph (this is our first time seeing him/her)
					VertexAltruist altruist = new VertexAltruist(ID, donorBloodType);
					pool.addAltruist(altruist);
					altruistIDs.add(ID);
					idToVertex.put(ID, altruist);
					strIDtoIntID.put(donorID, ID);
					ID++;
				} else {
					// If the donor is paired, add to the respective candidate's donor list
					String candidateID = line[DonorIdx.CANDIDATE_ID.idx()].trim();
					donorToCand.put(donorID, candidateID);
					// Update vertex blood type for the donor
					VertexPair vp = (VertexPair) idToVertex.get( strIDtoIntID.get(donorToCand.get(donorID) ));
					if(null==vp) {
						// Weird case: Match run on 2014-08-04 lists a donor as both a pair (line 182) and an NDD (line 258),
						// and the pair version's paired patient ID doesn't exist -- so skip it here
						IOUtil.dPrintln("Could not find vertex pair for donor ID: " + donorID + "; skipping.");
					} else {
						vp.setBloodTypeDonor(donorBloodType);
					}
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

	@SuppressWarnings({ "unused" })
	private void loadEdges(String edgeFilePath, Pool pool, Map<String, String> donorToCand, Map<Integer, Vertex> idToVertex, Map<String, Integer> strIDtoIntID, Set<Integer> altruistIDs) throws LoaderException {
		loadEdges(edgeFilePath, pool, donorToCand, idToVertex, strIDtoIntID, altruistIDs, false);
	}

	@SuppressWarnings("resource")
	private void loadEdges(String edgeFilePath, Pool pool, Map<String, String> donorToCand, Map<Integer, Vertex> idToVertex, Map<String, Integer> strIDtoIntID, Set<Integer> altruistIDs, boolean forceUnitWeights) throws LoaderException {


		// Read non-dummy edges from UNOS file
		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(edgeFilePath), delim);
			reader.readNext();  // skip headers

			String[] line;
			while((line = reader.readNext()) != null) {

				String candidateIDStr = line[EdgeWeightIdx.CANDIDATE_ID.idx()].trim();
				if(candidateIDStr.isEmpty()) {
					// Ignore weird zero-weight non-dummy altruist edges
					continue;
				}
				String donorID = line[EdgeWeightIdx.DONOR_ID.idx()];
				double edgeWeight = Double.valueOf(line[EdgeWeightIdx.EDGEWEIGHT.idx()].trim());

				Vertex from = null;
				Integer potentialAltID = strIDtoIntID.get(donorID);
				if(null != potentialAltID && altruistIDs.contains(potentialAltID)) {
					from = idToVertex.get(potentialAltID);
				} else {
					from = idToVertex.get( strIDtoIntID.get(donorToCand.get(donorID) ));
				}

				if(null == from) {
					throw new LoaderException("Trying to load an edge for nonexistent donor: " + donorID);
				}

				Vertex to = idToVertex.get(strIDtoIntID.get(candidateIDStr));
				if(null == to) {
					throw new LoaderException("Trying to load an edge for nonexistent candidate: " + candidateIDStr);
				}

				Edge e = pool.addEdge(from, to);
				if(null == e) { // edge existed in the graph already
					// For whatever reason, in March 2015 UNOS started listing two edges for dual-donor pairs
					double previousEdgeWeight = pool.getEdgeWeight(pool.getEdge(from, to));
					double newEdgeWeight = edgeWeight;
					if(forceUnitWeights) { newEdgeWeight = newEdgeWeight==0.0 ? 0.0 : 1.0; }
					if(previousEdgeWeight != newEdgeWeight) {
						System.out.println("Dual donor pair edge weight mismatch!");
						for(int idx=0; idx<line.length; idx++) { System.out.print(line[idx] + " "); } System.out.println();
					}
				} else {
					if(forceUnitWeights) {
						pool.setEdgeWeight(e, edgeWeight==0.0 ? 0.0 : 1.0);
					} else {
						pool.setEdgeWeight(e, edgeWeight);
					}
				}
			}

		} catch(IOException e) {
			e.printStackTrace();
		} finally { 
			IOUtil.closeIgnoreExceptions(reader);
		}

		// Add dummy edges from every candidate-donor pair to every altruist
		for(VertexAltruist altruist : pool.getAltruists()) {
			for(VertexPair pair : pool.getPairs()) {
				pool.setEdgeWeight(pool.addEdge(pair, altruist), 0.0);
			}
		}
	}

	public Pool loadFromFile(String donorFilePath, String recipientFilePath, String edgeFilePath) throws LoaderException {
		return loadFromFile(donorFilePath, recipientFilePath, edgeFilePath, false);
	}

	public Pool loadFromFile(String donorFilePath, String recipientFilePath, String edgeFilePath, boolean forceUnitWeights) throws LoaderException {

		IOUtil.dPrintln("Loading UNOS graph (donor file: " + donorFilePath + ")");
		Pool pool = new Pool(Edge.class);

		// Read in the recipients, make vertex pairs for each of them (note: no altruists until donor file read)
		Map<Integer, Vertex> idToVertex = new HashMap<Integer, Vertex>();
		Map<String, Integer> strIDtoIntID = new HashMap<String, Integer>();
		int nextID = loadRecipients(recipientFilePath, pool, idToVertex, strIDtoIntID);

		// Load donors (either paired with patients, or altruists)
		Map<String, String> donorToCand = new HashMap<String, String>();
		Set<Integer> altruistIDs = loadDonors(donorFilePath, pool, donorToCand, idToVertex, strIDtoIntID, nextID);

		// Load the edges and weights, draw them in the Pool
		loadEdges(edgeFilePath, pool, donorToCand, idToVertex, strIDtoIntID, altruistIDs, forceUnitWeights);

		IOUtil.dPrintln("Loaded UNOS graph with " + pool.vertexSet().size() + " vertices and "+ pool.edgeSet().size() + " edges.");
		return pool;
	}
}
