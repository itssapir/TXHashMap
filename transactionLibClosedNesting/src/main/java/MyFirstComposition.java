import java.util.concurrent.CountDownLatch;

public class MyFirstComposition {
    protected LinkedList L1 ;
    protected LinkedList L2 ;
    protected Queue Q1;

    public Object insertToL1(Integer key, Object val)
    {
        try
        {
            return L1.putIfAbsent(key, val);
        }catch (TXLibExceptions.AbortException exp) {
            if (TX.FLAT_NESTING) {
                throw exp;
            }
        }

        return null;
    }

    public Object insertToL2(Integer key, Object val)
    {
        try
        {
            return L2.putIfAbsent(key, val);
        }catch (TXLibExceptions.AbortException exp) {
            if (TX.FLAT_NESTING) {
                throw exp;
            }
        }

        return null;
    }

    class MyVeryBasicTest implements Runnable{

        LinkedList ll1, ll2;
        CountDownLatch latch;

        MyVeryBasicTest(CountDownLatch cdl)
        {
            ll1 = L1;
            ll2 = L2;
            latch = cdl;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println("InterruptedException");
            }


        }
    }

}
