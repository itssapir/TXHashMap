import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class HMTable {
    protected HashNodeList[] table;
    protected AtomicBoolean inResize;
    protected CountDownLatch resizeLatch;
    
    HMTable(int tableLength) {
    	table = new HashNodeList[tableLength];
    	inResize = new AtomicBoolean();
    	resizeLatch = new CountDownLatch(1);
    	for (int i=0; i<table.length; ++i) {
    		table[i] = new HashNodeList(i);
        }
    }
    
}
