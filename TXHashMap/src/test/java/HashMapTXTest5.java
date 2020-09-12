import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;

/*
 * test resize functionality : 
 * 		1				number of hashMaps
 * 		threadAmnt 		number of threads
 * 		keysPerThread	number of keys each thread inserts
 * 	each insertion is for unique key : [100*threadNum, 100*threadNum + keysPerThread-1]
 * 
 */
public class HashMapTXTest5 {

    @RepeatedTest(100)
    public void testTXLinkedListSingleton() {
        TXHashMap<Integer, String> HM = new TXHashMap<>();
        Integer zero = 0;
        assertEquals(false, HM.containsKey(zero));
        assertEquals(null, HM.get(zero));
        String zeroS = "zero";
        String empty = "";
        assertEquals(null, HM.put(zero, zeroS));
        assertEquals(zeroS, HM.put(zero, empty));
        assertEquals(true, HM.containsKey(zero));
        assertEquals(empty, HM.get(zero));
        Integer one = 1;
        String oneS = "one";
        Integer two = 2;
        String twoS = "two";
        assertEquals(null, HM.put(one, oneS));
        assertEquals(null, HM.put(two, twoS));
        assertEquals(true, HM.containsKey(one));
        assertEquals(oneS, HM.get(one));
        assertEquals(true, HM.containsKey(two));
        assertEquals(twoS, HM.get(two));
        assertEquals(null, HM.remove(-1));
        assertEquals(twoS, HM.remove(two));
        assertEquals(null, HM.remove(two));
        assertEquals(empty, HM.remove(zero));
        assertEquals(null, HM.remove(zero));
        assertEquals(oneS, HM.remove(one));
        assertEquals(null, HM.remove(one));
        assertEquals(false, HM.containsKey(one));
        assertEquals(null, HM.put(one, oneS));
        assertEquals(oneS, HM.remove(one));
        assertEquals(null, HM.put(zero, zeroS));
        assertEquals(zeroS, HM.put(zero, zeroS));
        assertEquals(zeroS, HM.remove(zero));
        assertEquals(null, HM.put(zero, zeroS));
        assertEquals(true, HM.containsKey(zero));
        assertEquals(zeroS, HM.get(zero));



    }

    @RepeatedTest(100)
    public void testLinkedListSingletonMultiThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TXHashMap<Integer, String> HM = new TXHashMap<>();
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

    	TXHashMap<Integer, String> HM;
        String threadName;
        CountDownLatch latch;

        Run(String name, CountDownLatch l, TXHashMap<Integer, String> hm) {
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
            String b_2 = threadName + "-b_2";
            String c = threadName + "-c";
            Integer k_a = 10 + threadName.charAt(1);
//			 System.out.println(threadName + ": " + k_a);
            Integer k_b = 20 + threadName.charAt(1);
//			 System.out.println(threadName + ": " + k_b);
            Integer k_c = 30 + threadName.charAt(1);
//			 System.out.println(threadName + ": " + k_c);

//			 System.out.println(threadName + ": put(k_a, a)");
            assertEquals(null, HM.put(k_a, a));
//			 System.out.println(threadName + ": put(k_a, a)");
            assertEquals(a, HM.put(k_a, a));
//			 System.out.println(threadName + ": containsKey(k_c)");
            assertEquals(false, HM.containsKey(k_c));
//			 System.out.println(threadName + ": put(k_c, c)");
            assertEquals(null, HM.put(k_c, c));
//			 System.out.println(threadName + ": containsKey(k_a)");
            assertEquals(true, HM.containsKey(k_a));
//			 System.out.println(threadName + ": containsKey(k_c)");
            assertEquals(true, HM.containsKey(k_c));
//			 System.out.println(threadName + ": containsKey(k_b)");
            assertEquals(false, HM.containsKey(k_b));
//			 System.out.println(threadName + ": put(k_b, b)");
            assertEquals(null, HM.put(k_b, b));
//			 System.out.println(threadName + ": containsKey(k_b)");
            assertEquals(true, HM.containsKey(k_b));
//			 System.out.println(threadName + ": containsKey(k_c)");
            assertEquals(true, HM.containsKey(k_c));
//			 System.out.println(threadName + ": put(k_c, c)");
            assertEquals(c, HM.put(k_c, c));
//			 System.out.println(threadName + ": remove(k_b)");
            assertEquals(b, HM.remove(k_b));
//			 System.out.println(threadName + ": remove(k_b)");
            assertEquals(null, HM.remove(k_b));
//			 System.out.println(threadName + ": remove(k_a)");
            assertEquals(a, HM.remove(k_a));
//			 System.out.println(threadName + ": remove(k_c)");
            assertEquals(c, HM.remove(k_c));
//			 System.out.println(threadName + ": remove(k_a)");
            assertEquals(null, HM.remove(k_a));
//			 System.out.println(threadName + ": remove(k_c)");
            assertEquals(null, HM.remove(k_c));
//			 System.out.println(threadName + ": containsKey(k_b)");
            assertEquals(false, HM.containsKey(k_b));
//			 System.out.println(threadName + ": containsKey(k_c)");
            assertEquals(false, HM.containsKey(k_c));
//			 System.out.println(threadName + ": put(k_a, a)");
            assertEquals(null, HM.put(k_a, a));
//			 System.out.println(threadName + ": put(k_b, b)");
            assertEquals(null, HM.put(k_b, b));
//			 System.out.println(threadName + ": containsKey(k_a)");
            assertEquals(true, HM.containsKey(k_a));
//			 System.out.println(threadName + ": put(k_b, b_2)");
            assertEquals(b, HM.put(k_b, b_2));
//			 System.out.println(threadName + ": get(k_a)");
            assertEquals(a, HM.get(k_a));
//			 System.out.println(threadName + ": containsKey(k_b)");
            assertEquals(true, HM.containsKey(k_b));
//			 System.out.println(threadName + ": get(k_b)");
            assertEquals(b_2, HM.get(k_b));
//			 System.out.println(threadName + ": remove(-1)");
            assertEquals(null, HM.remove(-1));
//			 System.out.println(threadName + ": remove(k_b)");
            assertEquals(b_2, HM.remove(k_b));
//			 System.out.println(threadName + ": remove(k_b)");
            assertEquals(null, HM.remove(k_b));
//			 System.out.println(threadName + ": put(k_c, c)");
            assertEquals(null, HM.put(k_c, c));
//			 System.out.println(threadName + ": remove(k_c)");
            assertEquals(c, HM.remove(k_c));
//			 System.out.println(threadName + ": remove(k_c)");
            assertEquals(null, HM.remove(k_c));
//			 System.out.println(threadName + ": remove(k_a)");
            assertEquals(a, HM.remove(k_a));
//			 System.out.println(threadName + ": remove(k_a)");
            assertEquals(null, HM.remove(k_a));
//			 System.out.println(threadName + ": containsKey(k_a)");
            assertEquals(false, HM.containsKey(k_a));
//			 System.out.println(threadName + ": put(k_a, a)");
            assertEquals(null, HM.put(k_a, a));
//			 System.out.println(threadName + ": remove(k_a)");
            assertEquals(a, HM.remove(k_a));

            assertEquals(null, HM.put(k_a, a));
            assertEquals(a, HM.get(k_a));
            assertEquals(null, HM.put(k_c, c));
            assertEquals(c, HM.get(k_c));
            assertEquals(null, HM.put(k_b, b));
            assertEquals(b, HM.get(k_b));
            assertEquals(c, HM.remove(k_c));
            assertEquals(a, HM.remove(k_a));
            assertEquals(b, HM.remove(k_b));
//			 System.out.println(threadName + ": end");
        }
    }

}
