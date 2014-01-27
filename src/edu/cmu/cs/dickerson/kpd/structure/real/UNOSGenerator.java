package edu.cmu.cs.dickerson.kpd.structure.real;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
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

// TODO    Invariance assumptions for generator:
// Recipient's health profile and preferences do not change over time
// Recipient's KPD_candidate_ID, donor's KPD_donor_ID do not change

// TODO    Additions for the future generator:
// Include max chain/cycle sizes on a per-vertex basis 

public class UNOSGenerator {

	// All donor-recipient pairs we've ever seen in reality
	private List<UNOSPair> pairs;
	// Map of generated vertices in the Pool to their real-world donor-recipient counterparts
	private Map<Vertex, UNOSPair> vertexMap;
	// Single random object, seeded externally for repetition
	private Random randGen;
	// Map KPD_donor_id -> UNOSDonor
	private Map<Integer, UNOSDonor> donors;
	// Map KPD_candidate_id -> UNOSRecipient
	private Map<Integer, UNOSRecipient> recipients;
	// Current unused vertex ID for optimization graphs
	private int currentVertexID;

	protected UNOSGenerator(Map<Integer, UNOSDonor> donors, Map<Integer, UNOSRecipient> recipients, List<UNOSPair> pairs, Random randGen) {
		this.donors = donors;
		this.recipients = recipients;
		this.pairs = pairs;
		this.randGen = randGen;
		this.vertexMap = new HashMap<Vertex, UNOSPair>();
		this.currentVertexID = 0;
	}

	public Pool generatePool(int size) {
		Pool pool = new Pool(Edge.class);
		this.addVertices(pool, size);
		return pool;
	}

	public void addVertices(Pool pool, int numNewVerts) {
		for(int idx=0; idx<numNewVerts; idx++) {

			// Sample a pair from the real data, make it a new pool Vertex
			UNOSPair samplePair = pairs.get(this.randGen.nextInt() % pairs.size());

			// Spawn a new unique Vertex linked back to the underlying UNOSPair
			Vertex sampleVert = samplePair.toBaseVertex(this.currentVertexID++);

			// Check di-edge compatibility between this new vertex and ALL vertices in the current pool
			for(Vertex v : pool.getPairs()) {
				// Only draw cardinality 1 edges from this vertex to compatible non-altruists
				if(UNOSPair.canDrawDirectedEdge(samplePair, v.getUnderlyingPair())) {
					Edge e = pool.addEdge(sampleVert, v);
					pool.setEdgeWeight(e, 1.0);
				}
				if(UNOSPair.canDrawDirectedEdge(v.getUnderlyingPair(), samplePair)) {
					Edge e = pool.addEdge(v, sampleVert);
					pool.setEdgeWeight(e, 1.0);
				}
			}
			for(Vertex alt : pool.getAltruists()) {
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
	}

	public static UNOSGenerator initialize(String baseUNOSpath, char delim) {
		return UNOSGenerator.initialize(baseUNOSpath, delim, new Random());
	}

	public static UNOSGenerator initialize(String baseUNOSpath, char delim, Random randGen) {

		Map<Integer, UNOSDonor> donors = new HashMap<Integer, UNOSDonor>();
		Map<Integer, UNOSRecipient> recipients = new HashMap<Integer, UNOSRecipient>();

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

			// Load in the recipients (reload headers array for each file, in case it changes)
			Set<UNOSRecipient> singleRunRecipients = new HashSet<UNOSRecipient>();
			try {
				reader = new CSVReader(new FileReader(donorFilePath), delim);

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

			
			
			// Only record new recipients and donors
			for(UNOSRecipient r : singleRunRecipients) {
				if(!recipients.containsKey(r.getKPDCandidateID())) {
					recipients.put(r.getKPDCandidateID(), r);
				}
			}
			for(UNOSDonor d : singleRunDonors) {
				if(!donors.containsKey(d.getKPDDonorID())) {
					donors.put(d.getKPDDonorID(), d);
				}
			}
			
		}  // end of reading all files loop


		// Make pairs out of the recipients and donors:
		// O(n^2)ish right now, but who cares because real data is small
		Set<UNOSPair> pairSet = new HashSet<UNOSPair>();
		for(Integer recipientID : recipients.keySet()) {
			UNOSRecipient r = recipients.get(recipientID);

			Set<UNOSDonor> pairDonors = new HashSet<UNOSDonor>();
			for(Integer donorID : donors.keySet()) {
				UNOSDonor d = donors.get(donorID);
				if(d.getKPDCandidateID().equals(r.getKPDCandidateID())) {
					pairDonors.add(d);
				}
			}

			if(pairDonors.size() < 1) { 
				IOUtil.dPrintln("Could not find a donor match for recipient: " + recipientID);
				System.exit(-1);
			}

			pairSet.add( UNOSPair.makeUNOSPair(pairDonors, r));
		}
		// Make pairs out of the altruistic (unpaired) donors
		for(Integer donorID : donors.keySet()) {
			UNOSDonor d = donors.get(donorID);
			if(d.isNonDirectedDonor()) {
				pairSet.add(UNOSPair.makeUNOSAltruist(d));
			}
		}

		IOUtil.dPrintln("Loaded " + pairSet.size() + " UNOS pairs.");
		return new UNOSGenerator(donors, recipients, new ArrayList<UNOSPair>(pairSet), randGen);
	}

	public Map<Vertex, UNOSPair> getVertexMap() {
		return vertexMap;
	}

	public Map<Integer, UNOSDonor> getDonors() {
		return donors;
	}

	public Map<Integer, UNOSRecipient> getRecipients() {
		return recipients;
	}

}
