import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;

public class HashMapTXTest {

    @Test
    public void testHashMapMultiThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HashMap<Integer, String> HM = new HashMap<>();
 
        int threadAmnt = 5;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadAmnt; ++i) {
        	threads.add(new Thread(new Run("T"+i, latch, HM)));
        }
        
        for (Thread t : threads) {
        	t.start();
        }
       
        latch.countDown();
        
        for (Thread t : threads) {
        	t.join();
        }
    }

    class Run implements Runnable {

        HashMap<Integer, String> HM;
        String threadName;
        CountDownLatch latch;

        Run(String name, CountDownLatch l, HashMap<Integer, String> hm) {
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
            Integer k_a = 10 + threadName.charAt(1);
            Integer k_b = 20 + threadName.charAt(1);
            Integer k_c = 30 + threadName.charAt(1);

            while (true) {
                try {
                    try {
                        TX.TXbegin();
                        assertEquals(false, HM.containsKey(k_c));
                        assertEquals(null, HM.get(k_c));
                        assertEquals(null, HM.put(k_c, c));
                        assertEquals(true, HM.containsKey(k_c));
                        assertEquals(false, HM.containsKey(k_a));
                        assertEquals(false, HM.containsKey(k_b));
                        assertEquals(null, HM.get(k_b));
                        assertEquals(null, HM.put(k_a, a));
                        assertEquals(null, HM.put(k_b, b));
                        assertEquals(true, HM.containsKey(k_b));
                        assertEquals(true, HM.containsKey(k_a));
                        assertEquals(a, HM.put(k_a, a));
                        assertEquals(b, HM.put(k_b, b));
                        assertEquals(c, HM.get(k_c));
                        assertEquals(a, HM.get(k_a));
                        assertEquals(b, HM.get(k_b));
                        assertEquals(null, HM.remove(-1));
                        assertEquals(b, HM.remove(k_b));
                        assertEquals(null, HM.remove(k_b));
                        assertEquals(false, HM.containsKey(k_b));
                        assertEquals(a, HM.remove(k_a));
                        assertEquals(c, HM.remove(k_c));
                        assertEquals(null, HM.remove(k_a));
                        assertEquals(null, HM.get(k_c));
                        assertEquals(null, HM.put(k_b, b));
                        assertEquals(b, HM.get(k_b));
                        assertEquals(b, HM.put(k_b, empty));
                        assertEquals(empty, HM.get(k_b));
                        assertEquals(null, HM.put(k_c, c));
                        assertEquals(c, HM.get(k_c));
                        assertEquals(c, HM.put(k_c, empty));
                        assertEquals(empty, HM.get(k_c));
                        assertEquals(empty, HM.remove(k_b));
                        assertEquals(empty, HM.remove(k_c));
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
