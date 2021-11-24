package unsw.online.vertexDisjoint;

import java.util.*;
import unsw.online.MetaPath;
/**
 * @author fangyixiang
 * @date 24 Sep. 2018
 * The max-flow-based algorithm for computing the v-degree
 */

public class VMaxFlow {
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	private MetaPath queryMPath = null;//the query meta-path
	private Map<Integer, Set<Integer>> flowGraphMap = null;
	private List<Set<Integer>> vertexList = null;
		
	public VMaxFlow(int graph[][], int vertexType[], int edgeType[], MetaPath queryPath) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
		this.queryMPath = queryPath;
	}

	public int obtainVDegree(int vertexId, Set<Integer> keepSet, Map<Integer, int[]> pathMap) {
		return obtainVNeighbors(vertexId, keepSet, pathMap).size();
	}
	
	public Set<Integer> obtainVNeighbors(int vertexId, Set<Integer> keepSet, Map<Integer, int[]> pathMap) {
		flowGraphMap = null;
		vertexList = null;
		
		//step 1: create the flow network
		createFlowGraph(vertexId, keepSet, pathMap);
		if(flowGraphMap == null)   return new HashSet<Integer>();
		
		//step 2: find the neighbor by finding an argument path
		if(pathMap != null) {
			return collectVNeighbors(vertexId, pathMap.keySet());
		}else {
			return collectVNeighbors(vertexId, null);
		}
	}
	
	private Set<Integer> collectVNeighbors(int vertexId, Set<Integer> existSet) {
		Set<Integer> nbSet = new HashSet<Integer>();
		if(existSet != null) {
			for(int id:existSet) {
				nbSet.add(id);
			}
		}
		int neighborId = augOnePathNeighbor(vertexId, -1);
		while(neighborId != -1) {
			nbSet.add(neighborId);
			neighborId = augOnePathNeighbor(vertexId, -1);
		}
		return nbSet;
	}

	//Notice: this method is added on Oct 31
	public Map<Integer, int[]> obtainVPaths(int vertexId, Set<Integer> keepSet, Map<Integer, int[]> pathMap) {
		flowGraphMap = null;
		vertexList = null;
		
		Set<Integer> removeSet = new HashSet<Integer>();
		for (int endPoint : pathMap.keySet() ) {
			if ( !keepSet.contains(endPoint) ) {
				removeSet.add(endPoint);
			}
		}
		pathMap.keySet().removeAll(removeSet);

		
		//step 1: create the flow network
		createFlowGraph(vertexId, keepSet, pathMap);
		if(flowGraphMap == null)   return new HashMap<Integer, int[]>();
		
		int neighborId = augOnePathNeighbor(vertexId, -1);
		while(neighborId != -1) {
			neighborId = augOnePathNeighbor(vertexId, -1);
		}

		//step 2: find the edge-disjoint paths from the flow network
		Map<Integer, int[]> maxFlowPathMap = new HashMap<Integer , int[]>();
		
		for (int vid : flowGraphMap.get(-1)) {
			int[] inverseVPath = obtainOnePath(vid , vertexId);//from right to left
			int[] vPath = new int[inverseVPath.length];
			for (int i = 0 ; i < vPath.length ; i++) {
				vPath[i] = inverseVPath[vPath.length - i - 1];
			}
			
			if (inverseVPath != null) {
				int endVertex = vPath[vPath.length - 1];
				//int[] ePath = getEPath(inverseVPath);
				//maxFlowPathMap.put(endVertex, ePath);
				maxFlowPathMap.put(endVertex, vPath);
			}
		}

		
		return maxFlowPathMap;
	}
	
	private int[] obtainOnePath(int vid , int tid) {
		Stack<Integer> stack = new Stack<Integer>();
		Map<Integer , Set<Integer> > visitMap = new HashMap<Integer , Set<Integer>>();

		stack.push(vid);		
		int requriedLen = queryMPath.vertex.length * 2 - 1;
		for (int i = 0 ; i < requriedLen ; i++) {
			Set<Integer> newSet = new HashSet<Integer>();
			visitMap.put(i, newSet);
		}
		visitMap.get(0).add(vid);

		while (!stack.isEmpty()) {
			boolean stackPush = false;
			for(int v : flowGraphMap.get(stack.peek())) {
				if ( stack.size() < requriedLen) {
					if (!visitMap.get(stack.size() ).contains(v)) {
						stackPush = true;
						visitMap.get(stack.size()).add(v);
						stack.push(v);
						break;
					}
				}
			}
			if(stack.size() == requriedLen && stack.peek() == tid) {
				break;
			}
			if(!stackPush) {
				stack.pop();
			}
		}
		if (stack.isEmpty()) {//there is no meta-path
			return null;
		}
		//delete this meta-path from the flow network
		for(int i = 0 ; i < stack.size() - 1 ; i++) {
			flowGraphMap.get(stack.get(i)).remove(stack.get(i+1));
		}
		int[] result = new int[queryMPath.vertex.length];
		for (int i = 0 ; i < result.length ; i++) {
			result[i] = stack.get(2 * i ) % graph.length;
		}
		return result;
	}
	
	//find a path from s to t in the flow network
	private int augOnePathNeighbor(int s, int t){
		Set<Integer> visitSet = new HashSet<Integer>();
		Stack<Integer> stack = new Stack<Integer>();
		stack.push(s);
		visitSet.add(s);
		while(!stack.isEmpty()) {
			boolean stackPush = false;
			for(int v : flowGraphMap.get(stack.peek())) {
				if (!visitSet.contains(v)) {
					stackPush = true;
					visitSet.add(v);
					stack.push(v);
					break;
				}
			}
			if(stack.peek() == t) {
				break;
			}
			if(!stackPush) {
				stack.pop();
			}
		}
		if(stack.isEmpty()) {
			return -1;//there is no path
		}
		
		//update the directions of edges in the flow network
		for(int i = 0 ; i < stack.size() - 1; i++) {
			flowGraphMap.get(stack.get(i)).remove(stack.get(i+1));
			flowGraphMap.get(stack.get(i+1)).add(stack.get(i));
		}
		int result = stack.get(stack.size() - 2) >= graph.length ? stack.get(stack.size() - 2) - graph.length : stack.get(stack.size() - 2);
		return result;
	}
	
	private void collectVertices(int vertexId, Set<Integer> keepSet) {
		//step 1: collect vertices from left to right
		vertexList = new ArrayList<Set<Integer>>();
		Set<Integer> v0Set = new HashSet<Integer>();
		v0Set.add(vertexId);
		vertexList.add(v0Set);
		for(int i = 0; i < queryMPath.pathLen; i++) {
			Set<Integer> curSet = new HashSet<Integer>();
			for(int vid: vertexList.get(i) ) {
				for(int k = 0; k < graph[vid].length; k = k + 2) {
					int tmpVId = graph[vid][k], tmpEId = graph[vid][k + 1];
					if (vertexType[tmpVId] == queryMPath.vertex[i + 1] 
							&& edgeType[tmpEId] == queryMPath.edge[i]) {
						if(i < queryMPath.pathLen - 1) {
							curSet.add(tmpVId);
						}else {
							if(keepSet.contains(tmpVId)) {
								curSet.add(tmpVId);
							}
						}
					}
				}
			}
			vertexList.add(curSet);
		}
		vertexList.get(queryMPath.pathLen).remove(vertexId);//the source node and sink node are different
		
		//step 2: collect vertices from right to left
		for (int i = queryMPath.pathLen; i > 0; i--) {
			Set<Integer> newSet = new HashSet<Integer>();
			for (int vid: vertexList.get(i)) {
				for (int k = 0; k < graph[vid].length; k = k + 2) {
					if (vertexList.get(i - 1).contains(graph[vid][k])) {
						newSet.add(graph[vid][k]);
					}
				}
			}
			vertexList.set(i - 1 , newSet);
		}
	}
	
	private void createFlowGraph(int vertexId ,Set<Integer> keepSet , Map<Integer, int[]> pathMap) {
		//step 1: collect vertices from left -> right and right -> left
		collectVertices(vertexId, keepSet);
		
		//step 2: create the flow network for the first (pathLen -1)-th layer
		flowGraphMap = new HashMap<Integer, Set<Integer>>();
		int NUM = graph.length;
		
		HashSet<Integer> firstNeiborSet = new HashSet<Integer>();
		for(int nid = 0 ; nid < graph[vertexId].length; nid += 2) {
			if(vertexList.get(1).contains(graph[vertexId][nid])) {
				firstNeiborSet.add(graph[vertexId][nid] );
			}
		}
		flowGraphMap.put(vertexId, firstNeiborSet);
		
		for(int i = 1; i < vertexList.size() - 1; i++) {
			for(int v : vertexList.get(i)) {
				HashSet<Integer> neiborSet = new HashSet<Integer>();
				Set<Integer> tmpSet = vertexList.get(i + 1);
				for(int nid = 0 ; nid < graph[v].length; nid += 2) {
					if(tmpSet.contains(graph[v][nid])) {
						if (flowGraphMap.containsKey(graph[v][nid])) {
							neiborSet.add(graph[v][nid] + NUM );
						} else {
							neiborSet.add(graph[v][nid] );
						}
					}
				}
				if(flowGraphMap.containsKey(v)) {
					int inVid = v + NUM;
					int outVid = inVid + 2 * NUM;
					Set<Integer> inSet = new HashSet<Integer>();
					inSet.add(outVid);
					flowGraphMap.put(inVid, inSet);
					flowGraphMap.put(outVid, neiborSet);
				} else {
					int inVid = v;
					int outVid = inVid + 2 * NUM;
					Set<Integer> inSet = new HashSet<Integer>();
					inSet.add(outVid);
					flowGraphMap.put(inVid, inSet);
					flowGraphMap.put(outVid, neiborSet);
				}	
			}
		}
		
		//step 3: create the flow network for the last pathLen-th layer
		for(int v : vertexList.get(vertexList.size() - 1)) {
			HashSet<Integer> neiborSet = new HashSet<Integer>();
			neiborSet.add(-1);//We use -1 to denote the terminate node
			if(flowGraphMap.containsKey(v)) {
				int inVid = v + NUM;
				int outVid = inVid + 2 * NUM;
				Set<Integer> inSet = new HashSet<Integer>();
				inSet.add(outVid);
				flowGraphMap.put(inVid, inSet);
				flowGraphMap.put(outVid, neiborSet);
			}else {
				int inVid = v;
				int outVid = inVid + 2 * NUM;
				Set<Integer> inSet = new HashSet<Integer>();
				inSet.add(outVid);
				flowGraphMap.put(inVid, inSet);
				flowGraphMap.put(outVid, neiborSet);
			}	
		}
		//HashSet<Integer>[] newSet = new HashSet<Integer>[]();
		//step 4: create the sink node
		Set<Integer> neiborSet = new HashSet<Integer>();
		flowGraphMap.put(-1, neiborSet);
		
		//step 5: add inverse Paths
		if (pathMap != null) {
			Set<int[]> newIdPathSet = getNewIdPathSet(vertexId , pathMap , NUM );
			for (int[] newIdPath : newIdPathSet) {
				for (int i = 1 ; i < newIdPath.length ; i++) {
					flowGraphMap.get(newIdPath[i - 1]).remove(newIdPath[i]);
					flowGraphMap.get(newIdPath[i]).add(newIdPath[i - 1]);
				}
			}
		}
	}

	//transfer the VertexPath to newID
	private Set<int[]> getNewIdPathSet (int vertexId , Map<Integer , int[]> pathsMap , int NUM ) {
		Set<int[]> newIdPathSet = new HashSet<int[]>();
		for (int vid : pathsMap.keySet()) {
			int[] vPath = pathsMap.get(vid);
			int[] newIdPath = getNewIdPath(vPath , NUM);
			newIdPathSet.add(newIdPath);
		}
		return newIdPathSet;
	}

	private int[] getNewIdPath(int[] vPath , int NUM) {
		int[] newIdPath = new int[vPath.length * 2];
		newIdPath[0] = vPath[0];
		newIdPath[1] = vPath[1];
		newIdPath[2] = vPath[1] + 2 * NUM;
		for (int i = 2 ; i < vPath.length ; i++) {
			if (flowGraphMap.get(newIdPath[2 * i - 2]).contains(vPath[i])) {
				newIdPath[2 * i - 1] = vPath[i];
				newIdPath[2 * i] = vPath[i] + 2 * NUM;
			} else if (flowGraphMap.get(newIdPath[2 * i - 2]).contains(vPath[i] + NUM)) {
				newIdPath[2 * i - 1] = vPath[i] + NUM;
				newIdPath[2 * i] = vPath[i] + 3 * NUM;
			}
		}
		newIdPath[newIdPath.length - 1] = -1;
		return newIdPath;
	}
}