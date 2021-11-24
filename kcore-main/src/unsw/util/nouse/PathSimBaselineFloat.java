package unsw.util.nouse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import unsw.Config;
import unsw.DataReader;
import unsw.online.MetaPath;
import unsw.online.basic.FastBCore;
import unsw.util.Log;
import unsw.util.LogFinal;


public class PathSimBaselineFloat {
	private int graph[][] = null;//data graph, including vertice IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	private List<Query> querysList = null;

	public Log logCoreInfo = null;

	public PathSimBaselineFloat(int graph[][], int vertexType[], int edgeType[], List<Query> querysList, String logFileName) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
		this.querysList = querysList;
		logCoreInfo = new Log(logFileName);
	}

	public Map<Integer, Map<Integer, Float>> query(int queryId, MetaPath queryMPath, int queryK) {
		FastBCore fbCore = new FastBCore(graph, vertexType, edgeType);
		Set<Integer> bcoreSet = fbCore.query(queryId, queryMPath, queryK);
		System.out.println();
		System.out.println( "queryId, queryK is " + queryId + ", " + queryK);
		System.out.println("bcore has size of " + bcoreSet.size() + ".");
		if (bcoreSet.size() > 100000) {
			return null;
		}

		AnalyzePathSim psimAna = new AnalyzePathSim(graph, vertexType, edgeType);
		Map<Integer, Map<Integer, Integer>> pGraph = psimAna.batchBuildForPathSim(bcoreSet, queryMPath); // the pGrpah keeps the path count for each p-pairs
		Map<Integer, Map<Integer, Float>> wGraph = new HashMap<Integer, Map<Integer, Float>>();
		// initialize the wGraph where wGraph keeps the pathsim for each p-pairs.
		Set<Float> psimSet = new HashSet<Float>();
		for (int vid : pGraph.keySet()) wGraph.put(vid, new HashMap<Integer, Float>());
		for (int vid : pGraph.keySet()) {
			Map<Integer, Integer> nbMap = pGraph.get(vid);
			for (int nbVid : nbMap.keySet()) {
				if (nbVid <= vid) continue;
				float curPSimV = (float) (pGraph.get(vid).get(nbVid) + pGraph.get(nbVid).get(vid)) / (float) (pGraph.get(vid).get(vid) + pGraph.get(nbVid).get(nbVid));
				wGraph.get(vid).put(nbVid, curPSimV);
				wGraph.get(nbVid).put(vid, curPSimV);
				psimSet.add(curPSimV);
			}
		}
		ArrayList<Float> psimList = new ArrayList<Float>();
		for (float psim : psimSet) {
			psimList.add(psim);
		}
		Collections.sort(psimList);
		int low  = 0, high = psimList.size() - 1;
		int mid = (low + high) / 2;
		while (low < high) {
			mid = (low + high) / 2;
			float curPSim = psimList.get(mid);
			Map<Integer, Map<Integer, Float>> psimCore = obtainSimCore(queryId, queryMPath, queryK, bcoreSet, curPSim, wGraph);
			if ( !psimCore.keySet().contains(queryId) ) {
				high = mid - 1;
			} else {
				low = mid + 1;
			}
		}
		// the first case: low = x, mid = (x+1), high = (x+2) -> low = x, mid = (x+1), high = x; the optimum index is x or (x-1);
		// the second case: low = x, mid = (x+1), high = (x+2) -> low = (x+2) , mid = (x+1), high = (x+2); the optimum index is (x+1) or (x+2);
		// the third case: low = x, mid = x, high = (x+1) -> low = x, mid = x, high = x-1; the optimum index is (x-1)
		// the fourth case: low = x, mid = x, high = (x+1) -> low = (x+1), mid = x, high = x+1; the optimum index is x or (x+1)
		// the total first case: low == (mid-1), high == (mid-1); test low and (low-1)
		// the total second case： low == (mid+1), high == (mid+1); test low and (low-1)
		// the total third case: low == mid, high == (mid+1); test (low-1)
		// the total fourth case: low == (mid+1), high == (mid+1); test low or (low-1)
		// generally speaking please test low or (low-1)

		float curPSim = psimList.get(low);
		Map<Integer, Map<Integer, Float>> psimCore = obtainSimCore(queryId, queryMPath, queryK, bcoreSet, curPSim, wGraph);
		if (!psimCore.keySet().contains(queryId)) {
			curPSim = psimList.get(low - 1);
			psimCore = obtainSimCore(queryId, queryMPath, queryK, bcoreSet, curPSim, wGraph);
		}
		System.out.println("The psimBCore has size of " + psimCore.size() + ".");
		return psimCore;
	}

	public Map<Integer, Map<Integer, Float>> obtainSimCore(int queryId, MetaPath queryMPath, int queryK, Set<Integer> keepSet, float psimValue, Map<Integer, Map<Integer, Float>> pgMap) {
		Map<Integer, Set<Integer>> cddGraph = new HashMap<Integer, Set<Integer>>(); // "cdd" means the abbreviation of candidate
		Queue<Integer> deletingQ = new LinkedList<Integer>(); // for recursively deleting those vertices whose degree are less than (k-2)
		for (int vid : pgMap.keySet()) {
			Map<Integer, Float> wnbMap = pgMap.get(vid);
			Set<Integer> newNbSet = new HashSet<Integer>();
			for (int wnbVid : wnbMap.keySet()) {
				if (wnbMap.get(wnbVid) < psimValue) continue;
				newNbSet.add(wnbVid);
			}
			cddGraph.put(vid, newNbSet);
			if (cddGraph.get(vid).size() < queryK) deletingQ.add(vid);
		}
		while (!deletingQ.isEmpty()) {
			int curVid = deletingQ.poll();
			Set<Integer> nbSet = cddGraph.get(curVid);
			for (int nbVid : nbSet) {
				cddGraph.get(nbVid).remove(curVid);
				if (cddGraph.get(nbVid).size() < queryK) {
					if (!deletingQ.contains(nbVid)) deletingQ.add(nbVid);
				}
			}
			cddGraph.remove(curVid);
		}

		if (!cddGraph.containsKey(queryId)) return new HashMap<Integer, Map<Integer, Float>>();
		// last step : find the connected component containing the vertex with the queryId.
		Queue<Integer> bfsQ = new LinkedList<Integer>();
		Set<Integer> resultSet = new HashSet<Integer>();
		bfsQ.add(queryId);
		while (!bfsQ.isEmpty()) {
			int curVid = bfsQ.poll();
			resultSet.add(curVid);
			for (int nbOfCurVid : cddGraph.get(curVid)) {
				if (!resultSet.contains(nbOfCurVid)) {
					if (!bfsQ.contains(nbOfCurVid)) {
						bfsQ.add(nbOfCurVid);
					}
				}
			}
		}

		Map<Integer, Map<Integer, Float>> resultMap = new HashMap<Integer, Map<Integer, Float>>();
		for (int vid : resultSet) {
			Map<Integer, Float> wnbMap =  pgMap.get(vid);
			Map<Integer, Float> newNbMap = new HashMap<Integer, Float>();
			for (int nbVid : wnbMap.keySet()) {
				if (resultSet.contains(nbVid)) {
					newNbMap.put(nbVid, wnbMap.get(nbVid));
				}
			}
			resultMap.put(vid, newNbMap);
		}

		return resultMap;
	}

	public Map<Integer, Map<Integer, Integer>> queryPCountBi(int queryId, MetaPath queryMPath, int queryK) {
		FastBCore fbCore = new FastBCore(graph, vertexType, edgeType);
		Set<Integer> bcoreSet = fbCore.query(queryId, queryMPath, queryK);
		System.out.println();
		System.out.println( "queryId, queryK is " + queryId + ", " + queryK);
		System.out.println("bcore has size of " + bcoreSet.size() + ".");
		if (bcoreSet.size() > 150000) {
			return null;
		}

		AnalyzePathSim psimAna = new AnalyzePathSim(graph, vertexType, edgeType);
		Map<Integer, Map<Integer, Integer>> pGraph = psimAna.batchBuildForPathSim(bcoreSet, queryMPath); // the pGrpah keeps the path count for each p-pairs
		Map<Integer, Map<Integer, Float>> wGraph = new HashMap<Integer, Map<Integer, Float>>();
		System.out.println("The pathsim is weighted graph built" );;
		// initialize the wGraph where wGraph keeps the pathsim for each p-pairs.
		Set<Float> psimSet = new HashSet<Float>();
		for (int vid : pGraph.keySet()) wGraph.put(vid, new HashMap<Integer, Float>());
		for (int vid : pGraph.keySet()) {
			Map<Integer, Integer> nbMap = pGraph.get(vid);
			for (int nbVid : nbMap.keySet()) {
				if (nbVid <= vid) continue;
				float curPSimV = (float) (pGraph.get(vid).get(nbVid) + pGraph.get(nbVid).get(vid)) / (float) (pGraph.get(vid).get(vid) + pGraph.get(nbVid).get(nbVid));
				wGraph.get(vid).put(nbVid, curPSimV);
				wGraph.get(nbVid).put(vid, curPSimV);
				psimSet.add(curPSimV);
			}
		}
		ArrayList<Float> psimList = new ArrayList<Float>();
		for (float psim : psimSet) {
			psimList.add(psim);
		}
		Collections.sort(psimList);
		/*int low  = 0, high = psimList.size() - 1;
		while (low < high) {
			float low_sim = psimList.get(low);
			float high_sim = psimList.get(high);
			float mid_sim = (low_sim + high_sim) / 2;
			int mid = -1 - Collections.binarySearch(psimList, mid_sim);
			float cur_sim = psimList.get(mid);
			Map<Integer, Map<Integer, Float>> psimCore = obtainSimCore(queryId, queryMPath, queryK, bcoreSet, cur_sim, wGraph);
			if (!psimCore.keySet().contains(queryId)) {
				high = mid - 1;
			} else {
				low = mid + 1;
			}
		}*/

		int low  = 0, high = psimList.size() - 1;
		int mid = (low + high) / 2;
		while (low < high) {
			mid = (low + high) / 2;
			float curPSim = psimList.get(mid);
			Map<Integer, Map<Integer, Float>> psimCore = obtainSimCore(queryId, queryMPath, queryK, bcoreSet, curPSim, wGraph);
			if ( !psimCore.keySet().contains(queryId) ) {
				high = mid - 1;
			} else {
				low = mid + 1;
			}
		}

		// the first case: low = x, mid = (x+1), high = (x+2) -> low = x, mid = (x+1), high = x; the optimum index is x or (x-1);
		// the second case: low = x, mid = (x+1), high = (x+2) -> low = (x+2) , mid = (x+1), high = (x+2); the optimum index is (x+1) or (x+2);
		// the third case: low = x, mid = x, high = (x+1) -> low = x, mid = x, high = x-1; the optimum index is (x-1)
		// the fourth case: low = x, mid = x, high = (x+1) -> low = (x+1), mid = x, high = x+1; the optimum index is x or (x+1)
		// the total first case: low == (mid-1), high == (mid-1); test low and (low-1)
		// the total second case： low == (mid+1), high == (mid+1); test low and (low-1)
		// the total third case: low == mid, high == (mid+1); test (low-1)
		// the total fourth case: low == (mid+1), high == (mid+1); test low or (low-1)
		// generally speaking please test low or (low-1)

		float curPSim = psimList.get(low);
		Map<Integer, Map<Integer, Float>> psimCore = obtainSimCore(queryId, queryMPath, queryK, bcoreSet, curPSim, wGraph);
		if (!psimCore.keySet().contains(queryId)) {
			curPSim = psimList.get(low - 1);
			psimCore = obtainSimCore(queryId, queryMPath, queryK, bcoreSet, curPSim, wGraph);
		}
		System.out.println("The psimBCore has size of " + psimCore.size() + ".");

		Map<Integer, Map<Integer, Integer>> psimCoreCount = new HashMap<Integer, Map<Integer, Integer>>();
		for (int vid : psimCore.keySet()) {
			Map<Integer, Integer> pCountNbr = new HashMap<Integer, Integer>();
			for (int nbVid : pGraph.get(vid).keySet()) {
				if (psimCore.containsKey(nbVid)) {
					pCountNbr.put(nbVid, pGraph.get(vid).get(nbVid));
				}
			}
			psimCoreCount.put(vid, pCountNbr);
		}

		//record it
		String queryInfo = "";
		queryInfo += String.valueOf(queryId);
		queryInfo += " , " + queryMPath.toString();
		logCoreInfo.log(queryInfo);
		logCoreInfo.log("Threshold is , " + String.valueOf(curPSim));
		for (int vid : psimCoreCount.keySet()) {
			logCoreInfo.log(String.valueOf(vid));
		}

		//
		float density = (float) 0.0;
		float totalDensity = (float) 0.0;
		float pathsim = (float) 0.0;
		float totalPathsim = (float) 0.0;
		float diameter = (float) 0.0;
		Map<Integer, Set<Integer>> gMap = new HashMap<Integer, Set<Integer> >();
		Set<Integer> keepSet = new HashSet<Integer>();
		for (int vid : psimCoreCount.keySet()) {
			keepSet.add(vid);
			Set<Integer> nbSet = new HashSet<Integer>();
			for (int nbVid : psimCoreCount.get(vid).keySet() ) {
				nbSet.add(nbVid);
			}
			gMap.put(vid, nbSet);
		}
		Diameter dComputer = new Diameter(gMap, keepSet);
		diameter = (float) dComputer.computeDiameter();
		for (int vid : keepSet) {
			Map<Integer, Float> nbMap = wGraph.get(vid);
			for (int nbVid : nbMap.keySet()) {
				totalDensity += pGraph.get(vid).get(nbVid);
				totalPathsim += wGraph.get(vid).get(nbVid);
				if (!keepSet.contains(nbVid)) continue;
				density += pGraph.get(vid).get(nbVid);
				pathsim += wGraph.get(vid).get(nbVid);
			}
		}
		float pairNum = keepSet.size() * (keepSet.size() - 1) / 2;
		float keepSetSize = keepSet.size();
		density /= keepSetSize;
		totalDensity /= keepSetSize;
		pathsim /= pairNum;
		totalPathsim /= pairNum;
		String simInfo = "#, Density : " + String.valueOf(density) + ", PathSim: " + String.valueOf(pathsim) + ", Diameter: " + String.valueOf(diameter);
		logCoreInfo.log(simInfo);
		String totalSimInfo = "*, TotalDensity : " + String.valueOf(totalDensity) + ", TotalPathSim: " + String.valueOf(totalPathsim);
		logCoreInfo.log(totalSimInfo);
		return psimCoreCount;
	}

 	public void processAvgPSim(String dataSetName) {
		// step 2: perform query
		List<float[]> sList = new ArrayList<float[]>();
		System.out.println("There are " + querysList.size() + " querys.");

		for (int i = 0; i < querysList.size(); i++) {
			Query query = querysList.get(i);
			System.out.println(i + " th query started, the quertId is " + query.id);
			Map<Integer, Map<Integer, Float>> psimCore = query(query.id, query.metapath, Config.k);
			if (psimCore == null) {
				continue;
			}

			float sim[] = new float[3];
			// compute path-sim values;
			float pathSimV = (float) 0.0;
			float denseV = (float) 0.0;
			for (int vid : psimCore.keySet()) {
				Map<Integer, Float> nbMap = psimCore.get(vid);
				for (int nbVid : nbMap.keySet()) {
					pathSimV += nbMap.get(nbVid);
					denseV = denseV + (float)0.0;
				}
			}
			pathSimV = pathSimV / 2;
			denseV = denseV / 2;
			int pairNum = psimCore.size() * (psimCore.size() - 1) / 2;
			pathSimV = pathSimV / pairNum;
			denseV = denseV / psimCore.size();

			Map<Integer, Set<Integer>> gMap = new HashMap<Integer, Set<Integer> >();
			Set<Integer> psimSet = new HashSet<Integer>();
			for (int vid : psimCore.keySet()) {
				psimSet.add(vid);
				Set<Integer> nbSet = new HashSet<Integer>();
				for (int nbVid : psimCore.get(vid).keySet() ) {
					nbSet.add(nbVid);
				}
				gMap.put(vid, nbSet);
			}
			Diameter dComputer = new Diameter(gMap, psimSet);
			float diameterV = dComputer.computeDiameter();
			sim[0] = pathSimV;
			sim[1] = denseV;
			sim[2] = diameterV;
			sList.add(sim);
		}

		// step 3: compute statistics
		double avgPathSim = 0, avgDense = 0, avgDiameter = 0;
		for (float sim[]:sList) {
			avgPathSim += sim[0];
			avgDense += sim[1];
			avgDiameter += sim[2];
		}
		avgPathSim /= sList.size();
		avgDense /= sList.size();
		avgDiameter /= sList.size();
		LogFinal.log(dataSetName + " AvgPathSim: " + LogFinal.format(avgPathSim) + "\t" + ", AvgDense: " + LogFinal.format(avgDense) + "\t" + ", AvgDiameter: "
				+ LogFinal.format(avgDiameter));
		LogFinal.log("\n");
	}

 	public void processAvgDense(String dataSetName) {
		// step 2: perform query
		List<Float> sList = new ArrayList<Float>();
		System.out.println("There are " + querysList.size() + " querys.");

		for (int i = 0; i < querysList.size(); i++) {
			Query query = querysList.get(i);
			System.out.println(i + " th query started, the quertId is " + query.id);
			Map<Integer, Map<Integer, Integer>> psimCoreCount = queryPCountBi(query.id, query.metapath, Config.k);
			if (psimCoreCount == null) {
				continue;
			}

			float denseV = (float) 0.0;
			for (int vid : psimCoreCount.keySet()) {
				Map<Integer, Integer> nbMap = psimCoreCount.get(vid);
				for (int nbVid : nbMap.keySet()) {
					denseV = denseV + (float) psimCoreCount.get(vid).get(nbVid);
				}
			}
			denseV = denseV / 2;
			denseV = denseV / psimCoreCount.size();
			sList.add(denseV);
		}

		// step 3: compute statistics
		double avgDense = 0;
		for (float sim : sList) {
			avgDense += sim;
		}
		avgDense /= sList.size();
		LogFinal.log(dataSetName + "AvgDense: " + LogFinal.format(avgDense));
		LogFinal.log("\n");
 	}

 	public void staticAvg(String dataSetName) {
		for (int i = 0; i < querysList.size(); i++) {
			Query query = querysList.get(i);
			System.out.println(i + " th query started, the quertId is " + query.id);
			queryPCountBi(query.id, query.metapath, Config.k);
		}
 	}

	public static void main(String[] args) {
		List<String> dataSetList = new ArrayList<String>();
		dataSetList.add("newDBLPPlus");
		dataSetList.add("IMDB750");
		dataSetList.add("FBMusic");
		Config.machineName = "Phoenix19";
		for (String dataSetName : dataSetList) {
			String graphDataSetPath = Config.root + "/" + dataSetName;
			String graphPath = graphDataSetPath + "/graph.txt";
			String vertexPath = graphDataSetPath + "/vertex.txt";
			String edgePath = graphDataSetPath + "/edge.txt";
			String queryPath = graphDataSetPath + "/querys.txt";
			DataReader dataReader = new DataReader(graphPath, vertexPath, edgePath);
			int[][] graph = dataReader.readGraph();
			int[] vertexType = dataReader.readVertexType();
			int[] edgeType = dataReader.readEdgeType();
			QueryReader reader = new QueryReader(queryPath);
			List<Query> querysList = reader.readQuerys();
			String pCoreRecordFile = "";
			PathSimBaselineFloat psb = new PathSimBaselineFloat(graph, vertexType, edgeType, querysList, pCoreRecordFile);
			psb.staticAvg(dataSetName);
		}


	}

}
