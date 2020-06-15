import java.lang.reflect.Field;
import java.util.ArrayList;
//import java.util.HashMap;
import sun.misc.Unsafe;

public class HashMap<K,V> {
    private static final int DEFAULT_SIZE = 128;
 
    private HashNodeList[] table = new HashNodeList[DEFAULT_SIZE];
    private int size;

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

    public boolean isEmpty() {
        return false;
    }

    public boolean containsKey(Object key) {
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

        int tableIdx = (table.length) & keyHash; // TODO: should we save the table length in the tx?
        HashNodeList hnList = table[tableIdx];
        
        // TX
        if (hnList.isLocked() || hnList.getVersion() > localStorage.readVersion) {
            // abort TX
            localStorage.TX = false;
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new AbortException();
        }

        //insert to read set:
        localStorage.hashReadSet.add(hnList);

        // search old val
        V oldVal = null;

        HashNode oldNode = writeSetGet(key, hnList);

        if (oldNode == null) {
            //check in table
            oldNode = getNode(hnList, key);
        }
        if (oldNode != null) {
            oldVal = (V)oldNode.getValue();
        }

        return oldVal;
    }

    public V put(K key, V value) throws TXLibExceptions.AbortException {
        LocalStorage localStorage = TX.lStorage.get();
        int h;
        int keyHash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // from java standard implementation.
        // TODO: handle the resize

        int tableIdx = (table.length) & keyHash; // TODO: should we save the table length in the tx?
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
        localStorage.hashReadSet.add(hnList);
        
        //insert to write set
        HashNode newNode = new HashNode(keyHash, key, value, null);
        HashNode oldNode = writeSetInsert(newNode, hnList);

        if (oldNode == null) {
            //check in table
            oldNode = getNode(hnList, key);
        }
        if (oldNode != null) {
            oldVal = (V)oldNode.getValue();
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
            if (key.equals(currKey)) {
                return current;
            }
            current = next;
        }
        return null;
    }

    // insert new node to the write set, and return the old node if existed
    private HashNode writeSetInsert(HashNode newNode, HashNodeList hnList) {
        LocalStorage localStorage = TX.lStorage.get();
        java.util.HashMap<Object, HashNode> listWriteSet = localStorage.hashWriteSet.get(hnList);
        if (listWriteSet == null) {
            listWriteSet = new java.util.HashMap<>();
            localStorage.hashWriteSet.put(hnList, listWriteSet);
        }

        HashNode oldNode = listWriteSet.put(newNode.getKey() , newNode);
        return oldNode;
    }

    // get node from the write set
    private HashNode writeSetGet(K key, HashNodeList hnList) {
        LocalStorage localStorage = TX.lStorage.get();
        java.util.HashMap<Object, HashNode> listWriteSet = localStorage.hashWriteSet.get(hnList);
        if (listWriteSet == null) {
            return null;
        }
        HashNode oldNode = listWriteSet.get(key);
        return oldNode;
    }

    public V remove(Object key) {
        return null;
    }



    // TODO: other methods as required.
}


    