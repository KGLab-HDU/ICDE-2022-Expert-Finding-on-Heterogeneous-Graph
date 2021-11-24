package unsw.util;

/**
 * @author fangyixiang
 * @date 10 Sep. 2018
 * check whether an incident edge is matched or not
 */
public class PathVertexEdgeMatch {
	
	public static boolean match(int targetVType, int vertexTypeArr[]) {
		for(int vertexType:vertexTypeArr) {
			if(vertexType == targetVType) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean match(int targetVType, int vertexTypeArr[], int targetEType, int edgeTypeArr[]) {
		for(int vertexType:vertexTypeArr) {
			if(vertexType == targetVType) {
				for(int edgeType:edgeTypeArr) {
					if(edgeType == targetEType) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
