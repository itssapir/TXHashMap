import java.util.concurrent.atomic.AtomicLong;

public class HashNodeList {
    private static final long deprecatedMask = 0x1000000000000000L;
    private static final long singletonMask = 0x2000000000000000L;
    private static final long versionNegMask = deprecatedMask | singletonMask;
    private LockQueue nlLock = new LockQueue();
    public HashNode head;
    public int index;
    private AtomicLong versionAndFlags = new AtomicLong();
  
    HashNodeList(int idx) {
        this.head = null;
        index = idx;
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

    protected boolean isSingleton() {
        long l = versionAndFlags.get();
        return (l & singletonMask) != 0;
    }

    protected void setSingleton(boolean value) {
        long l = versionAndFlags.get();
        if (value) {
            l |= singletonMask;
            versionAndFlags.set(l);
            return;
        }
        l &= (~singletonMask);
        versionAndFlags.set(l);
    }

    protected boolean isSameVersionAndSingleton(long version) {
        long l = versionAndFlags.get();
        if ((l & singletonMask) != 0) {
            l &= (~versionNegMask);
            return l == version;
        }
        return false;
    }

    protected void setVersionAndSingleton(long version, boolean value) {
        long l = versionAndFlags.get();
        l &= versionNegMask;
        l |= (version & (~versionNegMask));
        if (value) {
            l |= singletonMask;
            versionAndFlags.set(l);
            return;
        }
        l &= (~singletonMask);
        versionAndFlags.set(l);
    }

    protected boolean isDeprecated() {
        long l = versionAndFlags.get();
        return (l & deprecatedMask) != 0;
    }

    protected void setDeprecated(boolean value) {
        long l = versionAndFlags.get();
        if (value) {
            l |= deprecatedMask;
            versionAndFlags.set(l);
            return;
        }
        l &= (~deprecatedMask);
        versionAndFlags.set(l);
    }
    
    protected boolean isDeprecatedAndOlderVersion(long version) {
    	long l = versionAndFlags.get();
        if ((l & deprecatedMask) != 0) {
            boolean isSingleton = ((l & singletonMask) != 0);
            l &= (~versionNegMask);
            return l < version || (l == version && !isSingleton);
        }
        return false;
    }
}