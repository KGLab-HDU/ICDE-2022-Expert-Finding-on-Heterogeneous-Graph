package unsw.util;

import java.util.*;

import unsw.online.MetaPath;

/**
 * @author fangyixiang
 * @date 10 Sep. 2018
 * the pruned-path method for answering a query
 */
public class FindKNB {
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	
	private int queryId = -1;//the query vertex id
	private MetaPath queryMPath = null;//the query meta-path
	private int queryK = -1;//the threshold k
	
	public FindKNB(int graph[][], int vertexType[], int edgeType[]) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
	}
	
	public void query(int queryId, MetaPath queryMPath, int queryK) {
		this.queryId = queryId;
		this.queryMPath = queryMPath;
		this.queryK = queryK;
		
		//step 1: build the connected homogeneous graph
		long t1 = System.nanoTime();
		buildGraph();
		System.out.println((System.nanoTime() - t1) + "\n");
	}

	private Map<Integer, Set<Integer>> buildGraph() {
		long t1 = System.nanoTime();
		Map<Integer, Set<Integer>> pnbMap = new HashMap<Integer, Set<Integer>>();//a vertex -> its pnbs
		Map<Integer, List<Set<Integer>>> visitMap = new HashMap<Integer, List<Set<Integer>>>();//a vertex -> its visited vertices
		for(int curId = 0;curId < graph.length;curId ++) {
			if(vertexType[curId] == queryMPath.vertex[0]) {
				pnbMap.put(curId, new HashSet<Integer>());
				List<Set<Integer>> visitList = new ArrayList<Set<Integer>>();
				for(int i = 0;i <= queryMPath.pathLen;i ++)   visitList.add(new HashSet<Integer>());
				visitMap.put(curId, visitList);
			}
		}
		System.out.println(System.nanoTime() - t1);
		
		for(int curId = 0;curId < graph.length;curId ++) {
			if(vertexType[curId] == queryMPath.vertex[0]) {
				List<Set<Integer>> visitList = new ArrayList<Set<Integer>>();
				for(int i = 0;i <=queryMPath.pathLen;i ++)   visitList.add(new HashSet<Integer>());
				Set<Integer> pnbSet = new HashSet<Integer>();
				findFirstKNeighbors(curId, curId, 0, visitList, pnbSet);//find all the pnbs of curId
				pnbMap.put(curId, pnbSet);
			}
		}
		
		return pnbMap;
	}
	
	private void findFirstKNeighbors(int startID, int curId, int index, List<Set<Integer>> visitList, Set<Integer> pnbSet) {
		int targetVType = queryMPath.vertex[index + 1], targetEType = queryMPath.edge[index];
		
		int nbArr[] = graph[curId];
		for(int i = 0;i < nbArr.length;i += 2) {
			int nbVertexID = nbArr[i], nbEdgeID = nbArr[i + 1];
			Set<Integer> visitSet = visitList.get(index + 1);
			if(!visitSet.contains(nbVertexID) && targetVType == vertexType[nbVertexID] && targetEType == edgeType[nbEdgeID]) {
				if(index + 1 < queryMPath.pathLen) {
					findFirstKNeighbors(startID, nbVertexID, index + 1, visitList, pnbSet);
					if(pnbSet.size() >= queryK)   return ;//we have found k meta-paths
					visitSet.add(nbVertexID);//mark this vertex (and its branches) as visited
				}else {//a meta-path has been found
					if(nbVertexID != startID)   pnbSet.add(nbVertexID);
					visitSet.add(nbVertexID);//mark this vertex (and its branches) as visited
					if(pnbSet.size() >= queryK)   return ;//we have found k meta-paths
				}
			}
		}
	}
}
