package unsw.online.vertexDisjoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javafx.util.Pair;
import unsw.online.BatchSearch;
import unsw.online.BatchLinkerCSH;
import unsw.online.Greedy;
import unsw.online.MetaPath;
import unsw.online.basic.FastBCore;

/**
 * @author fangyixiang
 * @date 9 Oct. 2018
 * The baseline algorithm for the vertex-disjoint core computation
 */
public class LazyVCore {
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	private FastBCore quickCore = null;
	
	public LazyVCore(int graph[][], int vertexType[], int edgeType[]) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
		this.quickCore = new FastBCore(graph, vertexType, edgeType);
	}
	
	public Set<Integer> query(int queryId, MetaPath queryMPath, int queryK) {
		//step 0: check whether queryId's type matches with the meta-path
		if(queryMPath.vertex[0] != vertexType[queryId])   return null;

		//step 1: compute the basic-core
		Set<Integer> keepSet = quickCore.query(queryId, queryMPath, queryK);
		if(keepSet == null)   return null;
		
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
		
		//step 3: initialization
		int pathLen = queryMPath.pathLen;
		int threshold = (queryK - pathLen * (queryK / pathLen) > 0.0001) ? (queryK / pathLen) : (queryK / pathLen - 1);
		Map<Integer, Map<Integer, int[]>> allPathMap = new HashMap<Integer, Map<Integer, int[]>>();//vid -> (tid -> path)
		int visitArr[] = new int[graph.length];//vertices added into queue
		int degree[] = new int [graph.length];//vertex -> degree
		
		//step 4: compute the degree of each vertex using the greedy algorithm
		Greedy greedy = new Greedy(graph, vertexType, edgeType, queryMPath, keepSet);
		Queue<Integer> queue = new LinkedList<Integer>();//a queue
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
			
			degree[vid] = pathMap.size();
			if(degree[vid] <= threshold) {
				queue.add(vid);
				visitArr[vid] = 1;//must delete
			}else {
				if(degree[vid] < queryK) {
					queue.add(vid);
					visitArr[vid] = 2;//need double check
				}
				allPathMap.put(vid, pathMap);
			}
		}
		
		//step 5: iteratively remove vertices
		VMaxFlow vMaxFlow = new VMaxFlow(graph, vertexType, edgeType, queryMPath);
		BatchSearch batchSearch = new BatchSearch(graph, vertexType, edgeType, queryMPath);
		while(queue.size() > 0) {
			int curId = queue.poll();
			
			if(visitArr[curId] == 1) {//the first round
				keepSet.remove(curId);//delete this vertex
				
				//update the v-degrees of affected vertices
				Set<Integer> pnbSet = batchSearch.collect(curId, keepSet);//find the affected vertices
				for(int pnbId:pnbSet) {
					if(visitArr[pnbId] == 0) {//impose restriction
						degree[pnbId] = degree[pnbId] - 1;
						if(degree[pnbId] < queryK) {
							queue.add(pnbId);
							visitArr[pnbId] = 2;//it is not deleted in the first round
						}
					}
				}
			}else if(visitArr[curId] == 2){//the rest rounds
				Map<Integer, int[]> pathMap = allPathMap.get(curId);//use the old paths
				Map<Integer, int[]> maxFlowPathMap = vMaxFlow.obtainVPaths(curId, keepSet, pathMap);//compute the degree in a lazy manner
				if(maxFlowPathMap.size() < queryK) {
					keepSet.remove(curId);//delete this vertex
					
					//update the e-degrees of affected vertices
					Set<Integer> pnbSet = batchSearch.collect(curId, keepSet);//find the affected vertices
					for(int pnbId:pnbSet) {
						if(visitArr[pnbId] == 0) {//impose restriction
							degree[pnbId] = degree[pnbId] - 1;
							if(degree[pnbId] < queryK) {
								queue.add(pnbId);
								visitArr[pnbId] = 2;//it is not deleted in the first round
							}
						}
					}
				}else {
					degree[curId] = maxFlowPathMap.size();
					allPathMap.put(curId, maxFlowPathMap);
					visitArr[curId] = 0;//this vertex can be enqueued again
				}
			}
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