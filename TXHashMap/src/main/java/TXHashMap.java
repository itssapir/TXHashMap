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
			e.printStackTrace();
		}
        assert f != null;
        f.setAccessible(true);
		try {
			unsafe = (Unsafe) f.get(null);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
    }
    
    public V put(K key, V value) throws TXLibExceptions.AbortException {
        LocalStorage localStorage = TX.lStorage.get();
        
		// SINGLETON
		if (!localStorage.TX) {
			return putSingleton(key, value);
		}
		
		
        // TX
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
        
        localStorage.readOnly = false;
        if (hnList.isDeprecatedAndOlderVersion(localStorage.readVersion)) {
    		handleInResize();
    		hnList = getHNList(key);
    	}

        if (hnList.isLocked() || hnList.getVersion() > localStorage.readVersion) {
        	abortTX();
        }
        
        if (hnList.isSameVersionAndSingleton(localStorage.readVersion)) {
        	TX.incrementAndGetVersion();
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

    private V putSingleton(K key, V value) {
    	HMTable localTable;
    	HashNodeList hnList;
        int h;
        int keyHash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // from java standard implementation.

        while (true) {
        	localTable = globalTable;
            int tableIdx = (localTable.table.length - 1) & keyHash;
            hnList = localTable.table[tableIdx];
            hnList.lock();
            
            if (hnList.isDeprecated()) {
            	hnList.unlock();
            	while (true) {
	            	try {
						localTable.resizeLatch.await();
						break;
					} catch (InterruptedException e) {
						continue;
					}
            	}
            	continue;
            }
            // got lock on valid hnList (not deprecated)
            break;
        }
        
        // search for existing key
        V retVal = null;
        HashNode current = hnList.head;
		HashNode prev = null;
				
		while (current != null) {
			if (current.getKey().equals(key)) {
				retVal = (V)current.getValue();
				current.value = value;
				current.isDeleted = false;
				break;
			}
			prev = current;
			current = current.next;
		}
		
		if (current == null) {
			// add new node
			if (prev == null) {
				hnList.head = new HashNode(keyHash, key, value, null);
			} else {
				prev.next = new HashNode(keyHash, key, value, null);
			}
		}
		
		hnList.setVersionAndSingleton(TX.getVersion(), true);
		hnList.unlock();
		if (size.incrementAndGet() > localTable.table.length*LOAD_FACTOR) {
			resizeSingleton(localTable);
		}
        return retVal;
	}

	public V get(K key) throws TXLibExceptions.AbortException {
	    LocalStorage localStorage = TX.lStorage.get();
	    
		// SINGLETON
		if (!localStorage.TX) {
			return getSingleton(key);
		}
	    
	    // TX
	    LocalHashMap hm = localStorage.hmMap.get(this);
	    if (hm == null) {
	    	hm = new LocalHashMap();
	        localStorage.hmMap.put(this, hm);
	        hm.localHMTable = globalTable;
	        hm.initialSize = size.get();
	    }
	    HashNodeList hnList = getHNList(key);

        if (hnList.isDeprecatedAndOlderVersion(localStorage.readVersion)) {
    		handleInResize();
    		hnList = getHNList(key);
    	}

        if (hnList.isLocked() || hnList.getVersion() > localStorage.readVersion) {
        	abortTX();
        }
        
        if (hnList.isSameVersionAndSingleton(localStorage.readVersion)) {
        	TX.incrementAndGetVersion();
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

	private V getSingleton(K key) {
        return findKeySingleton(key).getSecond();
	}

	public V remove(K key)  throws TXLibExceptions.AbortException {
        LocalStorage localStorage = TX.lStorage.get();
        
		// SINGLETON
		if (!localStorage.TX) {
			return removeSingleton(key);
		}
		
        // TX
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
        
        localStorage.readOnly = false;
        if (hnList.isDeprecatedAndOlderVersion(localStorage.readVersion)) {
    		handleInResize();
    		hnList = getHNList(key);
    	}

        if (hnList.isLocked() || hnList.getVersion() > localStorage.readVersion) {
        	abortTX();
        }
        
        if (hnList.isSameVersionAndSingleton(localStorage.readVersion)) {
        	TX.incrementAndGetVersion();
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







    private V removeSingleton(K key) {
    	HMTable localTable;
    	HashNodeList hnList;
        int h;
        int keyHash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // from java standard implementation.

        while (true) {
        	localTable = globalTable;
            int tableIdx = (localTable.table.length - 1) & keyHash;
            hnList = localTable.table[tableIdx];
            hnList.lock();
            
            if (hnList.isDeprecated()) {
            	hnList.unlock();
            	while (true) {
	            	try {
						localTable.resizeLatch.await();
						break;
					} catch (InterruptedException e) {
						continue;
					}
            	}
            	continue;
            }
            // got lock on valid hnList (not deprecated)
            break;
        }
        
        // search for existing key
        V retVal = null;
        HashNode current = hnList.head;
		HashNode prev = null;
				
		while (current != null) {
			if (current.getKey().equals(key)) {
				retVal = (V)current.getValue();
				current.isDeleted = true;
				hnList.setVersionAndSingleton(TX.getVersion(), true);
				break;
			}
			prev = current;
			current = current.next;
		}
		
		if (current != null) {
			// removed node, link prev to next node
			if (prev == null) {
				hnList.head = current.next;
			} else {
				prev.next = current.next;
			}
		}
		
		hnList.setVersionAndSingleton(TX.getVersion(), true);
		size.decrementAndGet();
		hnList.unlock();
		
        return retVal;
	}

    
    public boolean containsKey(K key) {
	    LocalStorage localStorage = TX.lStorage.get();
	    
		// SINGLETON
		if (!localStorage.TX) {
			return containsKeySingleton(key);
		}
		
	    // TX
	    LocalHashMap hm = localStorage.hmMap.get(this);
	    if (hm == null) {
	    	hm = new LocalHashMap();
	        localStorage.hmMap.put(this, hm);
	        hm.localHMTable = globalTable;
	        hm.initialSize = size.get();
	    }
	    HashNodeList hnList = getHNList(key);
	
        if (hnList.isDeprecatedAndOlderVersion(localStorage.readVersion)) {
    		handleInResize();
    		hnList = getHNList(key);
    	}

        if (hnList.isLocked() || hnList.getVersion() > localStorage.readVersion) {
        	abortTX();
        }
        
        if (hnList.isSameVersionAndSingleton(localStorage.readVersion)) {
        	TX.incrementAndGetVersion();
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

	private boolean containsKeySingleton(K key) {
		return findKeySingleton(key).getFirst();
	}




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
        	if (hnList.isDeprecatedAndOlderVersion(localStorage.readVersion)) {
        		handleInResize();
        		// Get new hnList for given key, and start iterations from scratch
        		hnList = getHNList(key);
                current = hnList.head;
                continue;
        	}
            if (hnList.isLocked()) {
            		abortTX();    	
            }
         
            unsafe.loadFence();
            currKey = (K)current.getKey();
            HashNode next = current.next;
            unsafe.loadFence();
            
            if (hnList.isDeprecatedAndOlderVersion(localStorage.readVersion)) {
        		handleInResize();
        		// Get new hnList for given key, and start iterations from scratch
        		hnList = getHNList(key);
                current = hnList.head;
                continue;
        	}
         
            if (hnList.isLocked() || hnList.getVersion() > localStorage.readVersion) {
            	abortTX();
            }
            
            if (hnList.isSameVersionAndSingleton(localStorage.readVersion)) {
            	TX.incrementAndGetVersion();
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

        int tableIdx = (table.length - 1) & keyHash;
        return table[tableIdx];
    }
    

    
    private Pair<Boolean, V> findKeySingleton(K key) {
	   	HMTable localTable = globalTable;
		int h;
	    int keyHash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // from java standard implementation.
	    int tableIdx = (localTable.table.length - 1) & keyHash;
	
	    HashNodeList hnList = localTable.table[tableIdx];
	    // search for existing key
	    K currKey = null;
	    V retVal = null;
	    HashNode current = hnList.head;
	    
	    boolean startOver = false;
	    
	    while (current != null){
	    	if (startOver) {
	    		localTable = globalTable;
	    		tableIdx = (localTable.table.length - 1) & keyHash;
	    		hnList = localTable.table[tableIdx];
	    		currKey = null;
	    		current = hnList.head;
	    		startOver = false;
	    		continue;
	    	}
	        long prevVer = -1;
	        long curVer = hnList.getVersion();
	        HashNode next = null;
	        while (prevVer != curVer) {
	        	prevVer = curVer;
	        	unsafe.loadFence();
	            currKey = (K)current.getKey();
	            retVal = (V)current.getValue();
	            next = current.next;
	            unsafe.loadFence();
	            if (hnList.isLocked()) {
	            	startOver = true;
	            	break;
	            }
	            curVer = hnList.getVersion();	
	        }
	        if (startOver) {
	        	continue;
	        }
	
	        if (key.equals(currKey) && current.isDeleted == false) {
	        	return new Pair<Boolean,V>(true,retVal);
	        }
	        current = next;
	    }
	    return new Pair<Boolean,V>(false,null);
	}

	private void resize() {
        LocalStorage localStorage = TX.lStorage.get();
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm.localHMTable.inResize.compareAndSet(false, true) == false) { 
            // resize already happened, or is happening
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
            hnList.setDeprecated(true);
            hnList.unlock();
            
            newTable[i].setVersion(oldTable[i].getVersion());
            newTable[i].setSingleton(oldTable[i].isSingleton());
            newTable[i+oldTableLen].setVersion(oldTable[i].getVersion());
            newTable[i+oldTableLen].setSingleton(oldTable[i].isSingleton());
            
            // Copy hnList nodes to new table:
			HashNode current = hnList.head;
			while (current != null) {
				int newIdx = (newTable.length - 1) & current.hash;
				HashNode newNode = new HashNode(current.hash, current.getKey(), current.getValue(), newTable[newIdx].head);
                newTable[newIdx].head = newNode;
                current = current.next;
            }
        }
        globalTable = newGlobalTable;
        hm.localHMTable.resizeLatch.countDown();
        // update local TX state and return
        handleInResize();
    }
    
	private void resizeSingleton(HMTable localTable) {
        if (localTable.inResize.compareAndSet(false, true) == false) { 
            // resize already happened, or is happening
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
        for (int i = 0 ; i < oldTableLen ; ++i) {
            HashNodeList hnList = oldTable[i];
            hnList.lock();
            hnList.setDeprecated(true);
            hnList.unlock();
            newTable[i].setVersion(oldTable[i].getVersion());
            newTable[i].setSingleton(oldTable[i].isSingleton());
            newTable[i+oldTableLen].setVersion(oldTable[i].getVersion());
            newTable[i+oldTableLen].setSingleton(oldTable[i].isSingleton());
            
            // Copy hnList nodes to new table:
 			HashNode current = hnList.head;
 			while (current != null) {
 				int newIdx = (newTable.length - 1) & current.hash;
 				HashNode newNode = new HashNode(current.hash, current.getKey(), current.getValue(), newTable[newIdx].head);
                 newTable[newIdx].head = newNode;
                 current = current.next;
             }
        }
        
        globalTable = newGlobalTable;
        localTable.resizeLatch.countDown();
    }
	
    private void abortTX() {
        // abort TX
        LocalStorage localStorage = TX.lStorage.get();
        localStorage.TX = false;
        TXLibExceptions excep = new TXLibExceptions();
        throw excep.new AbortException();
    }
    

    private void handleInResize() {
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


    