package unsw.index.vertexDisjoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.util.Pair;
import unsw.index.Decomposition;
import unsw.online.BatchSearch;
import unsw.online.Greedy;
import unsw.online.MetaPath;
import unsw.online.vertexDisjoint.VMaxFlow;

public class GreedyVCoreDecomposition implements Decomposition{
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	private MetaPath queryMPath = null;//the query meta-path
	private Set<Integer> keepSet = null;
	private Greedy greedy = null;

	public GreedyVCoreDecomposition(int[][] graph , int[] vertexType , int[] edgeType) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
	}

	private int reverseOrderArr[] = null;
	public Map<Integer, Integer> decompose(MetaPath queryMPath) {
		this.queryMPath = queryMPath;
		keepSet = new HashSet<Integer>();
		for (int i = 0 ; i < vertexType.length ; i++) {
			if (vertexType[i] == queryMPath.vertex[0]) {
				keepSet.add(i);
			}
		}
		greedy = new Greedy(graph, vertexType, edgeType, queryMPath, keepSet);
		
		Map<Integer, Integer> coreNumMap = new HashMap<Integer , Integer>();
		Map<Integer , List<Set<Integer> > > visitedVertexMap = new HashMap<Integer , List<Set<Integer>>>();// vid -> verticesListSet
		Map<Integer, Map<Integer, int[]> > pathsMap = new HashMap<Integer, Map<Integer, int[]>> ();//vid -> (tid -> path)
		for (int vid : keepSet) {
			coreNumMap.put(vid , 0);
			List<Set<Integer>> newVerticeSet = new ArrayList<Set<Integer>>();
			for (int l = 0 ; l < queryMPath.pathLen ; l++) {
				Set<Integer> lthSet = new HashSet<Integer>();
				newVerticeSet.add(lthSet);
			}
			visitedVertexMap.put(vid, newVerticeSet);
 			
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
					int curNeighborsNum = findKPaths(vertexId , curCoreNum , pathsMap , visitedVertexMap);
					if (curNeighborsNum < curCoreNum) {
						removeSet.add(vertexId);
					}
				}
				if (!removeSet.isEmpty() ) {
					//step a: collect core numbers
					for(int vid:removeSet) {
						coreNumMap.put(vid, curCoreNum - 1);
						reverseOrderArr[size - orderIdx] = vid; orderIdx ++;
					}
					
					//step b: remove all the vertices in a batch
					keepSet.removeAll(removeSet);//delete removeSet from keepSet
					visitedVertexMap.keySet().removeAll(removeSet);//delete removeSet from visitedEdgeMap
					pathsMap.keySet().removeAll(removeSet);//delete removeSet from pathsMap
					
					//step c: find all the affected vertices
					//affectedVertices = collectInfluencedVerticesSet(removeSet);//compute which vertices are affected by these deleted vertices\
					affectedVertices = affVertexFinder.collect(removeSet, keepSet);
					//step d: update pathMap and visitedEdgeMap of the affected vertices
					update(pathsMap , visitedVertexMap , removeSet , affectedVertices);//update the visitedEdgeMap and removeSet
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
	
	private void update( Map<Integer , Map<Integer , int[]>> pathsMap , Map<Integer , List<Set<Integer> > > visitedVertexMap , Set<Integer> removeSet , Set<Integer> affectedVSet) {
		//delete the paths which ended in removeSet
		for (int affectedVid : affectedVSet) {
 			Map<Integer , int[]> curPathMap = pathsMap.get(affectedVid); 
 			if (curPathMap == null) {
 				continue;
 			}
 			List<Set<Integer>> curVList = visitedVertexMap.get(affectedVid);
 			Set<Integer> deleteNbrVSet = new HashSet<Integer>();// to store which vertices needs to be deleted as neighbor hoods
 			for ( int nid : curPathMap.keySet() ) {
 				if ( removeSet.contains(nid) ) {
 					deleteNbrVSet.add(nid);
 					for (int l = 1 ; l <= queryMPath.pathLen ; l++) {
 						int vid = curPathMap.get(nid)[l];
 						if ( !curVList.isEmpty() ) {
 							if ( !curVList.get(l - 1).isEmpty()) {
 								curVList.get(l - 1).remove(vid);
 							}
 						}
 					}
 				}
 			}
 			if ( !deleteNbrVSet.isEmpty() ) {
 	 			curPathMap.keySet().removeAll(deleteNbrVSet);
 			}
		}
	}
	
	private int findKPaths(int vertexId , int coreNumber , Map<Integer , Map<Integer , int[]>> pathsMap , Map<Integer , List<Set<Integer> > > visitedVertexMap) {
		VMaxFlow vMaxFlow = new VMaxFlow(graph , vertexType , edgeType , queryMPath);
		
		Map<Integer , int[]> curPathMap = pathsMap.get(vertexId);//the paths map for vertexId;
		List<Set<Integer>> curVisitedVertexList = visitedVertexMap.get(vertexId);// visited edges set for vertexId
		if (curVisitedVertexList == null) {
			int curDegree =  vMaxFlow.obtainVDegree(vertexId, keepSet, curPathMap);
			visitedVertexMap.put(vertexId, null);
			return curDegree;
		}
		int curSize = curPathMap.size();
		if (curSize >= coreNumber) {
			return coreNumber;
		}
		
		while (curSize < coreNumber) {
			//int[] newPath = greedy.obtainOneVPath(vertexId , curVisitedVertexList);//obtain one more metapath
			Pair<Integer , int[]> newPair= greedy.obtainOneVPath(vertexId , curVisitedVertexList);//obtain one more metapath
			
			if (newPair == null) {//call maximumFlow
				int curDegree =  vMaxFlow.obtainVDegree(vertexId, keepSet , curPathMap);
				//int curDegree = vMaxFlow.obtainVPaths(vertexId, keepSet, curPathMap).size();
				pathsMap.put(vertexId , null);
				visitedVertexMap.put(vertexId, null);
				return curDegree;
			}
			curPathMap.put(newPair.getKey(), newPair.getValue());
			curSize = curPathMap.size();// curSize + 1
		}
		return curSize;	
	}

	public int[] getReverseOrderArr() {
		return reverseOrderArr;
	}
}
