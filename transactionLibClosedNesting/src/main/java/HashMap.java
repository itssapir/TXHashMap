//import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public class HashMap<K,V> {
    private static final int DEFAULT_SIZE = 128;
    // Node structure
    static class Node<K,V> {
        final int hash;
        final K key;
        V value;
        Node<K,V> next;

        Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey() {
            return key;
        }
        public final V getValue() {
            return value;
        }
        public final String toString() {
            return key + "=" + value;
        }
        
        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }
    }

    static class NodeList<K,V> {
        private static final long versionNegMask = 0x0L;
        private LockQueue nlLock = new LockQueue();
        public Node<K,V> head;
        private AtomicLong versionAndFlags = new AtomicLong();
      
        NodeList() {
            this.head = null;
        }
        
        public long getVersion() {
            return (versionAndFlags.get() & (~versionNegMask));
        }
    
        public void setVersion(long version) {
            long l = versionAndFlags.get();
            assert(nlLock.isHeldByCurrent());
            l &= versionNegMask;
            l |= (version & (~versionNegMask));
            versionAndFlags.set(l);
        }
    
    
        public void lock() {
            nlLock.lock();
        }
    
        public boolean tryLock() {
            return nlLock.tryLock();
        }
    
        public void unlock() {
            nlLock.unlock();
        }
    }

    @SuppressWarnings({"unchecked"}) // TODO: should we remove this?
    private NodeList<K,V>[] table = (NodeList<K,V>[])new NodeList[DEFAULT_SIZE];
    private int size;

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


    public V get(Object key) {
        return null;
    }

    public V put(K key, V value) {
        return null;
    }

    public V remove(Object key) {
        return null;
    }



    // TODO: other methods as required.
}