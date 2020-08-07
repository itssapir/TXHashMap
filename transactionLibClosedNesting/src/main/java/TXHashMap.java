import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

//import java.util.HashMap;
import sun.misc.Unsafe;

public class TXHashMap<K, V> {
    private static final int DEFAULT_SIZE = 8;

    private HashNodeList[] table = new HashNodeList[DEFAULT_SIZE];
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

    public TXHashMap() {
        for (int i=0; i<table.length; ++i) {
            table[i] = new HashNodeList();
        }
    }
    public void clear() {

    }

    public boolean containsKey(K key) {
        LocalStorage localStorage = TX.lStorage.get();
        int h;
        int keyHash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // from java standard implementation.
        // TODO: handle the resize

        int tableIdx = (table.length - 1) & keyHash; // TODO: should we save the table length in the tx?
        HashNodeList hnList = table[tableIdx];
        
        // TX
        if (hnList.isLocked() || hnList.getVersion() > localStorage.readVersion) {
            // abort TX
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }

        //insert to read set:
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm == null) {
        	hm = new LocalHashMap();
        	localStorage.hmMap.put(this, hm);
        }
        hm.hashReadSet.add(hnList);
        
        HashNode oldNode = writeSetGet(key, hnList);

        if (oldNode == null) {
            //check in table
            oldNode = getNode(hnList, key);
        }
        if (oldNode != null && oldNode.isDeleted == false) {
            return true;
        }

        return false;
    }

    public boolean containsValue(Object value) {
        return false;
    }

    public int size() {
        return 0;
    }



    public V get(K key) throws TXLibExceptions.AbortException {
        LocalStorage localStorage = TX.lStorage.get();
        int h;
        int keyHash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // from java standard implementation.
        // TODO: handle the resize

        int tableIdx = (table.length - 1) & keyHash; // TODO: should we save the table length in the tx?
        HashNodeList hnList = table[tableIdx];
        
        // TX
        if (hnList.isLocked() || hnList.getVersion() > localStorage.readVersion) {
            // abort TX
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }

        //insert to read set:
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm == null) {
        	hm = new LocalHashMap();
        	localStorage.hmMap.put(this, hm);
        }
        hm.hashReadSet.add(hnList);
        
        // search old val
        V oldVal = null;

        HashNode oldNode = writeSetGet(key, hnList);

        if (oldNode == null) {
            //check in table
            oldNode = getNode(hnList, key);
        }
        if (oldNode != null && oldNode.isDeleted == false) {
            oldVal = (V)oldNode.getValue();
        }

        return oldVal;
    }

    public V put(K key, V value) throws TXLibExceptions.AbortException {
        LocalStorage localStorage = TX.lStorage.get();
        int h;
        int keyHash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // from java standard implementation.
        // TODO: handle the resize

        int tableIdx = (table.length - 1) & keyHash; // TODO: should we save the table length in the tx?
        HashNodeList hnList = table[tableIdx];
        
        // TX
        localStorage.readOnly = false;
        if (hnList.isLocked() || hnList.getVersion() > localStorage.readVersion) {
            // abort TX
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }
    
        // search old val
        V oldVal = null;

        //insert to read set:
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm == null) {
        	hm = new LocalHashMap();
        	localStorage.hmMap.put(this, hm);
        }
        hm.hashReadSet.add(hnList);
        
        //insert to write set
        HashNode newNode = new HashNode(keyHash, key, value, null);
        HashNode oldNode = writeSetInsert(newNode, hnList);

        if (oldNode == null) {
            //check in table
            oldNode = getNode(hnList, key);
        }
        if (oldNode != null && oldNode.isDeleted == false) {
            oldVal = (V)oldNode.getValue();
        } else{
            //completely new node, increase size
            hm.sizeDiff++; 
        }

        return oldVal;
    }

    public V remove(K key)  throws TXLibExceptions.AbortException {
        LocalStorage localStorage = TX.lStorage.get();
        int h;
        int keyHash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // from java standard implementation.
        // TODO: handle the resize

        int tableIdx = (table.length - 1) & keyHash; // TODO: should we save the table length in the tx?
        HashNodeList hnList = table[tableIdx];
        
        // TX
        localStorage.readOnly = false;
        if (hnList.isLocked() || hnList.getVersion() > localStorage.readVersion) {
            // abort TX
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }
    
        // search old val
        V oldVal = null;

        //insert to read set:
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm == null) {
        	hm = new LocalHashMap();
        	localStorage.hmMap.put(this, hm);
        }
        hm.hashReadSet.add(hnList);

        HashNode oldNode = writeSetGet(key, hnList);
        if (oldNode == null) {
            //check in table
            oldNode = getNode(hnList, key);
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

    // return Node if it exists or null otherwise
    private HashNode getNode(HashNodeList hnList, K key) throws TXLibExceptions.AbortException {
        LocalStorage localStorage = TX.lStorage.get();
        K currKey = null;
        HashNode current = hnList.head;

        while (current != null){
            if (hnList.isLocked()) {
                // abort TX
                localStorage.TX = false;
                TXLibExceptions excep = new TXLibExceptions();
                throw excep.new AbortException();
            }
            // TODO: do we need fence?
            unsafe.loadFence();
            currKey = (K)current.getKey();
            HashNode next = current.next;
            unsafe.loadFence();
            if (hnList.isLocked() || hnList.getVersion() > localStorage.readVersion ) {
                // abort TX
                localStorage.TX = false;
                TXLibExceptions excep = new TXLibExceptions();
                throw excep.new AbortException();
            }
            if (key.equals(currKey) && current.isDeleted == false) {
                return current;
            }
            current = next;
        }
        return null;
    }

    // insert new node to the write set, and return the old node if existed
    private HashNode writeSetInsert(HashNode newNode, HashNodeList hnList) {
        LocalStorage localStorage = TX.lStorage.get();
        LocalHashMap hm = localStorage.hmMap.get(this);
        if (hm == null) {
        	hm = new LocalHashMap();
        	localStorage.hmMap.put(this, hm);
        }
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
        if (hm == null) {
        	hm = new LocalHashMap();
        	localStorage.hmMap.put(this, hm);
        }
        java.util.HashMap<Object, HashNode> listWriteSet = hm.hashWriteSet.get(hnList);
        if (listWriteSet == null) {
            return null;
        }
        HashNode oldNode = listWriteSet.get(key);
        return oldNode;
    }





    // TODO: other methods as required.
}


    