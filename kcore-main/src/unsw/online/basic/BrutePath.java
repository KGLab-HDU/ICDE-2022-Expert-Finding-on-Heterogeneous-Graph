package unsw.online.basic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import unsw.online.MetaPath;

/**
 * @author fangyixiang
 * @date 10 Sep. 2018
 * the brute-force method for answering a query
 */
public class BrutePath {
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	
	private int queryId = -1;//the query vertex id
	private MetaPath queryMPath = null;//the query meta-path
	private int queryK = -1;//the threshold k
	
	public BrutePath(int graph[][], int vertexType[], int edgeType[]) {
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
		Map<Integer, Set<Integer>> graphMap = buildGraph();
		
		//step 2: compute the connected k-core
		Set<Integer> community = findKCore(graphMap);
		return community;
	}

	private Map<Integer, Set<Integer>> buildGraph() {
		Map<Integer, Set<Integer>> graphMap = new HashMap<Integer, Set<Integer>>();
		Set<Integer> visitSet = new HashSet<Integer>();
		
		Queue<Integer> queue = new LinkedList<Integer>();//a queue
		queue.add(queryId);
		visitSet.add(queryId);
		while(queue.size() > 0) {
			int curId = queue.poll();
			Set<Integer> pnbSet = findBNeighbors(curId);//find all the pnbs of curId
			graphMap.put(curId, pnbSet);
			
			for(int pnb:pnbSet) {//expand from pnb
				if(!visitSet.contains(pnb)) {
					queue.add(pnb);
					visitSet.add(pnb);
				}
			}
		}
		
		return graphMap;
	}

	private Set<Integer> findBNeighbors(int curId) {
		Set<Integer> pnbSet = findNextSet(curId, 0);//find all the pneihgbors of curId
		pnbSet.remove(curId);//2018-9-18 we do not allow self-linked neighbors
		return pnbSet;
	}
	
	private Set<Integer> findNextSet(int id, int index) {
		Set<Integer> resultSet = new HashSet<Integer>();
		int targetVType = queryMPath.vertex[index + 1];//the followed vertex type in the meta-path
		int targetEType = queryMPath.edge[index];//the followed edge type in the meta-path
		
		int nb[] = graph[id];
		for(int i = 0;i < nb.length;i += 2) {
			int nbVertexID = nb[i], nbEdgeID = nb[i + 1];
			
			if(targetVType == vertexType[nbVertexID] && targetEType == edgeType[nbEdgeID]) {
				if(index == queryMPath.pathLen - 1) {
					resultSet.add(nbVertexID);//this vertex is a pnb of curId
				}else {
					Set<Integer> matchedVertexSet = findNextSet(nbVertexID, index + 1);
					resultSet.addAll(matchedVertexSet);
				}
			}
		}
		
		return resultSet;
	}

	private Set<Integer> findKCore(Map<Integer, Set<Integer>> graphMap) {
		Queue<Integer> queue = new LinkedList<Integer>();
		
		//step 1: find the vertices can be deleted in the first round
		Set<Integer> deleteSet = new HashSet<Integer>();
		for(Map.Entry<Integer, Set<Integer>> entry : graphMap.entrySet()) {
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
			Set<Integer> pnbSet = graphMap.get(curId);
			for(int pnb:pnbSet) {//update curId's pnb
				if(!deleteSet.contains(pnb)) {
					Set<Integer> tmpSet = graphMap.get(pnb);
					tmpSet.remove(curId);
					if(tmpSet.size() < queryK) {
						queue.add(pnb);
						deleteSet.add(pnb);
					}
				}
			}
			graphMap.remove(curId);//clean all the pnbs of curId
		}
		
		//step 3: find the connected component containing q
		if(graphMap.get(queryId).size() < queryK)   return null;
		Set<Integer> community = new HashSet<Integer>();//vertices which have been put into queue
		Queue<Integer> ccQueue = new LinkedList<Integer>();
		ccQueue.add(queryId);
		community.add(queryId);
		while(ccQueue.size() > 0) {
			int curId = ccQueue.poll();
			for(int pnb:graphMap.get(curId)) {//enumerate curId's neighbors
				if(!community.contains(pnb)) {
					ccQueue.add(pnb);
					community.add(pnb);
				}
			}
		}
		return community;
	}
}
