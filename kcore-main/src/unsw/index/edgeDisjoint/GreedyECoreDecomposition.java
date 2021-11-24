package unsw.index.edgeDisjoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.util.Pair;
import unsw.index.Decomposition;
import unsw.online.BatchSearch;
import unsw.online.Greedy;
import unsw.online.MetaPath;
import unsw.online.edgeDisjoint.EMaxFlow;

public class GreedyECoreDecomposition implements Decomposition{
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	private MetaPath queryMPath = null;//the query meta-path
	private Set<Integer> keepSet = null;
	private Greedy greedy = null;

	public GreedyECoreDecomposition(int[][] graph , int[] vertexType , int[] edgeType) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
	}

	private int reverseOrderArr[] = null;
	public Map<Integer , Integer> decompose(MetaPath queryMPath) {
		this.queryMPath = queryMPath;
		keepSet = new HashSet<Integer>();
		for (int i = 0 ; i < vertexType.length ; i++) {
			if (vertexType[i] == queryMPath.vertex[0]) {
				keepSet.add(i);
			}
		}
		
		greedy = new Greedy(graph, vertexType, edgeType, queryMPath, keepSet);
		
		Map<Integer, Integer> coreNumMap = new HashMap<Integer , Integer>();
		Map<Integer, Set<Integer>> visitedEdgeMap = new HashMap<Integer, Set<Integer>>();//vid -> edge set
		Map<Integer, Map<Integer, int[]> > pathsMap = new HashMap<Integer, Map<Integer, int[]>> ();//vid -> (tid -> path)
		for (int vid : keepSet) {
			coreNumMap.put(vid , 0);
			
			Set<Integer> newEdgeSet = new HashSet<Integer>();
 			visitedEdgeMap.put(vid, newEdgeSet);
 			
 			Map<Integer , int[] > newPathMap = new HashMap<Integer , int[]>();
	 		pathsMap.put(vid, newPathMap);
		}
		
		BatchSearch affVertexFinder = new BatchSearch(graph , vertexType , edgeType , queryMPath);
		int curCoreNum = 1;
		int orderIdx = 1, size = keepSet.size();
		reverseOrderArr = new int[size];//keep the reverse order of vertices
		while (curCoreNum < keepSet.size()) {
			Set<Integer> removeSet = new HashSet<Integer>();
			boolean firstRound = true; // to determine weather this is the first round
			Set<Integer> affectedVertices = keepSet;//initialize affectedVertices as keepSet
			while(!removeSet.isEmpty() || firstRound ){
				removeSet.clear();
				firstRound = false;
				for (int vertexId : affectedVertices) {

					int curNeighborsNum = findKPaths(vertexId , curCoreNum , pathsMap , visitedEdgeMap);
					if (curNeighborsNum < curCoreNum) {
						removeSet.add(vertexId);
					}
				}
				if (!removeSet.isEmpty() ) {
					//step a: collect core numbers
					for(int vid:removeSet) {
						reverseOrderArr[size - orderIdx] = vid; orderIdx ++;
						coreNumMap.put(vid, curCoreNum - 1);
					}
					
					//step b: remove all the vertices in a batch
					keepSet.removeAll(removeSet);//delete removeSet from keepSet
					visitedEdgeMap.keySet().removeAll(removeSet);//delete removeSet from visitedEdgeMap
					pathsMap.keySet().removeAll(removeSet);//delete removeSet from pathsMap
					
					//step c: find all the affected vertices
					//affectedVertices = collectInfluencedVerticesSet(removeSet);//compute which vertices are affected by these deleted vertices\
					affectedVertices = affVertexFinder.collect(removeSet, keepSet);
					//step d: update pathMap and visitedEdgeMap of the affected vertices
					update(pathsMap , visitedEdgeMap , removeSet , affectedVertices);//update the visitedEdgeMap and removeSet
				}
			}
			if (keepSet.isEmpty()) {
				return coreNumMap;
			}

			curCoreNum++;
//			System.out.println(keepSet.size() + ":" + (curCoreNum - 1));
		}
		if (keepSet.size() > 0) {
			curCoreNum--;
			for (int vid : keepSet) {
				coreNumMap.put(vid, curCoreNum);
			}
		}
		return coreNumMap;
	}
	
	private void update( Map<Integer , Map<Integer , int[]>> pathsMap , Map<Integer , Set<Integer>> visitedEdgeMap , Set<Integer> removeSet , Set<Integer> affectedVSet) {
		//delete the paths which ended in removeSet
		for (int affectedVid : affectedVSet) {
 			Map<Integer , int[]> curPathMap = pathsMap.get(affectedVid); 
 			if (curPathMap == null) {
 				continue;
 			}
 			Set<Integer> curEdgeSet = visitedEdgeMap.get(affectedVid);
 			Set<Integer> deleteVSet = new HashSet<Integer>();// to store which vertices needs to be deleted as neighbor hoods
 			Set<Integer> deleteESet = new HashSet<Integer>();// to store which edges needed to be deleted as metaPaths
 			for ( int vid : curPathMap.keySet() ) {
 				if ( removeSet.contains(vid) ) {
 					deleteVSet.add(vid);
 					for (int l = 0 ; l < queryMPath.pathLen ; l++) {
 						int eid = curPathMap.get(vid)[l];
 						deleteESet.add(eid);
 					}
 				}
 			}
 			if ( !deleteVSet.isEmpty() ) {
 	 			curPathMap.keySet().removeAll(deleteVSet);
 			}
 			if ( ! (deleteESet.isEmpty() || curEdgeSet == null) ) {
 	 			curEdgeSet.removeAll(deleteESet);
 			}
		}
	}
	
	private int findKPaths(int vertexId , int coreNumber , Map<Integer , Map<Integer , int[]>> pathsMap , Map<Integer , Set<Integer>> visitedEdgeMap) {
		EMaxFlow eMaxFlow = new EMaxFlow(graph , vertexType , edgeType , queryMPath);
		
		Map<Integer , int[]> curPathMap = pathsMap.get(vertexId);//the paths map for vertexId;
		Set<Integer> curVisitedEdgeSet = visitedEdgeMap.get(vertexId);// visited edges set for vertexId
		if (curVisitedEdgeSet == null) {
			int curDegree = eMaxFlow.obtainEDegree(vertexId, keepSet, curPathMap);
			pathsMap.put(vertexId , null);
			visitedEdgeMap.put(vertexId, null);
			return curDegree;
		}
		int curSize = curPathMap.size();
		if (curSize >= coreNumber) {
			return coreNumber;
		}
		
		while (curSize < coreNumber) {
			Pair<Integer , int[]> newPair = greedy.obtainOneEPath(vertexId , curVisitedEdgeSet , curPathMap.keySet());
			if (newPair == null) {//call maximumFlow
				int curDegree = eMaxFlow.obtainEDegree(vertexId, keepSet, curPathMap);
				//int curDegree = eMaxFlow.obtainEPaths(vertexId, keepSet, curPathMap).size();
				pathsMap.put(vertexId , null);
				visitedEdgeMap.put(vertexId, null);
				return curDegree;
			}
			curPathMap.put(newPair.getKey(), newPair.getValue());// update the curPathMap
			curSize = curPathMap.size();// curSize + 1
		}
		return curSize;	
	}
	
	public int[] getReverseOrderArr() {
		return reverseOrderArr;
	}
}
