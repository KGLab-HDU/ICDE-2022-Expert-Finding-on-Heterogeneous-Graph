package unsw.index.basic;

import java.util.HashMap;
import java.util.Map;

import unsw.index.Decomposition;
import unsw.online.MetaPath;

/**
 * @author fangyixiang
 * @date Oct 15, 2018
 * Perform basic k-core decomposition
 */
public class BCoreDecomposition implements Decomposition{
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	
	public BCoreDecomposition(int graph[][], int vertexType[], int edgeType[]) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
		
	}
	
	private int reverseOrderArr[] = null;
	public Map<Integer, Integer> decompose(MetaPath queryMPath){
		//step 0: build a homogeneous graph
		HomoGraphBuilder b2 = new HomoGraphBuilder(graph, vertexType, edgeType, queryMPath);
		Map<Integer, int[]> pnbMap = b2.build();
		
		int newID = 1;
		Map<Integer, Integer> oldToNewMap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> newToOldMap = new HashMap<Integer, Integer>();
		for(int id:pnbMap.keySet()) {
			oldToNewMap.put(id, newID);//oldID -> newID
			newToOldMap.put(newID, id);//newID -> oldID
			newID ++;
		}
		
		//step 1: build a sub-graph
		int subGraph[][] = new int[pnbMap.size() + 1][];
		for(int id:pnbMap.keySet()) {
			int pnbArr[] = pnbMap.get(id);
			
			int curID = oldToNewMap.get(id);
			subGraph[curID] = new int[pnbArr.length];
			for(int j = 0;j < pnbArr.length;j ++) {
				int nbID = oldToNewMap.get(pnbArr[j]);
				subGraph[curID][j] = nbID;
			}
		}
		pnbMap = null;
		
//		//test codes
//		double memory = subGraph.length;
//		for(int i = 1;i < subGraph.length;i ++){
//			memory += subGraph[i].length;
//		}
//		if(memory > 20000000){
//			System.out.println("------------------------------------------> " + queryMPath.toString() + " " + memory);
//		}
		
		//step 2: kcore decomposition
		KCore kc = new KCore(subGraph);
		int subCore[] = kc.decompose();
		reverseOrderArr = kc.obtainReverseCoreArr();
		for(int i = 0;i < reverseOrderArr.length;i ++) {
			int tmpNewID = reverseOrderArr[i];
			int tmpOldID = newToOldMap.get(tmpNewID);
			reverseOrderArr[i] = tmpOldID;
		}
		
		//step 3: attach the core number
		Map<Integer, Integer> vertexCoreMap = new HashMap<Integer, Integer>();
		for(int i = 1;i < subCore.length;i ++) {
			int oldId = newToOldMap.get(i);
			int core = subCore[i];
			vertexCoreMap.put(oldId, core);
		}
		
		return vertexCoreMap;
	}
	
	public int[] getReverseOrderArr() {
		return reverseOrderArr;
	}
}
