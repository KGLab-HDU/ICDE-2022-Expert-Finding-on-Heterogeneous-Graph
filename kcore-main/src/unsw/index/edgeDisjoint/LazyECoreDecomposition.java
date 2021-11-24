package unsw.index.edgeDisjoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import unsw.index.Decomposition;
import unsw.online.BatchSearch;
import unsw.online.MetaPath;
import unsw.online.edgeDisjoint.EMaxFlow;

public class LazyECoreDecomposition implements Decomposition{
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	private MetaPath queryMPath = null;//the query meta-path
	private Set<Integer> keepSet = null;
	
	public LazyECoreDecomposition(int[][] graph , int[] vertexType , int[] edgeType) {
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
		
		EMaxFlow mfe = new EMaxFlow(graph , vertexType , edgeType , queryMPath);
		Map<Integer, Integer> coreNumMap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> degreeMap = new HashMap<Integer, Integer>();
		for (int vid : keepSet) {
			coreNumMap.put(vid , 0);
			degreeMap.put(vid, 0);
		}
		
		BatchSearch affVertexFinder = new BatchSearch(graph , vertexType , edgeType , queryMPath);
		int curCoreNum = 1;
		int orderIdx = 1, size = keepSet.size();
		reverseOrderArr = new int[size];//keep the reverse order of vertices
		while (curCoreNum < keepSet.size()) {
			//step 1: initialize a queue 
			Queue<Integer> removeQueue = new LinkedList<Integer>();
			Set<Integer> visitSet = new HashSet<Integer>();
			for (int vid : keepSet) {
				if (degreeMap.get(vid) < curCoreNum && !visitSet.contains(vid)) {
					removeQueue.add(vid);
					visitSet.add(vid);
				}
			}
			
			//step 2: remove vertices iteratively in a lazy manner
			while (!removeQueue.isEmpty()) {
				int removeId = removeQueue.poll();
				int exactDegree = mfe.obtainEDegree(removeId, keepSet, null);
				if(exactDegree < curCoreNum) {
					keepSet.remove(removeId);
					coreNumMap.put(removeId, curCoreNum - 1);//collect the core numbers
					reverseOrderArr[size - orderIdx] = removeId; orderIdx ++;
					
					//Set<Integer> affectedVertices = findBNeighbors(removeId, keepSet);
					Set<Integer> affectedVertices = affVertexFinder.collect(removeId, keepSet);
					for (int affectedVid : affectedVertices) {
						int newDegree = degreeMap.get(affectedVid) - 1;
						degreeMap.put(affectedVid, newDegree);
						if (newDegree < curCoreNum && !visitSet.contains(affectedVid)) {
							removeQueue.add(affectedVid);
							visitSet.add(affectedVid);
						}					
					}
				}else {
					degreeMap.put(removeId, exactDegree);
					visitSet.remove(removeId);
				}
			}
			
			//step 3: prepare for the next k
			curCoreNum++;
			//System.out.println("k=" + (curCoreNum - 1) + " |keepSet|=" + keepSet.size());
		}
		if (keepSet.size() > 0) {
			curCoreNum--;
			for (int vid : keepSet) {
				coreNumMap.put(vid, curCoreNum);
			}
		}
		return coreNumMap;
	}
	
	public int[] getReverseOrderArr() {
		return reverseOrderArr;
	}
}