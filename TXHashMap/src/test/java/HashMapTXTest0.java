import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;

public class HashMapTXTest0 {

    @RepeatedTest(100)
    public void testHashMapBasic() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TXHashMap<Integer,String> HM = new TXHashMap<>();
        Thread T1 = new Thread(new Run("T1", latch, HM));
        Thread T2 = new Thread(new Run("T2", latch, HM));
        Thread T3 = new Thread(new Run("T3", latch, HM));
        T1.start();
        T2.start();
        T3.start();
        latch.countDown();
        T1.join();
        T2.join();
        T3.join();
    }

    class Run implements Runnable {

        TXHashMap<Integer,String> HM;
        String threadName;
        CountDownLatch latch;

        Run(String name, CountDownLatch l, TXHashMap<Integer,String> hm) {
            threadName = name;
            latch = l;
            HM = hm;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println(threadName + ": InterruptedException");
            }
            String a = threadName + "-a";
            String b = threadName + "-b";
            String c = threadName + "-c";
            String empty = "";
            Integer k_a = 10 + threadName.charAt(1) - '0';
            Integer k_b = 20 + threadName.charAt(1) - '0';
            Integer k_c = 30 + threadName.charAt(1) - '0';

            while (true) {
                try {
                    try {
                        TX.TXbegin();
                        assertEquals(null, HM.get(k_c));
                        assertEquals(null, HM.put(k_c, c));
                        assertEquals(null, HM.get(k_b));
                        assertEquals(null, HM.put(k_a, a));
                        assertEquals(null, HM.put(k_b, b));
                        assertEquals(a, HM.put(k_a, a));
                        assertEquals(b, HM.put(k_b, b));
                        assertEquals(c, HM.get(k_c));
                        assertEquals(a, HM.get(k_a));
                        assertEquals(b, HM.get(k_b));

                    } finally {
                        TX.TXend();
                    }
                } catch (TXLibExceptions.AbortException exp) {
                    continue;
                }
                break;
            }

        }
    }

}
