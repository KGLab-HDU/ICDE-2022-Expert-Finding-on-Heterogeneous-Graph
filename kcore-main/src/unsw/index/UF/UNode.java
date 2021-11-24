package unsw.index.UF;

/**
 * @author fangyixiang
 * @date Sep 16, 2015
 */
public class UNode {
	public int value = 0;
	public UNode parent = null;
	public int rank = -1;
	
	
	public UNode(int value){
		this.value = value;
	}
}
