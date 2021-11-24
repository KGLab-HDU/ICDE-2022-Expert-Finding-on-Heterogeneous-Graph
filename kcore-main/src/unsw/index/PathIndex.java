package unsw.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import unsw.index.UF.UNode;
import unsw.index.UF.UnionFind;
import unsw.online.BatchSearch;
import unsw.online.MetaPath;

/**
 * @author fangyixiang
 * @date 11 Sep. 2018
 * Build the PathIndex (assume that each vertex has only one type)
 */
public class PathIndex {
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	private List<MetaPath> pathList = null;//a list of meta-paths
	private Map<Integer, List<MetaPath>> pathMap = null;//type of first vertices -> meta-path list
	
	private int index[][][] = null;//the simplified graph structure
	private Map<String, Integer> pathLocMap = null;//a meta-path string -> the location in the array
	
	public PathIndex(int graph[][], int vertexType[], int edgeType[], List<MetaPath> pathList) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
		this.pathList = pathList;
		
		this.index = new int[graph.length][][];
		this.pathLocMap = new HashMap<String, Integer>();
	}
	
	public int[][][] build(Decomposition decomposition) {
		//step 1: group paths according to the types of their first vertices
		this.pathMap = new HashMap<Integer, List<MetaPath>>();
		for(MetaPath path:pathList) {
			int firstType = path.vertex[0];
			if(pathMap.containsKey(firstType)) {
				pathMap.get(firstType).add(path);
			}else {
				List<MetaPath> list = new ArrayList<MetaPath>();
				list.add(path);
				pathMap.put(firstType, list);
			}
		}
		
	
		//step 1.1 initialize index array
//		long count = 0l;
		for(int i = 0;i < graph.length;i ++) {
			int type = vertexType[i];
			List<MetaPath> list = pathMap.get(type);
			if(list != null) {
				int typeSize = pathMap.get(type).size();
				index[i] = new int[typeSize][];
//				count += index[i].length;
			}
			
		}
//		System.out.println("total number: " + count);
		
		//step 2: process meta-paths one by one
		int allPathCounter = 0;
		for(Map.Entry<Integer, List<MetaPath>> entry:pathMap.entrySet()) {
			List<MetaPath> pathList = entry.getValue();
			
			//pathIdx is starting from 0 (the 0-th cell is occupied by the core number)
			for(int pathId = 0;pathId < pathList.size();pathId ++) {
				MetaPath queryMPath = pathList.get(pathId);
//				System.out.println((allPathCounter ++) + ": " + queryMPath.toString());//Yixiang: count the number of paths and show the paths
				pathLocMap.put(queryMPath.toString(), pathId);
				
				//step 2.1: perform kcore decomposition
				Map<Integer, Integer> vertexCoreMap = decomposition.decompose(queryMPath);
				int reverseOrder[] = decomposition.getReverseOrderArr();
				
				//step 2.2: build a simplified graph
				Map<Integer, Set<Integer>> coreGraphMap = buildCoreGraph(vertexCoreMap, reverseOrder, queryMPath);
								
				//step 2.3: build the index
				for(int curId:coreGraphMap.keySet()) {
					Set<Integer> nbSet = coreGraphMap.get(curId);
					index[curId][pathId] = new int[1 + nbSet.size()];
					index[curId][pathId][0] = vertexCoreMap.get(curId);//core number
					Iterator<Integer> iter = nbSet.iterator();
					int i = 1;
					while(iter.hasNext()) {
						int nbID = iter.next();
						index[curId][pathId][i ++] = nbID;
					}
				}
			}
		}
		
		return index;
	}

	private Map<Integer, Set<Integer>> buildCoreGraph(Map<Integer, Integer> vertexCoreMap, int reverseOrder[], MetaPath queryMPath) {
		//step 1: initialize the simplified graph
		Map<Integer, Set<Integer>> coreGraphMap = new HashMap<Integer, Set<Integer>>();
		for(int id:vertexCoreMap.keySet()) {
			coreGraphMap.put(id, new HashSet<Integer>());
		}
		
		//step 2: initialize the union-find data structure
		UnionFind unionFind = new UnionFind();
		Map<Integer, UNode> ufMap = new HashMap<Integer, UNode>();
		
		//step 3: consider vertices from large-core-numbers to small-core-numbers
		Set<Integer> keepSet = new HashSet<Integer>();
		BatchSearch affVertexFinder = new BatchSearch(graph, vertexType, edgeType, queryMPath);
		for(int curId:reverseOrder) {
			//step a: find all the neighbors with core numbers at least core[id]
			Set<Integer> nbSet = affVertexFinder.collect(curId, keepSet);
			
			//step b: create a union-finder node
			UNode curIdNode = new UNode(curId);
			unionFind.makeSet(curIdNode);
			ufMap.put(curId, curIdNode);
			
			//step c: consider neighbors one by one
			boolean isFirst = true;
			for(int nbId:nbSet) {
				UNode nbIdNode = ufMap.get(nbId);
				if(isFirst) {
					isFirst = false;
					
					//link these two nodes
					unionFind.union(curIdNode, nbIdNode);
					coreGraphMap.get(curId).add(nbId);
					coreGraphMap.get(nbId).add(curId);
				}else {
					UNode curIdParentNode = unionFind.find(curIdNode);
					UNode nbIdParentNode = unionFind.find(nbIdNode);
					
					if(curIdParentNode != nbIdParentNode) {
						//link these two nodes
						unionFind.union(curIdNode, nbIdNode);
						coreGraphMap.get(curId).add(nbId);
						coreGraphMap.get(nbId).add(curId);
					}
				}
			}
			
			//step d: update the keepSet
			keepSet.add(curId);
		}
		
		return coreGraphMap;
	}

	public Map<String, Integer> getPathLocMap() {
		return pathLocMap;
	}
}
