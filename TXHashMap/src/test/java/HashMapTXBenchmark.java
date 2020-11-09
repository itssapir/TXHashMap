import org.junit.Test;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;

/*
 * TODO: Documentation
 * 
 */
public class HashMapTXBenchmark {

	DecimalFormat formatter = new DecimalFormat("####");
	enum Op {PUT, GET};
	static int warmupCycles = 10000;
	static int threadAmnt = 6;
    @Test
    public void benchMark() throws InterruptedException {
        
    	int numPerJobArr[] = {1,5,10,20,30,50,100};
    	int numJobsArr[]  = {10000, 50000, 100000, 500000};
    	int numAvg = 3;
    	int keyRange = 10000;
    	for (int numJobs : numJobsArr) {
        	for (int numPerJob : numPerJobArr) {
        		List<List<Pair<Integer, Integer>>> jobs = new ArrayList<>(); 
            	createJobs(jobs, numJobs, numPerJob, keyRange);
            	System.out.println(String.format("Running with numJobs = %d , numPerJob = %d , warmupCycles = %d", numJobs, numPerJob, warmupCycles));
            	runTXBenchmark(jobs, numAvg);
            	runOracleBenchmark(jobs, numAvg);
        	}
    	}	
    }

    private void runOracleBenchmark(List<List<Pair<Integer,Integer>>> jobs, int numAvg) throws InterruptedException {
		long putTime = 0;
		long getTime = 0;
		
		for (int iter=0; iter<numAvg; ++iter) {
			CountDownLatch latch = new CountDownLatch(1);
	        ConcurrentHashMap<Integer, Integer> HM = new ConcurrentHashMap<>();
	        warmup(HM);
	        
	        List<Thread> threads = new ArrayList<>();
	        AtomicInteger idx = new AtomicInteger(0);
	        ReentrantLock lock = new ReentrantLock();
	        for (int i = 0; i < threadAmnt; ++i) {
	        	threads.add(new Thread(new RunOracle(i, latch, Op.PUT, HM, jobs, idx, lock)));
	        }
	        
	        for (Thread t : threads) {
	        	t.start();
	        }
	        
	        long startTime = System.nanoTime();

	        latch.countDown();
	        
	        for (Thread t : threads) {
	        	t.join();
	        }

	        long stopTime = System.nanoTime();
	        putTime += (stopTime - startTime);
	        
	        threads.clear();
	        latch = new CountDownLatch(1);
	        
	        for (int i = 0; i < threadAmnt; ++i) {
	        	threads.add(new Thread(new RunOracle(i, latch, Op.GET, HM, jobs, idx, lock)));
	        }
	        
	        for (Thread t : threads) {
	        	t.start();
	        }
	        
	        startTime = System.nanoTime();

	        latch.countDown();
	        
	        for (Thread t : threads) {
	        	t.join();
	        }

	        stopTime = System.nanoTime();
	        getTime += (stopTime - startTime);

		}
        System.out.println("Oracle Put time: " + formatter.format(putTime/numAvg));
        System.out.println("Oracle Get time: " + formatter.format(getTime/numAvg));

	}

	private void runTXBenchmark(List<List<Pair<Integer,Integer>>> jobs, int numAvg) throws InterruptedException {
		long putTime = 0;
		long getTime = 0;
		
		for (int iter=0; iter<numAvg; ++iter) {
	    	CountDownLatch latch = new CountDownLatch(1);
	        TXHashMap<Integer, Integer> HM = new TXHashMap<>();
	        warmup(HM);
	        List<Thread> threads = new ArrayList<>();
	        AtomicInteger idx = new AtomicInteger(0);
	        for (int i = 0; i < threadAmnt; ++i) {
	        	threads.add(new Thread(new RunTX(i, latch, Op.PUT, HM, jobs, idx)));
	        }
	        
	        for (Thread t : threads) {
	        	t.start();
	        }
	        
	        long startTime = System.nanoTime();
	
	        latch.countDown();
	        
	        for (Thread t : threads) {
	        	t.join();
	        }
	
	        long stopTime = System.nanoTime();
	        putTime += (stopTime - startTime);
	        
	        threads.clear();
	        latch = new CountDownLatch(1);
	        
	        for (int i = 0; i < threadAmnt; ++i) {
	        	threads.add(new Thread(new RunTX(i, latch, Op.GET, HM, jobs, idx)));
	        }
	        
	        for (Thread t : threads) {
	        	t.start();
	        }
	        
	        startTime = System.nanoTime();
	
	        latch.countDown();
	        
	        for (Thread t : threads) {
	        	t.join();
	        }
	
	        stopTime = System.nanoTime();
	        getTime += (stopTime - startTime);
		}
        System.out.println("TX Put time: " + formatter.format(putTime/numAvg));
        System.out.println("TX Get time: " + formatter.format(getTime/numAvg));
	}

	private void createJobs(List<List<Pair<Integer, Integer>>> jobs, int numJobs, int numPerJob, int keyRange) {
		for (int i=0; i<numJobs; ++i) {
			List<Pair<Integer, Integer>> job = new ArrayList<>();
			for (int j=0; j<numPerJob; ++j) { 
				int key = ThreadLocalRandom.current().nextInt(0, keyRange);
				int value = ThreadLocalRandom.current().nextInt(0, 100);

				Pair<Integer, Integer> jobPair = new Pair<>(key, value);
				job.add(jobPair);
			}
			jobs.add(job);
		}
	}

	private void warmup(ConcurrentHashMap<Integer, Integer> hm) {
		for (int i=0; i < warmupCycles; ++i) {
			hm.put(i, i);
		}
		
		for (int i=0; i < warmupCycles; ++i) {
			hm.remove(i);
		}
	}

	private void warmup(TXHashMap<Integer, Integer> hm) {
		for (int i=0; i < warmupCycles; ++i) {
			hm.put(i, i);
		}
		
		for (int i=0; i < warmupCycles; ++i) {
			hm.remove(i);
		}
	}
	
	class RunOracle implements Runnable {

		ConcurrentHashMap<Integer, Integer> HM;
        String threadName;
        int threadNum;
        CountDownLatch latch;
        Op op;
        AtomicInteger idx;
        ReentrantLock lock;
        List<List<Pair<Integer,Integer>>> jobs;

        RunOracle(int tNum, CountDownLatch l, Op op, ConcurrentHashMap<Integer, Integer> hm, List<List<Pair<Integer,Integer>>> jobs, AtomicInteger idx, ReentrantLock lock) {
        	threadNum = tNum;
        	threadName = "T" + tNum;
            latch = l;
            this.op = op;
            HM = hm;
            this.idx = idx;
            this.lock = lock;
            this.jobs = jobs;

        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println(threadName + ": InterruptedException");
            }
            
            int i = idx.getAndIncrement();
            while (i < jobs.size()) {
            	List<Pair<Integer, Integer>> job = jobs.get(i);
            	lock.lock();
            	for (Pair<Integer, Integer> pair : job) {
            		if (op == Op.PUT) {
            			HM.put(pair.getFirst(), pair.getSecond());
            		} else {
            			HM.get(pair.getFirst());
            		}
            	}
            	lock.unlock();
            	i = idx.getAndIncrement();
            }

        }
    }
	
	  class RunTX implements Runnable {
	
	        TXHashMap<Integer, Integer> HM;
	        String threadName;
	        int threadNum;
	        CountDownLatch latch;
	        Op op;
	        AtomicInteger idx;
	        List<List<Pair<Integer,Integer>>> jobs;

	        RunTX(int tNum, CountDownLatch l, Op op, TXHashMap<Integer, Integer> hm, List<List<Pair<Integer,Integer>>> jobs, AtomicInteger idx) {
	        	threadNum = tNum;
	        	threadName = "T" + tNum;
	            latch = l;
	            this.op = op;
	            HM = hm;
	            this.idx = idx;
	            this.jobs = jobs;
	        }
	        
	        @Override
	        public void run() {
	            try {
	                latch.await();
	            } catch (InterruptedException exp) {
	                System.out.println(threadName + ": InterruptedException");
	            }
	            
	            int i = idx.getAndIncrement();
	            while (i < jobs.size()) {
	            	List<Pair<Integer, Integer>> job = jobs.get(i);
		            while (true) {
		                try {
		                    try {
		                        TX.TXbegin();
		                    	for (Pair<Integer, Integer> pair : job) {
		                    		if (op == Op.PUT) {
		                    			HM.put(pair.getFirst(), pair.getSecond());
		                    		} else {
		                    			HM.get(pair.getFirst());
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
	            	i = idx.getAndIncrement();
	            }
	
	        }
	    }
	

}
