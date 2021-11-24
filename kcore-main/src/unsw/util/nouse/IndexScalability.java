package unsw.util.nouse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import unsw.Config;
//import unsw.exp.efficiency.QueryEfficiency;
//import unsw.exp.efficiency.SmallGraph;
import unsw.index.Decomposition;
import unsw.index.PathIndex;
import unsw.index.basic.BCoreDecomposition;
import unsw.index.edgeDisjoint.GreedyECoreDecomposition;
import unsw.index.vertexDisjoint.GreedyVCoreDecomposition;
import unsw.online.MetaPath;
import unsw.util.LogFinal;
import unsw.util.LogPart;

public class IndexScalability extends QueryEfficiency{
	private PathIndex pIndexConstructor;
	private List<MetaPath> queryMPathList;
	private SmallGraph sGraph;

	public IndexScalability(String dataPath, String dataSetName,List<Integer> queryKList) {
		super(dataPath, dataSetName, queryKList);
		// TODO Auto-generated constructor stub
		sGraph = new SmallGraph(graph , vertexType , edgeType);
		getQueryMetaPathList(dataPath);
		pIndexConstructor = new PathIndex(graph , vertexType , edgeType , queryMPathList);
	}

	private void getQueryMetaPathList(String metaPathsPath) {
		String metaPathsFile = metaPathsPath + "/metaPaths.txt";
		queryMPathList = new ArrayList<MetaPath>();
		try{
			BufferedReader stdin = new BufferedReader(new FileReader(metaPathsFile));
			String line = null;
			while((line = stdin.readLine()) != null){
				String s[] = line.split(" ");
				int[] vertex = new int[ (s.length + 1) / 2 ];
				int[] edge = new int[ (s.length - 1) / 2 ];
				for (int i = 0 ; i < s.length ; i = i + 2) {
					vertex[i / 2] = Integer.valueOf( s[i] );
					if (i + 1 < s.length) {
						edge[i / 2] = Integer.valueOf( s[i + 1] );
					}
				}
				MetaPath newMPath = new MetaPath(vertex , edge);
				queryMPathList.add(newMPath);
			}
			stdin.close();
		}catch(Exception e){
			e.printStackTrace();
		}

	}

	public void testBCoreScabality(int part , int total) {
		test(part , total , "BCore");
	}

	public void testECoreScabality(int part , int total) {
		test(part , total , "ECore");
	}

	public void testVCoreScabality(int part , int total) {
		test(part , total , "VCore");
	}

	private void test(int part , int total , String coreModelName) {
		Set<Integer> queryIdSet = new HashSet<Integer>();//Different from the QueryScalability; There is no vertices which must appear in the small graph;
		sGraph.getSmallGraph(part , total , queryIdSet);
		Decomposition decomposer = null;
		switch(coreModelName) {
			case "BCore":
				decomposer = new BCoreDecomposition(sGraph.smallGraph , sGraph.smallGraphVertexType , sGraph.smallGraphEdgeType);
				break;

			case "ECore":
				decomposer = new GreedyECoreDecomposition(sGraph.smallGraph , sGraph.smallGraphVertexType , sGraph.smallGraphEdgeType);
				break;

			case "VCore":
				decomposer = new GreedyVCoreDecomposition(sGraph.smallGraph , sGraph.smallGraphVertexType , sGraph.smallGraphEdgeType);
				break;
		}

		pIndexConstructor = new PathIndex(sGraph.smallGraph , sGraph.smallGraphVertexType , sGraph.smallGraphEdgeType , queryMPathList);
		long startTime = System.nanoTime();
		pIndexConstructor.build(decomposer);
		long endTime = System.nanoTime();

		double rate = (double) part / (double) total;
		String logInfo = dataSetName + "\t" + coreModelName + "\t IndexScalability"
				+ "\tPercent:" + rate + "\t" + String.valueOf(endTime - startTime);
		LogFinal.log(logInfo);
	}

	public static void main(String[] args) {
		List<String> dataSetList = new ArrayList<String>();
//		dataSetList.add("usFourSquare");
//		dataSetList.add("newDBLP");
//		dataSetList.add("yearIMDB");
		dataSetList.add("DBPedia");
		dataSetList.add("FBMusic");
		List<Integer> queryKList = new ArrayList<Integer>();
		queryKList.add(6);
		for (String dataSetName : dataSetList) {
			String graphDataSet = Config.root + "/data/" + dataSetName;

			for (int i = 5 ; i <= 5; i++) {
				IndexScalability indexScaleTest = new IndexScalability(graphDataSet, dataSetName, queryKList);
				indexScaleTest.testBCoreScabality(i , 5);

				System.gc();
				try{Thread.sleep(15000);}catch(Exception e){}//sleep
			}
			for (int i = 5 ; i <= 5 ; i++) {
				IndexScalability indexScaleTest = new IndexScalability(graphDataSet, dataSetName, queryKList);
				indexScaleTest.testECoreScabality(i , 5);

				System.gc();
				try{Thread.sleep(15000);}catch(Exception e){}//sleep
			}
			for (int i = 5 ; i <= 5 ; i++) {
				IndexScalability indexScaleTest = new IndexScalability(graphDataSet, dataSetName, queryKList);
				indexScaleTest.testVCoreScabality(i , 5);

				System.gc();
				try{Thread.sleep(15000);}catch(Exception e){}//sleep
			}

			LogFinal.log("\n");
			LogPart.log("\n");
		}
	}
}
