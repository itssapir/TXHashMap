import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;

public class HashMapTXTest2 {

    @Test
    public void testHashMapMultiThreadMultiMap() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TXHashMap<Integer, String> HM1 = new TXHashMap<>();
        TXHashMap<Integer, String> HM2 = new TXHashMap<>();
        
        int threadAmnt = 16;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadAmnt; ++i) {
        	threads.add(new Thread(new Run("T"+i, latch, HM1, HM2)));
        }
        
        for (Thread t : threads) {
        	t.start();
        }
       
        latch.countDown();
        
        for (Thread t : threads) {
        	t.join();
        }
        return;
    }

    class Run implements Runnable {

        TXHashMap<Integer, String> HM1;
        TXHashMap<Integer, String> HM2;
        String threadName;
        CountDownLatch latch;

        Run(String name, CountDownLatch l, TXHashMap<Integer, String> hm1, TXHashMap<Integer, String> hm2) {
            threadName = name;
            latch = l;
            HM1 = hm1;
            HM2 = hm2;
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
            String c2 = threadName + "-c2";
            String empty = "";
            Integer k_a = 10 + threadName.charAt(1);
            Integer k_b = 20 + threadName.charAt(1);
            Integer k_c = 30 + threadName.charAt(1);

            while (true) {
                try {
                    try {
                        TX.TXbegin();
                        /*
                        // test for size, don't use more than 9 threads..
                        assertEquals(null, HM1.put(k_c, c));
                        assertEquals(null, HM2.put(k_b, b));
                        assertEquals(null, HM2.put(k_c, c));                
                        assertEquals(c, HM2.put(k_c, c2));
                        assertEquals(null, HM1.put(k_a, a));
                        */
                        assertEquals(false, HM1.containsKey(k_c));
                        assertEquals(false, HM2.containsKey(k_c));
                        assertEquals(null, HM1.get(k_c));
                        assertEquals(null, HM2.get(k_c));
                        
                        assertEquals(null, HM1.put(k_c, c));
                        assertEquals(c, HM1.get(k_c));
                        assertEquals(null, HM2.get(k_c));   
                        
                        assertEquals(null, HM2.put(k_b, b));
                        assertEquals(null, HM1.get(k_b));
                        assertEquals(b, HM2.get(k_b));
                        
                        assertEquals(null, HM2.put(k_c, c2));
                        assertEquals(c, HM1.get(k_c));
                        assertEquals(c2, HM2.get(k_c));   
                        
                        assertEquals(true, HM1.containsKey(k_c));
                        assertEquals(true, HM2.containsKey(k_c));                     
                        assertEquals(c, HM1.remove(k_c));
                        assertEquals(false, HM1.containsKey(k_c));
                        assertEquals(true, HM2.containsKey(k_c));  
 
                        assertEquals(null, HM1.remove(k_b));
                        assertEquals(b, HM2.remove(k_b));
                        assertEquals(null, HM1.remove(k_c));
                        assertEquals(c2, HM2.remove(k_c));
                        
                        /*
                        assertEquals(false, HM1.containsKey(k_c));
                        assertEquals(null, HM1.get(k_c));
                        assertEquals(null, HM1.put(k_c, c));
                        assertEquals(true, HM1.containsKey(k_c));
                        assertEquals(false, HM1.containsKey(k_a));
                        assertEquals(false, HM1.containsKey(k_b));
                        assertEquals(null, HM1.get(k_b));
                        assertEquals(null, HM1.put(k_a, a));
                        assertEquals(null, HM1.put(k_b, b));
                        assertEquals(true, HM1.containsKey(k_b));
                        assertEquals(true, HM1.containsKey(k_a));
                        assertEquals(a, HM1.put(k_a, a));
                        assertEquals(b, HM1.put(k_b, b));
                        assertEquals(c, HM1.get(k_c));
                        assertEquals(a, HM1.get(k_a));
                        assertEquals(b, HM1.get(k_b));                
                        assertEquals(null, HM1.remove(-1));
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
                        */
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
