import java.util.HashMap;
import java.util.HashSet;

public class LocalHashMap {
	public int sizeDiff = 0;
	public int initialSize;
	public HMTable localHMTable;
	public HashSet<HashNodeList> hashReadSet = new HashSet<>();
	public HashMap<HashNodeList, HashMap<Object, HashNode>> hashWriteSet = new HashMap<>();
}
