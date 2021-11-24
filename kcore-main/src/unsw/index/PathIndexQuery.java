package unsw.index;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import unsw.online.MetaPath;

/**
 * @author fangyixiang
 * @date 12 Sep. 2018
 * Query algorithm based on the PathIndex
 */
public class PathIndexQuery {
    /**
     * the vertex type
     */
    private int vertexType[] = null;
    /**
     * the index structure
     */
    private int index[][][] = null;
    private Map<String, Integer> pathLocMap = null;//a meta-path string -> the location of dimension in the array

    public PathIndexQuery(int vertexType[], int index[][][], Map<String, Integer> pathLocMap) {
        this.vertexType = vertexType;
        this.index = index;
        this.pathLocMap = pathLocMap;
    }

    public Set<Integer> query(int queryId, MetaPath queryMPath, int queryK) {
        if (vertexType[queryId] != queryMPath.vertex[0])
            return null;//the types of queryId and queryMPath are not matched
        if (!pathLocMap.containsKey(queryMPath.toString())) return null;//the meta-path is not considered in the index

        int pathId = pathLocMap.get(queryMPath.toString());
        if (index[queryId][pathId][0] < queryK) return null;//the core number is less than k

        Set<Integer> community = new HashSet<Integer>();//vertices which have been put into queue
        Queue<Integer> ccQueue = new LinkedList<Integer>();
        ccQueue.add(queryId);
        community.add(queryId);
        while (ccQueue.size() > 0) {
            int curId = ccQueue.poll();
            int nbArr[] = index[curId][pathId];
            for (int i = 1; i < nbArr.length; i++) {
                int nbID = nbArr[i];
                if (index[nbID][pathId][0] >= queryK) {//nbID's core number must be at least k
                    if (!community.contains(nbID)) {
                        ccQueue.add(nbID);
                        community.add(nbID);
                    }
                }
            }
        }
        return community;
    }
}
