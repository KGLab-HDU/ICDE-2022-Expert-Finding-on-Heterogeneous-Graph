package unsw.online.basic;

import java.util.*;

import unsw.online.BatchSearch;
import unsw.online.MetaPath;

/**
 * @author fangyixiang
 * @date 10 Sep. 2018
 * the pruned-path method for answering a query
 */
public class PrunePath {
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	
	private int queryId = -1;//the query vertex id
	private MetaPath queryMPath = null;//the query meta-path
	private int queryK = -1;//the threshold k
	
	public PrunePath(int graph[][], int vertexType[], int edgeType[]) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
	}
	
	public Set<Integer> query(int queryId, MetaPath queryMPath, int queryK) {
		this.queryId = queryId;
		this.queryMPath = queryMPath;
		this.queryK = queryK;
		
		//step 0: check whether queryId's type matches with the meta-path
		if(queryMPath.vertex[0] != vertexType[queryId])   return null;

		//step 1: build the connected homogeneous graph
		Map<Integer, Set<Integer>> pnbMap = buildGraph();
		
		//step 2: compute the connected k-core
		Set<Integer> community = findKCore(pnbMap);
		return community;
	}

	private Map<Integer, Set<Integer>> buildGraph() {
		//step 1: find all the vertices
		Set<Integer> keepSet = new HashSet<Integer>();
		for(int curId = 0;curId < graph.length;curId ++) {
			if(vertexType[curId] == queryMPath.vertex[0]) {
				keepSet.add(curId);
			}
		}
		
		//step 2: build the graph
		Map<Integer, Set<Integer>> pnbMap = new HashMap<Integer, Set<Integer>>();
		BatchSearch batchSearch = new BatchSearch(graph, vertexType, edgeType, queryMPath);
		for(int curId = 0;curId < graph.length;curId ++) {
			if(vertexType[curId] == queryMPath.vertex[0]) {
				Set<Integer> pnbSet = batchSearch.collect(curId, keepSet);
				pnbMap.put(curId, pnbSet);
			}
		}
		
		return pnbMap;
	}

	private Set<Integer> findKCore(Map<Integer, Set<Integer>> pnbMap) {
		Queue<Integer> queue = new LinkedList<Integer>();//simulate a queue
		
		//step 1: find the vertices can be deleted in the first round
		Set<Integer> deleteSet = new HashSet<Integer>();
		for(Map.Entry<Integer, Set<Integer>> entry : pnbMap.entrySet()) {
			int curId = entry.getKey();
			Set<Integer> pnbSet = entry.getValue();
			if(pnbSet.size() < queryK) {
				queue.add(curId);
				deleteSet.add(curId);
			}
		}
		
		//step 2: delete vertices whose degrees are less than k
		while(queue.size() > 0) {
			int curId = queue.poll();//delete curId
			Set<Integer> pnbSet = pnbMap.get(curId);
			for(int pnb:pnbSet) {//update curId's pnb
				if(!deleteSet.contains(pnb)) {
					Set<Integer> tmpSet = pnbMap.get(pnb);
					tmpSet.remove(curId);
					if(tmpSet.size() < queryK) {
						queue.add(pnb);
						deleteSet.add(pnb);
					}
				}
			}
			pnbMap.put(curId, new HashSet<Integer>());//clean all the pnbs of curId
		}
		
		//step 3: find the connected component containing q
		if(pnbMap.get(queryId).size() < queryK)   return null;
		Set<Integer> community = new HashSet<Integer>();//vertices which have been put into queue
		Queue<Integer> ccQueue = new LinkedList<Integer>();
		ccQueue.add(queryId);
		community.add(queryId);
		while(ccQueue.size() > 0) {
			int curId = ccQueue.poll();
			for(int pnb:pnbMap.get(curId)) {//enumerate curId's neighbors
				if(!community.contains(pnb)) {
					ccQueue.add(pnb);
					community.add(pnb);
				}
			}
		}
		return community;
	}
}
