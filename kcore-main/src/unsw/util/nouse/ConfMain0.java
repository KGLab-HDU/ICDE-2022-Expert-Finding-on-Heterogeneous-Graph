package unsw.util.nouse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import unsw.Config;
import unsw.DataReader;
//import unsw.exp.groundTruth.ClassReader;
//import unsw.exp.groundTruth.Precision;
//import unsw.exp.groundTruth.Recall;
import unsw.online.MetaPath;
import unsw.online.basic.FastBCore;
import unsw.online.edgeDisjoint.BatchECore;
import unsw.online.vertexDisjoint.BatchVCore;

/**
 * @author fangyixiang
 * @date 11 Jun. 2019
 */
public class ConfMain0 {

	public static void main(String[] args) {
		String graphDataSetPath = Config.root + "\\HIN\\dataset\\";
		String dataSetName = "CaseStudyDBLP";
		String graphPath = graphDataSetPath + dataSetName + "/graph.txt";
		String vertexPath = graphDataSetPath + dataSetName + "/vertex.txt";
		String edgePath = graphDataSetPath + dataSetName + "/edge.txt";
		String classPath = graphDataSetPath + dataSetName + "/class.txt";

		//step 1: read data and class labels
		DataReader dataReader = new DataReader(graphPath, vertexPath, edgePath);
		int graph[][] = dataReader.readGraph();
		int vertexType[] = dataReader.readVertexType();
		int edgeType[] = dataReader.readEdgeType();
		ClassReader classReader = new ClassReader(classPath);
		Map<Integer, Integer> classMap = classReader.read();

		//step 2: ground-truth communities
		List<Set<Integer>> groundList = new ArrayList<Set<Integer>>();
		for(int i = 0;i < 4;i ++) groundList.add(new HashSet<Integer>());
		for(int i = 0;i < vertexType.length;i ++) {
			if(vertexType[i] == 1) {//focus on authors
				int classId = classMap.get(i);
				groundList.get(classId).add(i);
			}
		}
		Map<Integer, Set<Integer>> groundMap = new HashMap<Integer, Set<Integer>>();
		for(int i = 0;i < groundList.size();i ++) {
			Set<Integer> set = groundList.get(i);
			for(int id:set) {
				groundMap.put(id, set);//vertex -> community
			}
		}

		//step 3: perform queries
		MetaPath queryMPath = new MetaPath("2 4 0 0 1 3 0 1 2");//vpapv
//		MetaPath queryMPath = new MetaPath("2 4 0 2 3 5 0 1 2");//vptpv
		FastBCore bCore = new FastBCore(graph, vertexType, edgeType);
		BatchECore eCore = new BatchECore(graph, vertexType, edgeType);
		BatchVCore vCore = new BatchVCore(graph, vertexType, edgeType);
		for(int k = 3;k <= 50; k ++) {
			int count = 0;
			double bF1Score = 0.0, eF1Score = 0.0, vF1Score = 0.0;

			for(int i = 0;i < graph.length;i ++) {
				if(vertexType[i] == 2) {
					Set<Integer> set = groundMap.get(i);

					Set<Integer> bSet = bCore.query(i, queryMPath, k);
					Set<Integer> eSet = eCore.query(i, queryMPath, k);
					Set<Integer> vSet = vCore.query(i, queryMPath, k);

					if(bSet != null && eSet !=null && vSet != null) {
						double pre1 = Precision.compute(set, bSet);
						double rec1 = Recall.compute(set, bSet);
						bF1Score += 2 * pre1 * rec1 / (pre1 + rec1);

						double pre2 = Precision.compute(set, eSet);
						double rec2 = Recall.compute(set, eSet);
						eF1Score += 2 * pre2 * rec2 / (pre2 + rec2);

						double pre3 = Precision.compute(set, vSet);
						double rec3 = Recall.compute(set, vSet);
						vF1Score += 2 * pre3 * rec3 / (pre3 + rec3);

						count ++;
					}
				}
			}

			bF1Score /= count;
			eF1Score /= count;
			vF1Score /= count;

			System.out.println("k=" + k + " count=" + count);
			System.out.println("bF1Score=" + bF1Score + "\neF1Score=" + eF1Score + "\nvF1Score" + vF1Score);
		}
	}

}
