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
public class HashMapTXTest4 {

    @RepeatedTest(100)
    public void testHashMapResize() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TXHashMap<Integer, String> HM = new TXHashMap<>();
 
        int threadAmnt = 32;
        int keysPerThread = 80;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadAmnt; ++i) {
        	threads.add(new Thread(new Run(i, latch, HM, keysPerThread)));
        }
        
        for (Thread t : threads) {
        	t.start();
        }
       
        latch.countDown();
        
        for (Thread t : threads) {
        	t.join();
        }
        
        
        // validate hashMap contents
        while (true) {
            try {
                try {
                    TX.TXbegin();
                    for (int tNum = 0; tNum < threadAmnt; ++tNum) {
                    	String threadName = "T"+tNum;
                    	for (int j=0; j<keysPerThread; ++j) {
                    		int key = 100*j + tNum;
                    		assertEquals(threadName, HM.get(key));
                    	}
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

    class Run implements Runnable {

        TXHashMap<Integer, String> HM;
        String threadName;
        int threadNum;
        CountDownLatch latch;
        Integer numKeys;

        Run(int tNum, CountDownLatch l, TXHashMap<Integer, String> hm, Integer numK) {
        	threadNum = tNum;
        	threadName = "T" + tNum;
            latch = l;
            HM = hm;
            numKeys = numK;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println(threadName + ": InterruptedException");
            }
            List<Integer> keys = new ArrayList<>();
            for(int i = 0 ; i < numKeys ; ++i) {
            	keys.add( 100*i + threadNum);
            }

            while (true) {
                try {
                    try {
                        TX.TXbegin();
                        for (int key : keys ) {
                            assertEquals(null, HM.put(key, threadName));
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
