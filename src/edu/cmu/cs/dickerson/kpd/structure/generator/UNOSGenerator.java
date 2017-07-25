package edu.cmu.cs.dickerson.kpd.structure.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;
import edu.cmu.cs.dickerson.kpd.helper.IOUtil;
import edu.cmu.cs.dickerson.kpd.structure.Edge;
import edu.cmu.cs.dickerson.kpd.structure.Pool;
import edu.cmu.cs.dickerson.kpd.structure.Vertex;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSDonor;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSPair;
import edu.cmu.cs.dickerson.kpd.structure.real.UNOSRecipient;
import edu.cmu.cs.dickerson.kpd.structure.real.sampler.ExactSplitUNOSSampler;
import edu.cmu.cs.dickerson.kpd.structure.real.sampler.InOrderUNOSSampler;
import edu.cmu.cs.dickerson.kpd.structure.real.sampler.RealSplitUNOSSampler;
import edu.cmu.cs.dickerson.kpd.structure.real.sampler.UNOSSampler;
import edu.cmu.cs.dickerson.kpd.structure.types.BloodType;

// TODO    Invariance assumptions for generator:
// Recipient's health profile and preferences do not change over time
// Recipient's KPD_candidate_ID, donor's KPD_donor_ID do not change

// TODO    Additions for the future generator:
// Include max chain/cycle sizes on a per-vertex basis 

public class UNOSGenerator extends PoolGenerator {

	// All donor-recipient pairs we've ever seen in reality
	private List<UNOSPair> pairs;
	// Map of generated vertices in the Pool to their real-world donor-recipient counterparts
	private Map<Vertex, UNOSPair> vertexMap;
	// Map KPD_donor_id -> UNOSDonor
	private Map<String, UNOSDonor> donors;
	// Map KPD_candidate_id -> UNOSRecipient
	private Map<String, UNOSRecipient> recipients;
	// Current unused vertex ID for optimization graphs
	private int currentVertexID;
	// How many overlaps allowed between donor and patient before no edge is drawn?
	private int threshold;
	
	protected UNOSGenerator(Map<String, UNOSDonor> donors, Map<String, UNOSRecipient> recipients, List<UNOSPair> pairs, Random randGen, int threshold) {
		super(randGen);
		this.donors = donors;
		this.recipients = recipients;
		this.pairs = pairs;
		this.vertexMap = new HashMap<Vertex, UNOSPair>();
		this.currentVertexID = 0;
		this.threshold = threshold;
	}

	/**
	 * Given N UNOSPairs (either patient-donor pairs or altruists), creates an
	 * NxN adjacency matrix where cell i,j represents whether UNOSPair i could
	 * give to UNOSPair j, according to our compatibility function.
	 * 
	 * Writes this matrix and an additional information matrix (one row per 
	 * vertex, tells whether this vertex is alt/pair, blood type, etc) to two 
	 * CSV files with baseFileName as head of file
	 */
	public void writeSamplingFiles(String baseFileName) {
		
		// Generate a pool containing exactly one of every pair or altruist
		// loaded into the generator from real data
		this.currentVertexID = 0;  // have to reset so this doesn't keep incrementing as we make independent pools
		this.vertexMap = new HashMap<Vertex, UNOSPair>();
		Pool pool = new Pool(Edge.class);
		addVerticesToPool(pool, pairs.size(), new InOrderUNOSSampler(pairs));
		int N = pool.vertexSet().size();
		assert(N == pairs.size());
		IOUtil.dPrintln("Generated pool with " + N + " unique UNOS altruists and pairs.");
		
		// Write the dense adjacency matrix to a CSV file
		pool.writeUNOSGraphToDenseAdjacencyMatrix(baseFileName+"_adj.csv");
		
		// Create an N x F matrix recording other features of each vertex,
		// and write to a CSV file as well
		String[] detailsArray = new String[N];
		for(Vertex v : pool.vertexSet()) {
			int idx = v.getID();
			assert(idx>=0 && idx<N);
			UNOSPair pair = v.getUnderlyingPair();
			
			String donorABO = "-";
			if(v.getUnderlyingPair().getDonors().size() > 0) {
				donorABO = pair.getDonors().iterator().next().abo.toString();
			}
			detailsArray[idx] = idx + "," + 
					(pair.isAltruist() ? "1," : "0,") + 
					(pair.getRecipient() == null ? "-" : pair.getRecipient().abo) + "," + 
					donorABO + ",";
		}
		
		try {
			PrintWriter writer = new PrintWriter(baseFileName+"_details.csv", "UTF-8");
			for(int v_i=0; v_i<N; v_i++) {
				writer.println(detailsArray[v_i]);
			}

			writer.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		return;
		
	}
	
	public void printStatistics() {
		
		Map<BloodType, Double> donorABO = new HashMap<BloodType, Double>();
		Map<BloodType, Double> candABO = new HashMap<BloodType, Double>();
		for(BloodType bt : BloodType.values()) {
			donorABO.put(bt, 0.0);
			candABO.put(bt, 0.0);
		}
		// Add up number of donors, candidates with each ABO type
		for(UNOSDonor d : this.donors.values()) {
			donorABO.put(d.abo, donorABO.get(d.abo)+1.0);
		}
		for(UNOSRecipient r : this.recipients.values()) {
			candABO.put(r.abo, candABO.get(r.abo)+1.0);
		}
		// Translate raw counts to percentages
		for(BloodType bt : BloodType.values()) {
			donorABO.put(bt, donorABO.get(bt)/this.donors.size());
			candABO.put(bt, candABO.get(bt)/this.recipients.size());
			System.out.println(bt.toString() + "\t\tDonor: " + donorABO.get(bt) + "\t\tRecipient: " + candABO.get(bt));
		}
		return;
	}
	
	
	@Override
	public Pool generate(int numPairs, int numAltruists) {
		this.currentVertexID = 0;  // have to reset so this doesn't keep incrementing as we make independent pools
		this.vertexMap = new HashMap<Vertex, UNOSPair>();
		Pool pool = new Pool(Edge.class);
		this.addVerticesToPool(pool, numPairs, numAltruists);
		return pool;	
	}

	
	public Pool generatePool(int size) {
		this.currentVertexID = 0;  // have to reset so this doesn't keep incrementing as we make independent pools
		this.vertexMap = new HashMap<Vertex, UNOSPair>();
		Pool pool = new Pool(Edge.class);
		this.addVerticesToPool(pool, size);
		return pool;	
	}

	/**
	 * Adds N new vertices to the pool, of which exactly numPairs are pairs and numAltruists are
	 * altruists, sampled from the base UNOS set of pairs and altruists
	 */
	@Override
	public Set<Vertex> addVerticesToPool(Pool pool, int numPairs, int numAltruists) {
		// We sample from data, so we don't control #pairs or #altruists
		return addVerticesToPool(pool, numPairs+numAltruists, 
				new ExactSplitUNOSSampler(this.pairs,this.random,numPairs,numAltruists));
	}
	
	/**
	 * Adds N new vertices to the pool, sampling from the base UNOS distribution of alts and pairs
	 */
	public Set<Vertex> addVerticesToPool(Pool pool, int numNewVerts) {
		return addVerticesToPool(pool, numNewVerts, 
				new RealSplitUNOSSampler(this.pairs,this.random));
	}
	
	
	public Set<Vertex> addVerticesToPool(Pool pool, int numNewVerts, UNOSSampler sampler) {
		Set<Vertex> newVerts = new HashSet<Vertex>();
		for(int idx=0; idx<numNewVerts; idx++) {

			// Sample a pair from the real data, make it a new pool Vertex
			UNOSPair samplePair = sampler.takeSample();

			// Spawn a new unique Vertex linked back to the underlying UNOSPair
			Vertex sampleVert = samplePair.toBaseVertex(this.currentVertexID++);
			pool.addVertex(sampleVert);
			newVerts.add(sampleVert);
			
			// Check di-edge compatibility between this new vertex and ALL vertices in the current pool
			for(Vertex v : pool.getPairs()) {
				if(v.equals(sampleVert)) { continue; }
				
				// Only draw cardinality 1 edges from this vertex to compatible non-altruists
				if(UNOSPair.canDrawDirectedEdge(samplePair, v.getUnderlyingPair(), this.threshold)) {
					Edge e = pool.addEdge(sampleVert, v);
					pool.setEdgeWeight(e, 1.0);
				}
				if(UNOSPair.canDrawDirectedEdge(v.getUnderlyingPair(), samplePair, this.threshold)) {
					Edge e = pool.addEdge(v, sampleVert);
					if(samplePair.isAltruist()) {
						pool.setEdgeWeight(e, 0.0);
					} else {
						pool.setEdgeWeight(e, 1.0);
					}
				}
			}
			for(Vertex alt : pool.getAltruists()) {
				if(alt.equals(sampleVert)) { continue; }
				
				// Always draw a (dummy) edge from this vertex to altruists, UNLESS this is an altruist
				if(UNOSPair.canDrawDirectedEdge(samplePair, alt.getUnderlyingPair())) {
					Edge e = pool.addEdge(sampleVert, alt);
					pool.setEdgeWeight(e, 0.0);
				}
				// Only draw cardinality 1 edge from altruist to compatible pair vertices
				if(UNOSPair.canDrawDirectedEdge(alt.getUnderlyingPair(), samplePair)) {
					Edge e = pool.addEdge(alt, sampleVert);
					pool.setEdgeWeight(e, 1.0);
				}
			}

			// Keep track of who maps to whom, optimization -> real data
			vertexMap.put(sampleVert, samplePair);
		}
		return newVerts;
	}

	public static UNOSGenerator makeAndInitialize(String baseUNOSpath, char delim) {
		return UNOSGenerator.makeAndInitialize(baseUNOSpath, delim, new Random(), 0);
	}

	public static UNOSGenerator makeAndInitialize(String baseUNOSpath, char delim, Random randGen) {
		return UNOSGenerator.makeAndInitialize(baseUNOSpath, delim, randGen, 0);
	}

	public static UNOSGenerator makeAndInitialize(String baseUNOSpath, char delim, Random randGen, int threshold) {

		Map<String, UNOSDonor> donors = new HashMap<String, UNOSDonor>();
		Map<String, UNOSRecipient> recipients = new HashMap<String, UNOSRecipient>();

		// We assume a directory structure of:
		// baseUNOSpath/
		// ->  KPD_CSV_IO_MMDDYY/
		//     |-- YYYYMMDD_donor_xxx.csv    # donor file
		//     |-- YYYYMMDD_recipient_xxx.csv  # recipient file
		//     |-- # possibly some other files
		File baseUNOSDir = new File(baseUNOSpath);
		List<File> matchDirList = Arrays.asList(baseUNOSDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return file.isDirectory() && !name.toLowerCase().endsWith(".zip");
			}
		}));

		int matchRunsLoaded = 0;
		for(File matchDir : matchDirList) {

			// We assume a lot about filenames here.  Figure out which .csv files matter
			String matchRunID = "", donorFilePath = "", recipientFilePath = "";
			File[] csvFiles = matchDir.listFiles(new FilenameFilter() {  @Override public boolean accept(File file, String name) { return name.endsWith(".csv"); } });
			if(null == csvFiles || csvFiles.length < 1) { continue; }

			// Get the donor and recipient .csv filenames and also the match run ID (= date of match run)
			for(File csvFile : Arrays.asList(csvFiles)) {
				if(csvFile.getName().toUpperCase().contains("DONOR")) {
					donorFilePath = csvFile.getAbsolutePath();
					matchRunID = csvFile.getName().substring(0,8);
				} else if(csvFile.getName().toUpperCase().contains("RECIPIENT")) {
					recipientFilePath = csvFile.getAbsolutePath();
				}
			}

			// Make sure we're actually looking at a UNOS match run
			// Error out SUPER HARD for now, soften this when we're less error-prone
			if(donorFilePath.isEmpty() || recipientFilePath.isEmpty() || matchRunID.isEmpty()) {
				IOUtil.dPrintln("Couldn't figure out this directory!");
				System.exit(-1);
			}

			CSVReader reader = null;

			IOUtil.dPrintln("Loading " + recipientFilePath);
			// Load in the recipients (reload headers array for each file, in case it changes)
			Set<UNOSRecipient> singleRunRecipients = new HashSet<UNOSRecipient>();
			try {
				reader = new CSVReader(new FileReader(recipientFilePath), delim);

				// Reload headers array for each file, in case it changes
				Map<String, Integer> headers = IOUtil.stringArrToHeaders(reader.readNext());
				
				String[] line;
				while((line = reader.readNext()) != null) {
					singleRunRecipients.add( UNOSRecipient.makeUNOSRecipient(line, headers) );
				}
				
			} catch(IOException e) {
				e.printStackTrace();
			} finally { 
				IOUtil.closeIgnoreExceptions(reader);
			}
			
			
			IOUtil.dPrintln("Loading " + donorFilePath);
			// Load in the donors 
			Set<UNOSDonor> singleRunDonors = new HashSet<UNOSDonor>();
			try {
				reader = new CSVReader(new FileReader(donorFilePath), delim);

				// Reload headers array for each file, in case it changes
				Map<String, Integer> headers = IOUtil.stringArrToHeaders(reader.readNext());
				
				String[] line;
				while((line = reader.readNext()) != null) {
					singleRunDonors.add( UNOSDonor.makeUNOSDonor(line, headers) );
				}

			} catch(IOException e) {
				e.printStackTrace();
			} finally { 
				IOUtil.closeIgnoreExceptions(reader);
			}

			
			
			// Only record new recipients
			for(UNOSRecipient r : singleRunRecipients) {
				if(!recipients.containsKey(r.kpdCandidateID)) {
					recipients.put(r.kpdCandidateID, r);
				}
			}
			// Record only new donors OR old donors who have switched recipients
			for(UNOSDonor d : singleRunDonors) {
				if(!donors.containsKey(d.kpdDonorID)) {
					donors.put(d.kpdDonorID, d);
				} else if( !d.nonDirectedDonor && null!=donors.get(d.kpdDonorID).kpdCandidateID && !donors.get(d.kpdDonorID).kpdCandidateID.equals(d.kpdCandidateID) ) {
					// TODO eventually track donors who left then returned
				}
			}
			
			matchRunsLoaded++;
			
		}  // end of reading all files loop
		IOUtil.dPrintln("Loaded data from " + matchRunsLoaded + " UNOS match runs.");

		
		// Make pairs out of the recipients and donors:
		// O(n^2)ish right now, but who cares because real data is small
		Set<UNOSPair> pairSet = new HashSet<UNOSPair>();
		for(String recipientID : recipients.keySet()) {
			UNOSRecipient r = recipients.get(recipientID);

			Set<UNOSDonor> pairDonors = new HashSet<UNOSDonor>();
			for(String donorID : donors.keySet()) {
				UNOSDonor d = donors.get(donorID);
				if(!d.nonDirectedDonor && d.kpdCandidateID.equals(r.kpdCandidateID)) {
					pairDonors.add(d);
				}
			}

			if(pairDonors.size() < 1) { 
				IOUtil.dPrintln("Could not find a donor match for recipient: " + recipientID);
				//System.exit(-1);
			} else {
				//IOUtil.dPrintln("Found " + pairDonors.size() + " donors for recipient " + recipientID);
			}

			pairSet.add( UNOSPair.makeUNOSPair(pairDonors, r));
		}
		// Make pairs out of the altruistic (unpaired) donors
		for(String donorID : donors.keySet()) {
			UNOSDonor d = donors.get(donorID);
			if(d.nonDirectedDonor) {
				pairSet.add(UNOSPair.makeUNOSAltruist(d));
			}
		}

		IOUtil.dPrintln("Loaded " + pairSet.size() + " UNOS pairs.");
		return new UNOSGenerator(donors, recipients, new ArrayList<UNOSPair>(pairSet), randGen, threshold);
	}

	public Map<Vertex, UNOSPair> getVertexMap() {
		return vertexMap;
	}

	public Map<String, UNOSDonor> getDonors() {
		return donors;
	}

	public Map<String, UNOSRecipient> getRecipients() {
		return recipients;
	}

	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}
	
	
	
	/** 
	 * Load all the files in, generate a graph with one of each vertex ever
	 * seen, and output an adjacency matrix
	 */
	public static void main(String[] args) {
		String basePath = IOUtil.getBaseUNOSFilePath();
		UNOSGenerator gen = UNOSGenerator.makeAndInitialize(basePath, ',');
		
		gen.writeSamplingFiles("unos_data");
	}

	
}
