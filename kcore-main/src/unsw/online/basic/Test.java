package unsw.online.basic;

import java.util.Set;

import unsw.Config;
import unsw.DataReader;
import unsw.online.MetaPath;
import unsw.online.basic.HomBCore;
import unsw.online.basic.FastBCore;

/**
 * @author fangyixiang
 * @date 18 Sep. 2018
 */
public class Test {

	public static void main(String[] args) {
//		DataReader dataReader = new DataReader(Config.smallDBLPGraph, Config.smallDBLPVertex, Config.smallDBLPEdge);
		DataReader dataReader = new DataReader(Config.dblpGraph, Config.dblpVertex, Config.dblpEdge);
		int graph[][] = dataReader.readGraph();
		int vertexType[] = dataReader.readVertexType();
		int edgeType[] = dataReader.readEdgeType();
		
		int queryK = 5;
		int vertex[] = {1, 0, 1}, edge[] = {3, 0};
//		int vertex[] = {1, 0, 2, 0, 1}, edge[] = {3, 1, 4, 0};
		MetaPath queryMPath = new MetaPath(vertex, edge);
		
		int count = 0;
		long time1 = 0, time2 = 0;
		for(int i = 0;i < graph.length;i ++) {
			if(vertexType[i] == 1) {
				int queryId = 111509;
				System.out.println("queryId=" + queryId + " queryK=" + queryK);
				
				long t1 = System.nanoTime();
				HomBCore prunePath = new HomBCore(graph, vertexType, edgeType);
				Set<Integer> rsSet1 = prunePath.query(queryId, queryMPath, queryK);
//				Set<Integer> rsSet1 = null;
				long t2 = System.nanoTime();
				if(rsSet1 != null)  System.out.println("|rsSet1|=" + rsSet1.size() + " time:" + (t2 - t1));
				else System.out.println("rsSet1 is empty");
				
				FastBCore quickCore = new FastBCore(graph, vertexType, edgeType);
				Set<Integer> rsSet2 = quickCore.query(queryId, queryMPath, queryK);
				long t3 = System.nanoTime();
				if(rsSet2 != null)  System.out.println("|rsSet2|=" + rsSet2.size()  + " time:" + (t3 - t2));
				else System.out.println("rsSet2 is empty");
				
				if(rsSet1 == null && rsSet2 == null) {
					System.out.println("All are empty results");
				}else if(rsSet1.size() == rsSet2.size()) {
					System.out.println("OK |C1|=" + rsSet1.size() + "   |C2|=" + rsSet2.size());
				}else{
					System.out.println("NO |C1|=" + rsSet1.size() + "   |C2|=" + rsSet2.size());
					for(int id:rsSet1) {
						if(rsSet2.contains(id) == false) {
							System.out.println("vertex-" + id + " is in rsSet1, but not in rsSet2");
						}
					}
					System.exit(0);
				}
				
				if(rsSet1 != null && rsSet1.size() > 0) {
					count ++;
					time1 += t2 - t1;
					time2 += t3 - t2;
					if(count % 10 == 0) System.out.println("queryId=" + queryId + "\ntime1=" + (t2 - t1) / count + "\ntime2=" + (t3 - t2) / count + "\n");
				}
				
				break;
			}
		}
		System.out.println("Finished " + count + " queries.\ntime1=" + time1 + "\ntime2=" + time2);
 	}

}
/*
Finished 100 queries.
time1=5098921438619
time2=295729466216

queryId=111509 queryK=10
rsSet1 is empty
|rsSet2|=132980 time:1915050469
*/