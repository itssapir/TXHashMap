import java.util.concurrent.atomic.AtomicLong;

public class HashNodeList {
    private static final long versionNegMask = 0x0L;
    private LockQueue nlLock = new LockQueue();
    public HashNode head;
    private AtomicLong versionAndFlags = new AtomicLong();
  
    HashNodeList() {
        this.head = null;
    }
    
    public long getVersion() {
        return (versionAndFlags.get() & (~versionNegMask));
    }

    public void setVersion(long version) {
        long l = versionAndFlags.get();
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

    public boolean isLocked() {
        return nlLock.isLocked();
    }
}