package unsw.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import unsw.Config;
import unsw.DataReader;
import unsw.index.basic.BCoreDecomposition;
import unsw.online.MetaPath;
import unsw.online.basic.FastBCore;

/**
 * @author fangyixiang
 * @date 27 Sep. 2018
 */
public class Test {

    public static void main(String[] args) {
//		DataReader dataReader = new DataReader(Config.smallDBLPGraph, Config.smallDBLPVertex, Config.smallDBLPEdge);
		DataReader dataReader = new DataReader(Config.dblpGraph, Config.dblpVertex, Config.dblpEdge);
//        DataReader dataReader = new DataReader(Config.FsqGraph, Config.FsqVertex, Config.FsqEdge);
        int graph[][] = dataReader.readGraph();
        int vertexType[] = dataReader.readVertexType();
        int edgeType[] = dataReader.readEdgeType();

        int vertex1[] = {0, 1, 0}, edge1[] = {0, 3};
        MetaPath metaPath1 = new MetaPath(vertex1, edge1);
//        int vertex2[] = {1, 0, 2, 0, 1}, edge2[] = {3, 1, 4, 0};
        int vertex2[] = {2, 0, 2, 0, 1}, edge2[] = {3, 1, 4, 0};
        MetaPath metaPath2 = new MetaPath(vertex2, edge2);

        List<MetaPath> list = new ArrayList<>();
        list.add(metaPath1);
        list.add(metaPath2);
        Decomposition bcd = new BCoreDecomposition(graph, vertexType, edgeType);
        PathIndex pathIndex = new PathIndex(graph, vertexType, edgeType, list);
        int index[][][] = pathIndex.build(bcd);
        Map<String, Integer> pathLocMap = pathIndex.getPathLocMap();

        PathIndexQuery query = new PathIndexQuery(vertexType, index, pathLocMap);
        int queryK = 1;

        for (int i = 0; i < graph.length; i++) {
            if (vertexType[i] == 0) {
                int queryId = i;
                Set<Integer> rsSet1 = query.query(queryId, metaPath1, queryK);

                FastBCore quickCore = new FastBCore(graph, vertexType, edgeType);
                Set<Integer> rsSet2 = quickCore.query(queryId, metaPath2, queryK);

                if (rsSet1 == null && rsSet2 == null) {
                    System.out.println("All are empty results");
                } else if (rsSet1.size() == rsSet2.size()) {
                    System.out.println("OK |C1|=" + rsSet1.size() + "   |C2|=" + rsSet2.size());
                } else {
                    System.out.println("NO |C1|=" + rsSet1.size() + "   |C2|=" + rsSet2.size());
                    for (int id : rsSet1) {
                        if (rsSet2.contains(id) == false) {
                            System.out.println("vertex-" + id + " is in rsSet1, but not in rsSet2");
                        }
                    }
                    System.exit(0);
                }
            }
        }
    }

}
