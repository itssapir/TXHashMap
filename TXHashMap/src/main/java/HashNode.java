public class HashNode {
    public final int hash;
    public final Object key;
    public Object value;
    public boolean isDeleted;
    public HashNode next;

    HashNode(int hash, Object key, Object value, HashNode next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.isDeleted = false;
        this.next = next;
    }

    public final Object getKey() {
        return key;
    }
    public final Object getValue() {
        return value;
    }
    public final String toString() {
        return key + "=" + value;
    }
    
    public final Object setValue(Object newValue) {
        Object oldValue = value;
        value = newValue;
        return oldValue;
    }
}