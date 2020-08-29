import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


//import java.util.HashMap;
import sun.misc.Unsafe;

public class TXHashMap<K, V> {
    private static final int INITIAL_TABLE_LENGTH = 8;
    private static final float LOAD_FACTOR = 0.75f;
    private HMTable globalTable = new HMTable(INITIAL_TABLE_LENGTH);
    protected AtomicInteger size = new AtomicInteger();
    

    // TODO: needed for fence?
    private static Unsafe unsafe = null;
	static {
		Field f = null;
		try {
			f = Unsafe.class.getDeclaredField("theUnsafe");
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        assert f != null;
        f.setAccessible(true);
		try {
			unsafe = (Unsafe) f.get(null);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void clear() {

    }

    public boolean containsKey(K key) {
        LocalStorage localStorage = TX.lStorage.get();
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm == null) {
        	hm = new LocalHashMap();
            localStorage.hmMap.put(this, hm);
            hm.localHMTable = globalTable;
            hm.initialSize = size.get();
        }
        HashNodeList hnList = getHNList(key);

        // TX
        if (hnList.isLocked()) {
        	if (!hnList.isDeprecated || hnList.getVersion() > localStorage.readVersion) {
        		abortTX();
        	} else {
        		handleInResize();
        		// TODO :update the local variables
        		hnList = getHNList(key);
        	}       	
        }
     
        if ( hnList.getVersion() > localStorage.readVersion) {
        	abortTX();
        }

        //insert to read set:
        hm.hashReadSet.add(hnList);
        
        HashNode oldNode = writeSetGet(key, hnList);

        if (oldNode == null) {
            //check in table
            oldNode = getNode(hnList, key).getSecond();
        }
        if (oldNode != null && oldNode.isDeleted == false) {
            return true;
        }

        return false;
    }

    public boolean containsValue(Object value) {
        return false;
    }


    private void resize() {
        LocalStorage localStorage = TX.lStorage.get();
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm.localHMTable.inResize.compareAndSet(false, true) == false) { 
            // resize already happened, or is happening
            // TODO :abort or not? need to decide..
        	handleInResize();
        	return;
        }

        HashNodeList[] oldTable = globalTable.table;
        int oldTableLen = oldTable.length;
        HMTable newGlobalTable = new HMTable(2*oldTableLen);
        HashNodeList[] newTable = newGlobalTable.table;
        
        for (int i=0; i<newTable.length; ++i) {
            newTable[i] = new HashNodeList(i);
        }
        // move nodes from old table to the new table. 
        // the old NodeLists will stay locked.
        for (int i = 0 ; i < oldTableLen ; ++i) {
            HashNodeList hnList = oldTable[i];
            hnList.lock();
            hnList.isDeprecated = true;
            newTable [i].setVersion(oldTable[i].getVersion());
            newTable [i+oldTableLen].setVersion(oldTable[i].getVersion());
            // search the write set node in the table hnList:
			HashNode current = hnList.head;
			while (current != null) {
                HashNode next = current.next;
                int newIdx = (newTable.length - 1) & current.hash;
                current.next = newTable[newIdx].head;
                newTable[newIdx].head = current;
                current = next;
            }
        }
        globalTable = newGlobalTable;
        hm.localHMTable.resizeLatch.countDown();
        // update local TX state and return
        handleInResize();
    }



    public V get(K key) throws TXLibExceptions.AbortException {
        LocalStorage localStorage = TX.lStorage.get();
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm == null) {
        	hm = new LocalHashMap();
            localStorage.hmMap.put(this, hm);
            hm.localHMTable = globalTable;
            hm.initialSize = size.get();
        }
        HashNodeList hnList = getHNList(key);
        
        // TX
        if (hnList.isLocked()) {
        	if (!hnList.isDeprecated || hnList.getVersion() > localStorage.readVersion) {
        		abortTX();
        	} else {
        		handleInResize();
        		// TODO :update the local variables
        		hnList = getHNList(key);
        	}       	
        }
     
        if ( hnList.getVersion() > localStorage.readVersion) {
        	abortTX();
        }

        //insert to read set:
        hm.hashReadSet.add(hnList);
        
        // search old val
        V oldVal = null;

        HashNode oldNode = writeSetGet(key, hnList);

        if (oldNode == null) {
            //check in table
            oldNode = getNode(hnList, key).getSecond();
        }
        if (oldNode != null && oldNode.isDeleted == false) {
            oldVal = (V)oldNode.getValue();
        }

        return oldVal;
    }

    public V put(K key, V value) throws TXLibExceptions.AbortException {
        LocalStorage localStorage = TX.lStorage.get();
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm == null) {
        	hm = new LocalHashMap();
            localStorage.hmMap.put(this, hm);
            hm.localHMTable = globalTable;
            hm.initialSize = size.get();
        }
        int h;
        int keyHash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // from java standard implementation.
        HashNodeList hnList = getHNList(key);
        
        // TX
        localStorage.readOnly = false;
        if (hnList.isLocked()) {
        	if (!hnList.isDeprecated || hnList.getVersion() > localStorage.readVersion) {
        		abortTX();
        	} else {
        		handleInResize();
        		// TODO :update the local variables
        		hnList = getHNList(key);
        	}       	
        }
     
        if ( hnList.getVersion() > localStorage.readVersion) {
        	abortTX();
        }
    
        // search old val
        V oldVal = null;

        //insert to read set:
        hm.hashReadSet.add(hnList);
        
        //insert to write set
        HashNode newNode = new HashNode(keyHash, key, value, null);
        HashNode oldNode = writeSetInsert(newNode, hnList);

        if (oldNode == null) {
            //check in table
            oldNode = getNode(hnList, key).getSecond();
        }
        if (oldNode != null && oldNode.isDeleted == false) {
            oldVal = (V)oldNode.getValue();
        } else{
            //completely new node, increase size
            hm.sizeDiff++; 

            // check if resize is needed
            if (hm.initialSize + hm.sizeDiff > hm.localHMTable.table.length*LOAD_FACTOR) {
                resize();
            }
        }

        return oldVal;
    }

    public V remove(K key)  throws TXLibExceptions.AbortException {
        LocalStorage localStorage = TX.lStorage.get();
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm == null) {
        	hm = new LocalHashMap();
            localStorage.hmMap.put(this, hm);
            hm.localHMTable = globalTable;
            hm.initialSize = size.get();
        }        
        int h;
        int keyHash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // from java standard implementation.
        HashNodeList hnList = getHNList(key);
        
        // TX
        localStorage.readOnly = false;
        if (hnList.isLocked()) {
        	if (!hnList.isDeprecated || hnList.getVersion() > localStorage.readVersion) {
        		abortTX();
        	} else {
        		handleInResize();
        		// TODO :update the local variables
        		hnList = getHNList(key);
        	}       	
        }
     
        if ( hnList.getVersion() > localStorage.readVersion) {
            abortTX();
        }
    
        // search old val
        V oldVal = null;

        //insert to read set:
        hm.hashReadSet.add(hnList);

        HashNode oldNode = writeSetGet(key, hnList);
        if (oldNode == null) {
            //check in table
        	Pair<HashNodeList, HashNode> res = getNode(hnList, key); 
            hnList = res.getFirst();
        	oldNode = res.getSecond();
        }
        if (oldNode != null && oldNode.isDeleted == false) {
            oldVal = (V)oldNode.getValue();
            HashNode newNode = new HashNode(keyHash, key, oldVal, null);
            newNode.isDeleted = true;
            hm.sizeDiff--;
            writeSetInsert(newNode, hnList);
        }
        return oldVal;
    }







    // TODO: other methods as required.

    // insert new node to the write set, and return the old node if existed
    private HashNode writeSetInsert(HashNode newNode, HashNodeList hnList) {
        LocalStorage localStorage = TX.lStorage.get();
        LocalHashMap hm = localStorage.hmMap.get(this);
        java.util.HashMap<Object, HashNode> listWriteSet = hm.hashWriteSet.get(hnList);
        if (listWriteSet == null) {
            listWriteSet = new java.util.HashMap<>();
            hm.hashWriteSet.put(hnList, listWriteSet);
        }

        HashNode oldNode = listWriteSet.put(newNode.getKey() , newNode);
        return oldNode;
    }

    // get node from the write set
    private HashNode writeSetGet(K key, HashNodeList hnList) {
        LocalStorage localStorage = TX.lStorage.get();
        LocalHashMap hm = localStorage.hmMap.get(this);
        java.util.HashMap<Object, HashNode> listWriteSet = hm.hashWriteSet.get(hnList);
        if (listWriteSet == null) {
            return null;
        }
        HashNode oldNode = listWriteSet.get(key);
        return oldNode;
    }
    

    // return Node if it exists or null otherwise
    private Pair<HashNodeList, HashNode> getNode(HashNodeList hnList, K key) throws TXLibExceptions.AbortException {
        LocalStorage localStorage = TX.lStorage.get();
        K currKey = null;
        HashNode current = hnList.head;

        while (current != null){
            if (hnList.isLocked()) {
            	if (!hnList.isDeprecated || hnList.getVersion() > localStorage.readVersion) {
                    abortTX();
            	} else {
            		handleInResize();
            		// Get new hnList for given key, and start iterations from scratch
            		hnList = getHNList(key);
                    current = hnList.head;
                    continue;
            	}       	
            }
         
            // TODO: do we need fence?
            unsafe.loadFence();
            currKey = (K)current.getKey();
            HashNode next = current.next;
            unsafe.loadFence();
            if (hnList.isLocked()) {
            	if (!hnList.isDeprecated || hnList.getVersion() > localStorage.readVersion) {
                    abortTX();
            	} else {
            		handleInResize();
            		// Get new hnList for given key, and start iterations from scratch
            		hnList = getHNList(key);
                    current = hnList.head;
                    continue;
            	}       	
            }
         
            if ( hnList.getVersion() > localStorage.readVersion) {
                abortTX();
            }
            if (key.equals(currKey) && current.isDeleted == false) {
                return new Pair<HashNodeList, HashNode>(hnList, current);
            }
            current = next;
        }
        return new Pair<HashNodeList, HashNode>(hnList, null);
    }

    
    
    private HashNodeList getHNList(K key) {
        LocalStorage localStorage = TX.lStorage.get();
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm == null) {
        	hm = new LocalHashMap();
            localStorage.hmMap.put(this, hm);
            hm.localHMTable = globalTable;
            hm.initialSize = size.get();
        }
        HashNodeList[] table = hm.localHMTable.table;
        
        int h;
        int keyHash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // from java standard implementation.

        int tableIdx = (table.length - 1) & keyHash; // TODO: should we save the table length in the tx?
        return table[tableIdx];
    }
    
    
    private void abortTX() {
        // abort TX
        LocalStorage localStorage = TX.lStorage.get();
        localStorage.TX = false;
        TXLibExceptions excep = new TXLibExceptions();
        throw excep.new AbortException();
    }
    

    private void handleInResize() {
    	// TODO: implement 
        LocalStorage localStorage = TX.lStorage.get();
        LocalHashMap hm = localStorage.hmMap.get(this);
        int oldTableLen = hm.localHMTable.table.length;
        // Wait for resize operations to complete and update to new globalHMTable
        do {
        	try {
				hm.localHMTable.resizeLatch.await();
			} catch (InterruptedException e) {
				// TODO do we want to handle this?
				abortTX();
			}        
	        // get new globalTable to local state
	        hm.localHMTable = globalTable;
        } while (hm.localHMTable.inResize.get());
        
        // Update read set to new state
        HashSet<HashNodeList> oldReadSet = hm.hashReadSet;
        hm.hashReadSet = new HashSet<>();
        int numResizes = hm.localHMTable.table.length / oldTableLen; // in case multiple resizes occurred
        for (HashNodeList hnList : oldReadSet) {
        	for (int i=0; i<numResizes; ++i) {
        		hm.hashReadSet.add(hm.localHMTable.table[hnList.index + i*oldTableLen]);
        	}
        }
        
        // Update write set to new state
        HashMap<HashNodeList, HashMap<Object, HashNode>> oldWriteSet = hm.hashWriteSet;
        hm.hashWriteSet = new HashMap<>();
        for (HashMap<Object, HashNode> wsNodesMap : oldWriteSet.values()) {
        	for (HashNode node : wsNodesMap.values()) {
        		writeSetInsert(node, getHNList((K)node.getKey()));
        	}
        }
    	return;
    }
    
    
    
}


    