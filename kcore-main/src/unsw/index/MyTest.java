package unsw.index;

import unsw.Config;
import unsw.DataReader;
import unsw.index.basic.BCoreDecomposition;
import unsw.online.MetaPath;
import unsw.online.basic.FastBCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author fangyixiang
 * @date 27 Sep. 2018
 */
public class MyTest {

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

        List<MetaPath> list = new ArrayList<>();
        list.add(metaPath1);
        Decomposition bcd = new BCoreDecomposition(graph, vertexType, edgeType);
        PathIndex pathIndex = new PathIndex(graph, vertexType, edgeType, list);
        int index[][][] = pathIndex.build(bcd);
        Map<String, Integer> pathLocMap = pathIndex.getPathLocMap();

        PathIndexQuery query = new PathIndexQuery(vertexType, index, pathLocMap);
        int queryK = 8; //k = 1, 375441; k = 2 371166
//        System.out.println("Length : " + graph.length);

        for (int i = 0; i < graph.length; i++) {
            if (vertexType[i] == 0) {
                int queryId = i;
                Set<Integer> rsSet1 = query.query(queryId, metaPath1, queryK);
                if (rsSet1 == null) {
                    System.out.println("queryId : " + queryId + " : All are empty results");
                } else {
                    System.out.println("queryId : " + queryId + " : |C1|=" + rsSet1.size());
                }
            }
        }
    }
}
