package dummyImplementation;

//import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class TXHashMap<K,V> {

    // Node structure
    static class Node<K,V> {
        final int hash;
        final K key;
        V value;
        int updateTime;
        Node<K,V> next;

        Node(int hash, K key, V value, int updateTime, Node<K,V> next) {
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
        Node<K,V> head;
        ReentrantLock lock;
        int updateTime;

        NodeList() {
            this.head = null;
            this.updateTime = 0;
            lock = new ReentrantLock();
        }
        public final int getUpdateTime() {
            return updateTime;
        }
    }

    static private AtomicInteger GVC = new AtomicInteger();
    private NodeList<K,V>[] table;
    private int size;
    static private ThreadLocal<Integer /*TODO: data structure for each thread*/> TXData = new ThreadLocal<>();    

    
    public TXHashMap() {

    }

    public void TXBegin() {
        
    }


    public void TXCommit() {
        
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