package unsw.util;

import java.util.*;

import unsw.Config;
import unsw.DataReader;
import unsw.online.MetaPath;
import unsw.online.basic.HomBCore;

/**
 * @author fangyixiang
 * @date 10 Sep. 2018
 * the pruned-path method for answering a query
 */
public class AvgDegree {
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	
	private int queryId = -1;//the query vertex id
	private MetaPath queryMPath = null;//the query meta-path
	private int queryK = -1;//the threshold k
	
	public AvgDegree(int graph[][], int vertexType[], int edgeType[]) {
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
		buildGraph();
		
		return null;
	}

	private Map<Integer, Set<Integer>> buildGraph() {
		long count = 0;
		Map<Integer, Set<Integer>> graphMap = new HashMap<Integer, Set<Integer>>();
		for(int curId = 0;curId < graph.length;curId ++) {
			if(vertexType[curId] == queryMPath.vertex[0]) {
				List<Set<Integer>> visitList = new ArrayList<Set<Integer>>();
				for(int i = 0;i <=queryMPath.pathLen;i ++)   visitList.add(new HashSet<Integer>());
				Set<Integer> pnbSet = new HashSet<Integer>();
				findPNeighbors(curId, curId, 0, visitList, pnbSet);//find all the pnbs of curId
				
				count += pnbSet.size();
				graphMap.put(curId, null);
				
				if(graphMap.size() % 10000 == 0) {
					System.out.println("finished " + graphMap.size() + " average-degree" + (count * 1.0 / graphMap.size()));
				}
			}
		}
		
		System.out.println("average degree: " + count * 1.0 / graphMap.size());
		
		return graphMap;
	}
	
	private void findPNeighbors(int startID, int curId, int index, List<Set<Integer>> visitList, Set<Integer> pnbSet) {
		int targetVType = queryMPath.vertex[index + 1], targetEType = queryMPath.edge[index];
		
		int nbArr[] = graph[curId];
		for(int i = 0;i < nbArr.length;i += 2) {
			int nbVertexID = nbArr[i], nbEdgeID = nbArr[i + 1];
			Set<Integer> visitSet = visitList.get(index + 1);
			if(!visitSet.contains(nbVertexID) && targetVType == vertexType[nbVertexID] && targetEType == edgeType[nbEdgeID]) {
				if(index + 1 < queryMPath.pathLen) {
					findPNeighbors(startID, nbVertexID, index + 1, visitList, pnbSet);
					visitSet.add(nbVertexID);//mark this vertex (and its branches) as visited
				}else {//a meta-path has been found
					if(nbVertexID != startID)   pnbSet.add(nbVertexID);
					visitSet.add(nbVertexID);//mark this vertex (and its branches) as visited
				}
			}
		}
	}

	public static void main(String[] args) {
		DataReader dataReader = new DataReader(Config.dblpGraph, Config.dblpVertex, Config.dblpEdge);
		int graph[][] = dataReader.readGraph();
		int vertexType[] = dataReader.readVertexType();
		int edgeType[] = dataReader.readEdgeType();
		
		int queryId = 111509;
		int queryK = 10;
		int vertex[] = {1, 0, 2, 0, 1}, edge[] = {3, 1, 4, 0};//1052
//		int vertex[] = {1, 0, 1}, edge[] = {3, 0};//5.67
		MetaPath queryMPath = new MetaPath(vertex, edge);
		
		AvgDegree avg = new AvgDegree(graph, vertexType, edgeType);
		avg.query(queryId, queryMPath, queryK);
	}
}
//5.672121535581932
