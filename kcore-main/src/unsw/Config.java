package unsw;

import java.io.File;

import javax.swing.filechooser.FileSystemView;

/**
 * @author fangyixiang
 * @date 2018-09-10
 * Global parameters
 */
public class Config {
    //stem file paths
    public static String stemFile = "./stemmer.lowercase.txt";
    public static String stopFile = "./stopword.txt";

    //the root of date files
//	public static String root = "/data/hancwang/YIxing/HINData";
//	public static String root = "C:\\Users\\fangyixiang\\Desktop";
    public static String root = "D:\\Just\\UNSW\\HINData\\";

    //D:\Just\UNSW\HINData\DBLP
    {
        FileSystemView fsv = FileSystemView.getFileSystemView();
        File com = fsv.getHomeDirectory();
        root = com.getPath();//automatically obtain the path of Desktop
    }

    //SmallDBLP
    public static String smallDBLPRoot = root + "\\HIN\\dataset\\SmallDBLP\\";
    public static String smallDBLPGraph = smallDBLPRoot + "graph.txt";
    public static String smallDBLPVertex = smallDBLPRoot + "vertex.txt";
    public static String smallDBLPEdge = smallDBLPRoot + "edge.txt";

    //DBLP
    public static String dblpRoot = root + "\\DBLP\\";
    public static String dblpGraph = dblpRoot + "graph.txt";
    public static String dblpVertex = dblpRoot + "vertex.txt";
    public static String dblpEdge = dblpRoot + "edge.txt";

    //IMDB
    public static String IMDBRoot = root + "\\HIN\\dataset\\yearIMDB\\";
    public static String IMDBGraph = IMDBRoot + "graph.txt";
    public static String IMDBVertex = IMDBRoot + "vertex.txt";
    public static String IMDBEdge = IMDBRoot + "edge.txt";

    //Foursquare
    public static String FsqRoot = root + "\\Foursquare\\";
    public static String FsqGraph = FsqRoot + "graph.txt";
    public static String FsqVertex = FsqRoot + "vertex.txt";
    public static String FsqEdge = FsqRoot + "edge.txt";

    //DBpedia
    public static String dbpediaRoot = root + "\\HIN\\dataset\\DBPedia\\";
    public static String dbpediaGraph = dbpediaRoot + "graph.txt";
    public static String dbpediaVertex = dbpediaRoot + "vertex.txt";
    public static String dbpediaEdge = dbpediaRoot + "edge.txt";

    public static String machineName = "Phoenix19";
    //	public static String logFinalResultFile = Config.root + "/outdata/" + machineName;//our final experimental result data
    public static String logFinalResultFile = "./log";//our final experimental result data
    public static String logPartResultFile = Config.root + "/outdata/" + machineName + "-part";//intermediate result

    public static int k = 6;
}
