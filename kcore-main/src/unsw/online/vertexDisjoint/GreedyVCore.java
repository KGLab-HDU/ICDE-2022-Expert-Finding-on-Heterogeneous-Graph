package unsw.online.vertexDisjoint;

import java.util.*;

import javafx.util.Pair;
import unsw.online.BatchSearch;
import unsw.online.BatchLinker;
import unsw.online.BatchLinkerCSH;
import unsw.online.Greedy;
import unsw.online.MetaPath;

/**
 * @author fangyixiang
 * @date 24 Sep. 2018
 * The baseline algorithm for the edge-disjoint core computation
 */
public class GreedyVCore {
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	
	public GreedyVCore(int graph[][], int vertexType[], int edgeType[]) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
	}
	
	public Set<Integer> query(int queryId, MetaPath queryMPath, int queryK) {
		//step 0: check whether queryId's type matches with the meta-path
		if(queryMPath.vertex[0] != vertexType[queryId])   return null;
		
		//step 1: compute the connected subgraph via batch-search with labeling (BSL)
		BatchLinker batchLinker = new BatchLinker(graph, vertexType, edgeType);
		Set<Integer> keepSet = batchLinker.link(queryId, queryMPath);
		
		//step 2: perform pruning
		int SECONDVertexTYPE = queryMPath.vertex[1], SECONDEdgeTYPE = queryMPath.edge[0];
		Iterator<Integer> keepIter = keepSet.iterator();
		while(keepIter.hasNext()) {
			int id = keepIter.next();
			int count = 0;
			for(int i = 0;i < graph[id].length;i += 2) {
				int nbVId = graph[id][i], nbEId = graph[id][i + 1];
				if(vertexType[nbVId] == SECONDVertexTYPE && edgeType[nbEId] == SECONDEdgeTYPE) {
					count ++;
					if(count >= queryK) break;
				}
			}
			if(count < queryK) keepIter.remove();
		}
		if(!keepSet.contains(queryId)) return null;
		
		//step 3: initialization variables
		int pathLen = queryMPath.pathLen;
		int threshold = (queryK - pathLen * (queryK / pathLen) > 0.0001) ? (queryK / pathLen) : (queryK / pathLen - 1);
		VMaxFlow vMaxFlow = new VMaxFlow(graph, vertexType, edgeType, queryMPath);
		Map<Integer, Map<Integer, int[]>> allPathMap = new HashMap<Integer, Map<Integer, int[]>>();//vid -> (tid -> path)
		Map<Integer, List<Set<Integer>>> allVisitMap = new HashMap<Integer, List<Set<Integer>>>();//vid -> edge set
		
		//step 4: find k-path for each vertex
		Greedy greedy = new Greedy(graph, vertexType, edgeType, queryMPath, keepSet);
		Set<Integer> deleteSet = new HashSet<Integer>();//vertices to be deleted
		for(int vid:keepSet) {
			List<Set<Integer>> visitList = new ArrayList<Set<Integer>>();
			for(int i = 0;i < pathLen;i ++) visitList.add(new HashSet<Integer>());
			Map<Integer, int[]> pathMap = new HashMap<Integer, int[]>();
			while(true) {//invoke the greedy algorithm
				Pair<Integer, int[]> pair = greedy.obtainOneVPath(vid, visitList);
				if(pair != null)   pathMap.put(pair.getKey(), pair.getValue());//eNB -> metapath
				else   break;

				if(pathMap.size() >= queryK)   break;
			}

			if(pathMap.size() <= threshold) {
				deleteSet.add(vid);
			}else if(pathMap.size() < queryK) {
				Map<Integer, int[]> maxFlowPathMap = vMaxFlow.obtainVPaths(vid, keepSet, pathMap);//using max-flow algorithm
				
				if(maxFlowPathMap.size() < queryK) {
					deleteSet.add(vid);
				}else {
					allVisitMap.put(vid, null);//this vertex must use max-flow algorithm
					allPathMap.put(vid, maxFlowPathMap);
				}
			}else {
				allVisitMap.put(vid, visitList);
				allPathMap.put(vid, pathMap);
			}
		}
		
		//step 5: iteratively perform batch deleting
		BatchSearch batchSearch = new BatchSearch(graph, vertexType, edgeType, queryMPath);
		while(deleteSet.size() > 0) {
			//step 5.1: perform delete operation
			for(int vid:deleteSet) {
				keepSet.remove(vid);
				allVisitMap.remove(vid);
				allPathMap.remove(vid);
			}
			
			//step 5.2: locate the affected vertices
			Set<Integer> batchSet = batchSearch.collect(deleteSet, keepSet);
			
			//step 5.3: increase their neighbors and find vertices needed to be deleted
			Set<Integer> nextDeleteSet = new HashSet<Integer>();//vertices to be deleted
			for(int vid:batchSet) {
				List<Set<Integer>> visitList = allVisitMap.get(vid);
				Map<Integer, int[]> pathMap = allPathMap.get(vid);
				if(visitList != null) {
					//step a: remove some paths
					Iterator<Map.Entry<Integer, int[]>> iter = pathMap.entrySet().iterator();
					while(iter.hasNext()) {
						Map.Entry<Integer, int[]> entry = iter.next();
						int vNBId = entry.getKey(), tmpPath[] = entry.getValue();
						if(!keepSet.contains(vNBId)) {
							iter.remove();//update the pathMap
							for(int i = 0;i < pathLen;i ++) {
								visitList.get(i).remove(tmpPath[i + 1]);
							}
						}
					}
					
					//step b: supplement some new paths
					while(true) {
						Pair<Integer, int[]> pair = greedy.obtainOneVPath(vid, visitList);
						if(pair != null) {
							pathMap.put(pair.getKey(), pair.getValue());
						}else {
							break;
						}
						
						if(pathMap.size() >= queryK)   break;
					}
					
					//step c: invoke the exact algorithm
					if(pathMap.size() < queryK) {
						Map<Integer, int[]> maxFlowPathMap = vMaxFlow.obtainVPaths(vid, keepSet, pathMap);//using max-flow algorithm
						
						if(maxFlowPathMap.size() < queryK) {
							nextDeleteSet.add(vid);
						}else{
							allVisitMap.put(vid, null);//this vertex must use max-flow algorithm
							allPathMap.put(vid, maxFlowPathMap);
						}
					}
				}else {
					Map<Integer, int[]> maxFlowPathMap = vMaxFlow.obtainVPaths(vid, keepSet, pathMap);//using max-flow algorithm
					if(maxFlowPathMap.size() < queryK) {
						nextDeleteSet.add(vid);
					}else {
						allPathMap.put(vid, maxFlowPathMap);
					}
				}
			}
			
			//step 5.5: prepare for the next round of deletion
			deleteSet = nextDeleteSet;
		}
		
		//step 6: find a connected community
		Map<Integer, Set<Integer>> tmpPnbMap = new HashMap<Integer, Set<Integer>>();
		for(int id:keepSet) {
			Map<Integer, int[]> pathMap = allPathMap.get(id);
			Set<Integer> set = new HashSet<Integer>();
			for(int nbId:pathMap.keySet()) set.add(nbId);
			tmpPnbMap.put(id, set);
		}
		BatchLinkerCSH ccFinder = new BatchLinkerCSH(graph, vertexType, edgeType, queryId, queryMPath, keepSet, tmpPnbMap);
		return ccFinder.computeCC();
	}
}
