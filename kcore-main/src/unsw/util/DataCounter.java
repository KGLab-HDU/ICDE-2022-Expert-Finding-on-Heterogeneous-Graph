package unsw.util;

import java.util.HashMap;
import java.util.Map;

import unsw.Config;
import unsw.DataReader;

/**
 * @author fangyixiang
 * @date Sep 17, 2018
 * count the number of entries for each type
 */
public class DataCounter {

	public static void main(String[] args) {
//		DataReader dataReader = new DataReader(Config.dblpGraph, Config.dblpVertex, Config.dblpEdge);
//		DataReader dataReader = new DataReader(Config.IMDBGraph, Config.IMDBVertex, Config.IMDBEdge);
//		DataReader dataReader = new DataReader(Config.FsqGraph, Config.FsqVertex, Config.FsqEdge);
		DataReader dataReader = new DataReader(Config.dbpediaGraph, Config.dbpediaVertex, Config.dbpediaEdge);
		
		int graph[][] = dataReader.readGraph();
		int vertexType[] = dataReader.readVertexType();
		int edgeType[] = dataReader.readEdgeType();
		
		int vNum = graph.length;
		int eNum = 0;
		for(int i = 0;i < graph.length;i ++) {
			eNum += graph[i].length / 2;
		}
		System.out.println("vertex-number: " + vNum
				+ "\nedge-number: " + eNum / 2);
		
		Map<Integer, Integer> vMap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> eMap = new HashMap<Integer, Integer>();
		
		for(int i = 0;i < vertexType.length;i ++) {
			int type = vertexType[i];
			if(vMap.containsKey(type)) {
				int count = vMap.get(type);
				vMap.put(type, count + 1);
			}else {
				vMap.put(type, 1);
			}
		}
		
		for(int i = 0;i < edgeType.length;i ++) {
			int type = edgeType[i];
			if(eMap.containsKey(type)) {
				int count = eMap.get(type);
				eMap.put(type, count + 1);
			}else {
				eMap.put(type, 1);
			}
		}
		
		System.out.println("total vertex-type: " + vMap.size());
		System.out.println("total edge-type: " + eMap.size());
		
//		int emptyVType = 0, emtpyEType = 0;
		for(int i = 0;i < 100000000;i ++) {
			if(vMap.containsKey(i)) {
				System.out.println("vertex-type:" + i + "   count:" + vMap.get(i));
			}else {
				break;
			}
		}
		
		for(int i = 0;i < 100000000;i ++) {
			if(eMap.containsKey(i)) {
				System.out.println("edge-type:" + i + "   count:" + eMap.get(i));
			}else {
				break;
			}
		}
	}

}
/*
C:\Users\fangyixiang\Desktop\HIN\dataset\DBLP\graph.txt |V|=6902915 |E|=44883574
vertex-number: 6902915
edge-number: 89767148
vertex-type:0   count:4297514
vertex-type:1   count:2150185
vertex-type:2   count:69915
vertex-type:3   count:385301
edge-type:0   count:12575783
edge-type:1   count:4215503
edge-type:2   count:28092288
edge-type:3   count:12575783
edge-type:4   count:4215503
edge-type:5   count:28092288

C:\Users\fangyixiang\Desktop\HIN\dataset\FourSquare\graph.txt |V|=187223 |E|=2093454
vertex-number: 187223
edge-number: 2093454
vertex-type:0   count:165069
vertex-type:1   count:32
vertex-type:2   count:425
vertex-type:3   count:21242
vertex-type:4   count:455
edge-type:0   count:165069
edge-type:1   count:165069
edge-type:2   count:165069
edge-type:3   count:165069
edge-type:4   count:881658
edge-type:5   count:881658
edge-type:6   count:881658
edge-type:7   count:881658

C:\Users\fangyixiang\Desktop\HIN\dataset\DBPedia\graph.txt |V|=5900558 |E|=17961887
vertex-number: 5900558
edge-number: 17961887
total vertex-type: 413
total edge-type: 1274
*/