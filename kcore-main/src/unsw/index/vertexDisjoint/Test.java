package unsw.index.vertexDisjoint;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;

import unsw.Config;
import unsw.DataReader;
import unsw.index.edgeDisjoint.GreedyECoreDecomposition;
import unsw.index.edgeDisjoint.LazyECoreDecomposition;
import unsw.online.MetaPath;

/**
 * @author fangyixiang
 * @date 9 Oct. 2018
 */
public class Test {

	public static void main(String[] args) {
		DataReader dataReader = new DataReader(Config.dblpGraph, Config.dblpVertex, Config.dblpEdge);
		int graph[][] = dataReader.readGraph();
		int vertexType[] = dataReader.readVertexType();
		int edgeType[] = dataReader.readEdgeType();
		
		int vertex[] = {1, 0, 1}, edge[] = {3, 0};
//		int vertex[] = {1, 0, 2, 0, 1}, edge[] = {3, 1, 4, 0};
		MetaPath queryMPath = new MetaPath(vertex, edge);
		
		long time1 = 0, time2 = 0, count = 0;
		
		long t0 = System.nanoTime();
//		LazyVCoreDecomposition dvc = new LazyVCoreDecomposition(graph, vertexType, edgeType);
//		Map<Integer, Integer> map1 = dvc.decompose(queryMPath);
		long t1 = System.nanoTime();

		GreedyVCoreDecomposition greedy = new GreedyVCoreDecomposition(graph, vertexType, edgeType);
		Map<Integer, Integer> map2 = greedy.decompose(queryMPath);
		long t2 = System.nanoTime();
		
//		System.out.println("Lazy: " + (t1 - t0)/1000000 + "\nGreedy: " + (t2 - t1)/1000000);
//		for(int id:map1.keySet()) {
//			int core1 = map1.get(id);
//			int core2 = map2.get(id);
//			if(core1 != core2) {
//				System.out.println("wrong!!!\ncore1[" + id + "]=" + core1 + "\ncore2[" + id + "]=" + core2);
//			}
//		}
	}

}
