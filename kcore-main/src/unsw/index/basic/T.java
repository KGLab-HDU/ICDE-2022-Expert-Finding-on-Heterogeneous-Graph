package unsw.index.basic;

import java.util.Map;

import unsw.Config;
import unsw.DataReader;
import unsw.online.MetaPath;

/**
 * @author fangyixiang
 * @date 27 Sep. 2018
 */
public class T {

	public static void main(String[] args) {
		DataReader dataReader = new DataReader(Config.dblpGraph, Config.dblpVertex, Config.dblpEdge);
		int graph[][] = dataReader.readGraph();
		int vertexType[] = dataReader.readVertexType();
		int edgeType[] = dataReader.readEdgeType();

		int queryId = 601;
		int queryK = 10;
		int vertex[] = {1, 0, 1}, edge[] = {3, 0};
//		int vertex[] = {1, 0, 2, 0, 1}, edge[] = {3, 1, 4, 0};
//		int vertex[] = {2, 0, 1, 0, 2}, edge[] = {4, 0, 3, 1};
		MetaPath queryMPath = new MetaPath(vertex, edge);

		long time1 = System.nanoTime();
		BCoreDecomposition bcd = new BCoreDecomposition(graph, vertexType, edgeType);
		Map<Integer, Integer> map = bcd.decompose(queryMPath);
		long time2 = System.nanoTime();
		System.out.println(time2 - time1);

		int count5 = 0, max = 0;
		for(Map.Entry<Integer, Integer> entry:map.entrySet()) {
			if(entry.getValue() >= 5) {
				count5 ++;
			}
			if(entry.getValue() >= max) {
				max = entry.getValue();
			}
		}
		System.out.println("count5=" + count5 + "\n max=" + max);
	}

}
