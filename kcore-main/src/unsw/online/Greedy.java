package unsw.online;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javafx.util.Pair;

/**
 * @author fangyixiang
 * @date 9 Oct. 2018
 * Greedy algorithm for computing e-degree and v-degrees
 */
public class Greedy {
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	private MetaPath queryMPath = null;//the query meta-path
	private Set<Integer> keepSet = null;
	
	public Greedy(int graph[][], int vertexType[], int edgeType[], MetaPath queryMPath, Set<Integer> keepSet) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
		this.queryMPath = queryMPath;
		this.keepSet = keepSet;
	}
	
	//find a edge-disjoint path
	public Pair<Integer, int[]> obtainOneEPath(int vid, Set<Integer> visitSet, Set<Integer> eNBVertexSet) {
		Stack<Integer> vertexStack = new Stack<Integer>();
		Stack<Integer> edgeStack = new Stack<Integer>();
		Map<Integer , Integer> visitLocationMap = new HashMap<Integer , Integer>();
		vertexStack.add(vid);
		edgeStack.add(-1);
		Set<Integer> tmpVisitSet = new HashSet<Integer>();//temporally mark the visited edges
		
		while (!vertexStack.isEmpty()) {
			boolean stackPush = false;
			int curPeek = vertexStack.peek();
			int neighborArray[] = graph[curPeek];
			
			if (!visitLocationMap.containsKey(curPeek)) {
				visitLocationMap.put(curPeek, 0);
			} 			
			int location = visitLocationMap.get(curPeek);
			
			for(int i = location ;i < neighborArray.length;i += 2) {
				int nbVId = neighborArray[i], nbEId = neighborArray[i + 1];
				int layer = vertexStack.size();
				if (!(visitSet.contains(nbEId) || tmpVisitSet.contains(nbEId))
						&& vertexType[nbVId] == queryMPath.vertex[layer] && edgeType[nbEId] == queryMPath.edge[layer - 1]) {
					
					visitLocationMap.put(curPeek, i + 2);
					stackPush = true;
					vertexStack.push(nbVId);
					edgeStack.push(nbEId);
					tmpVisitSet.add(nbEId);
					break;
				}
			}
			if(vertexStack.size() == queryMPath.pathLen + 1) {
				int eNBVertexId = vertexStack.peek();
				if(eNBVertexId != vid && !eNBVertexSet.contains(eNBVertexId) && keepSet.contains(eNBVertexId)) {
					break;
				}else {//the e-neighbor cannot be vid or other e-neighbors which have been selected
					vertexStack.pop();
					edgeStack.pop();
				}
			}
			if(!stackPush) {
				vertexStack.pop();
				edgeStack.pop();
			}
		}
				
		if (vertexStack.isEmpty()) {//there is no meta-path
			return null;
		}else {
			int[] metapath = new int[queryMPath.pathLen];
			for(int i = 1;i <= queryMPath.pathLen;i ++) {
				int eid = edgeStack.get(i);
				metapath[i - 1] = eid;//obtain the metapath
				visitSet.add(eid);//mark the path as visited
			}
			return new Pair<Integer, int[]>(vertexStack.peek(), metapath);
		}
	}
	
	//find a vertex-disjoint path
	public Pair<Integer, int[]> obtainOneVPath(int vertexId, List<Set<Integer>> visitList) {
		//Set<Integer> tmpVisitSet = new HashSet<Integer>();
		List<Set<Integer>> tmpVisitList = new ArrayList<Set<Integer>>();
		for (int i = 0 ; i < visitList.size() ; i++) {
			Set<Integer> iLVisitSet = new HashSet<Integer>();
			tmpVisitList.add(iLVisitSet);
		}
		
		Stack<Integer> vertexStack = new Stack<Integer>();
		Map<Integer , Integer> visitLocationMap = new HashMap<Integer , Integer>();
		vertexStack.add(vertexId);
		while (!vertexStack.isEmpty()) {
			boolean stackPush = false;
			int curPeek = vertexStack.peek();
			int neighborAry[] = graph[curPeek];
			if (!visitLocationMap.containsKey(curPeek)) {
				visitLocationMap.put(curPeek, 0);
			} 
			int curLocation = visitLocationMap.get(curPeek);
			for(int i = curLocation ; i < neighborAry.length ; i = i + 2) {
				int neighborVid = neighborAry[i], neighborEid = neighborAry[i + 1];				
				int layer = vertexStack.size();
				boolean condition = !(visitList.get(layer - 1).contains(neighborVid) || /*tmpVisitSet.contains(neighborVid)*/tmpVisitList.get(layer - 1).contains(neighborVid)   )
				&& vertexType[neighborVid] == queryMPath.vertex[layer] && edgeType[neighborEid] == queryMPath.edge[layer - 1];
				if (condition ) {	
					visitLocationMap.put(curPeek, i + 2);
					stackPush = true;
					vertexStack.push(neighborVid);
					//tmpVisitSet.add(neighborVid);
					tmpVisitList.get(layer - 1).add(neighborVid);
					break;
				}
			}
			
			if(vertexStack.size() == queryMPath.pathLen + 1) {
				int curNeighborVid = vertexStack.peek();
				if ( vertexId != curNeighborVid && keepSet.contains(curNeighborVid)  )  {
					break;
				} else {
					vertexStack.pop();
				}
			}
			if(!stackPush) {
				vertexStack.pop();
			}
		}
		
		if (vertexStack.isEmpty()) {
			//there is no meta-path
			return null;
		}else {
			int[] metapath = new int[queryMPath.pathLen + 1];
			for(int i = 0 ; i <= queryMPath.pathLen ; i++) {
				int vid = vertexStack.get(i);
				metapath[i] = vid;//obtain the metapath
				if (i > 0) {
					visitList.get(i - 1).add(vid);//mark the path as visited
				}
			}
			return new Pair<Integer, int[]>(vertexStack.peek(), metapath);
		}
	}
}

