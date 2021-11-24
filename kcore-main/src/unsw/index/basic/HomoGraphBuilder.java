package unsw.index.basic;

import java.util.*;

import unsw.online.BatchSearch;
import unsw.online.MetaPath;

/**
 * @author fangyixiang
 * @date 15 Oct. 2018
 * build a homogeneous graph
 */
public class HomoGraphBuilder {
	private int graph[][] = null;//data graph, including vertice IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	private MetaPath queryMPath = null;//the query meta-path
	
	public HomoGraphBuilder(int graph[][], int vertexType[], int edgeType[], MetaPath queryMPath) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
		this.queryMPath = queryMPath;
	}
	
	public Map<Integer, int[]> build(){
		//step 1: collect vertices of the same type
		int STARTTYPE = queryMPath.vertex[0];
		Set<Integer> keepSet = new HashSet<Integer>();
		for(int i = 0;i < vertexType.length;i ++) {
			if(vertexType[i] == STARTTYPE) {
				keepSet.add(i);
			}
		}
		
		//step 2: find neighbors
		BatchSearch affVertexFinder = new BatchSearch(graph, vertexType, edgeType, queryMPath);
		Map<Integer, int[]> pnbMap = new HashMap<Integer, int[]>();
		for(int startId:keepSet) {
			Set<Integer> nbSet = affVertexFinder.collect(startId, keepSet);
			int nbArr[] = new int[nbSet.size()];
			int i = 0;
			for(int nbId:nbSet) {
				nbArr[i] = nbId;
				i ++;
			}
			pnbMap.put(startId, nbArr);
		}
		
		return pnbMap;
	}
	
}
