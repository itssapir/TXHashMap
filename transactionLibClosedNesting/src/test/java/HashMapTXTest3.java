import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;

/*
 *	checking serialization of transactions on reading/writing hashMap
 *	2 hashMaps, 2 indices. 
 */
public class HashMapTXTest3 {

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
            Integer k_a = 10;
            Integer k_b = 20;
            Integer k_c = 30;

            while (true) {
                try {
                    try {
                        TX.TXbegin();
                        
                        if (HM1.containsKey(k_a) == true) {
                        	String current = HM1.get(k_a);
                        	assertEquals(current, HM1.put(k_a, current  + " " + a));
                        } else {
                            assertEquals(null, HM1.put(k_a, a));
                        }
                        
                        if (HM2.containsKey(k_a) == true) {
                        	String current = HM2.get(k_a);
                        	assertEquals(current, HM2.put(k_a, current  + " " + a));
                        } else {
                            assertEquals(null, HM2.put(k_a, a));
                        }
                        
                        if (HM1.containsKey(k_b) == true) {
                        	String current = HM1.get(k_b);
                        	assertEquals(current, HM1.put(k_b, current  + " " + b));
                        } else {
                            assertEquals(null, HM1.put(k_b, b));
                        }
                        
                        if (HM2.containsKey(k_b) == true) {
                        	String current = HM2.get(k_b);
                        	assertEquals(current, HM2.put(k_b, current  + " " + b));
                        } else {
                            assertEquals(null, HM2.put(k_b, b));
                        }
                        
                        
                        
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
