package unsw.index.edgeDisjoint;

import java.util.ArrayList;
import java.util.List;
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
//		DataReader dataReader = new DataReader(Config.smallDBLPGraph, Config.smallDBLPVertex, Config.smallDBLPEdge);
//		String graphFile = "C:\\Users\\fangyixiang\\Desktop\\HIN\\dataset\\newDBLP\\graph.txt";
//		String vertexFile = "C:\\Users\\fangyixiang\\Desktop\\HIN\\dataset\\newDBLP\\vertex.txt";
//		String edgeFile = "C:\\Users\\fangyixiang\\Desktop\\HIN\\dataset\\newDBLP\\edge.txt";
//		DataReader dataReader = new DataReader(graphFile, vertexFile, edgeFile);
		int graph[][] = dataReader.readGraph();
		int vertexType[] = dataReader.readVertexType();
		int edgeType[] = dataReader.readEdgeType();

		List<MetaPath> list = new ArrayList<MetaPath>();
		list.add(new MetaPath("0 0 1 3 0"));
		list.add(new MetaPath("1 3 0 0 1"));
		list.add(new MetaPath("0 2 3 5 0"));
		list.add(new MetaPath("3 5 0 2 3"));
		list.add(new MetaPath("0 1 2 4 0"));
		list.add(new MetaPath("2 4 0 1 2"));

		for(MetaPath p:list) {
			GreedyECoreDecomposition greedy = new GreedyECoreDecomposition(graph, vertexType, edgeType);
			Map<Integer, Integer> map1 = greedy.decompose(p);

			System.out.println("|map1|=" + map1.size());
			int maxK1  = 0;
			for(Map.Entry<Integer, Integer> entry:map1.entrySet()) {
				if(entry.getValue() > maxK1) {
					maxK1 = entry.getValue();
				}
			}

			System.out.println(p.toString() + "->" + maxK1);
		}

	}

}
/*
APVPA k = 5
time1: 1941360552755
time2: 469740031876
*/
