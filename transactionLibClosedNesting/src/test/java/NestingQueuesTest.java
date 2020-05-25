import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import static java.lang.System.exit;



public class NestingQueuesTest {
//    private static Random rand = new Random(1);
    static final boolean TEST_BEST_OF_BOTH = false;
    protected class Test1 implements Runnable{

            Queue q;

        public Test1(Queue q_ext){
                q = q_ext;
            }
            @Override
            public void run() {
//                try {
//                    latch.await();
//                } catch (InterruptedException exp) {
//                    System.out.println("InterruptedException");
//                }
                for (int i = 0; i < 1; i++) {
                    while (true) {
                        try {
                            try {
                                TX.TXbegin();
                                System.out.println("ext");
//                                q.enqueue(3);
                                innerTransaction();
                                System.out.println("ext");
                                try{
//                                    System.out.println(q.dequeue());
                                    System.out.println(q.dequeue());
                                }
                                catch (TXLibExceptions.QueueIsEmptyException e){
                                    System.out.println("Q is empty");
                                    TXLibExceptions excep = new TXLibExceptions();
                                    throw excep.new AbortException();
                                }
                                innerTransaction();
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

            private void innerTransaction() {
                try{
                    try {
                        TX.TXbegin();
                        System.out.println("inner");
                        try{
                            System.out.println(q.dequeue());
//                            q.enqueue(4);
//                            q.enqueue(5);
                            System.out.println(q.dequeue());
                        }
                        catch (TXLibExceptions.QueueIsEmptyException e){
                            System.out.println("Q is empty");
                        }
                    } finally {
                        TX.TXend();
                        System.out.println("finished inner");
                    }
                }
                catch(TXLibExceptions.AbortException exp) {
                    System.out.println("Thread: " + Thread.currentThread().getName() + " is aborting");
                }
            }

        }

    protected class Test2 implements Runnable {

        Queue q1;
        Queue q2;
        CountDownLatch cdl;

        public Test2(Queue q1_ext, Queue q2_ext, CountDownLatch c) {
            q1 = q1_ext;
            q2 = q2_ext;
            cdl = c;
        }

        @Override
        public void run() {
            try {
                cdl.await();
            } catch (InterruptedException exp) {
                System.out.println("InterruptedException");
            }
            for (int i = 0; i < 1; i++) {
                while (true) {
                    try {
                        try {
                            TX.TXbegin();
//                            System.out.println("ext");
                            for (int j = 0; j < 3; j++) innerTransaction(q1);
                            for (int j = 0; j < 3; j++)
                            {
                                if(TX.DEBUG_MODE_QUEUE) {
                                    System.out.println("starting inner " + j + " @ " + Thread.currentThread().getName());
                                }
                                innerTransaction(q2);
                            }
//                            try{
//                                System.out.println(q.dequeue());
//                                System.out.println(q.dequeue());
//                            }
//                            catch (TXLibExceptions.QueueIsEmptyException e){
//                                System.out.println("Q is empty");
//                                TXLibExceptions excep = new TXLibExceptions();
//                                throw excep.new AbortException();
//                            }
                        } finally {
                            TX.TXend();
                        }
                    } catch (TXLibExceptions.AbortException exp) {
                        if(TX.DEBUG_MODE_QUEUE) {
                            System.out.println(Thread.currentThread().getName() + " ext had aborted");
                        }
                        continue;
                    }
                    if(TX.DEBUG_MODE_QUEUE) {
                        System.out.println(Thread.currentThread().getName() + " ext had committed");
                    }
                    break;
                }
            }
        }


        private void innerTransaction(Queue q) {
            String tname = Thread.currentThread().getName();
            while(true){
                try {
                    try {
                        TX.TXbegin();
                        try {
                            if(TX.DEBUG_MODE_QUEUE) {
                                System.out.println(q.dequeue() + " by " + tname);
                            }
                            else
                            {
                                q.dequeue();
                            }
//                            q.enqueue(3);
                        } catch (TXLibExceptions.QueueIsEmptyException e) {
                            System.out.println("Q is empty @ " + tname );
                            if (q != q2) System.out.println("Q1 is empty @ " + tname);
                            System.exit(-2);
                        }
                    } finally {
                        TX.TXend();
//                        System.out.println("finished inner");
                    }
                } catch (TXLibExceptions.NestedAbortException exp) {
//                    System.exit(-3);
                    throw exp;
                } catch (TXLibExceptions.AbortException exp) {
                    if (TX.FLAT_NESTING) {
                        if(TX.DEBUG_MODE_QUEUE) System.out.println(tname + " aborted");
                        throw exp;
                    }
                    if (TX.CLOSED_NESTING) {
//                        System.out.println(tname + " was aborted");
                        continue;
                    }
                }
//                System.out.println(tname + " inner had committed");
                break;
            }
        }

    }

    protected class Test3 implements Runnable{
        Queue q;
        LinkedList l;
        CountDownLatch latch;
        Random rand;
        AbortRateCounter counter;

        protected Test3(Queue q_e, LinkedList l_e, CountDownLatch cdl, AbortRateCounter arc)
        {
            q = q_e;
            l = l_e;
            latch = cdl;
            counter = arc;
            rand = new Random();
        }


        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println("InterruptedException");
            }
            for (int i = 0; i < 5000; i++) {
                while (true) {
                    try {
                        try {
                            TX.TXbegin();
                            counter.parentTry();
                            for (int j = 0; j < 10; j++) {
                                if(TEST_BEST_OF_BOTH && TX.CLOSED_NESTING) // flat when (almost) no contention
                                    innerNotTxL();
                                else
                                    innerTransactionL();
                            }
                            for (int j = 0; j < 2; j++)
                            {
                                if(TX.DEBUG_MODE_QUEUE) {
                                    System.out.println("starting inner " + j + " @ " + Thread.currentThread().getName());
                                }
                                innerTransactionQ();
                            }


                        } finally {
                            TX.TXend();
                        }
                    } catch (TXLibExceptions.AbortException exp) {
                        if(TX.DEBUG_MODE_QUEUE) {
                            System.out.println(Thread.currentThread().getName() + " ext had aborted");
                        }
                        counter.parentAbort();
                        continue;
                    } catch ( TXLibExceptions.QueueIsEmptyException exp)
                    {
                        System.err.println("Queue is empty @ " + Thread.currentThread().getName());
                        System.exit(-2);
                    }
                    if(TX.DEBUG_MODE_QUEUE) {
                        System.out.println(Thread.currentThread().getName() + " ext had committed");
                    }
                    break;
                }
            }
        }


        private void innerTransactionQ() throws TXLibExceptions.QueueIsEmptyException {
            String tname ;
            if(TX.DEBUG_MODE_QUEUE) tname = Thread.currentThread().getName();
            while(true){
                try {
                    try {
                        TX.TXbegin();
                        counter.nestedTry();
                        try {
                            if(TX.DEBUG_MODE_QUEUE) {
                                System.out.println(q.dequeue() + " by " + tname);
                            }
                            else
                            {
                                q.dequeue();
                            }
                            for(int j = 0 ; j < 3 ; j++)
                            {
                                q.enqueue(j);
                            }
                        } catch (TXLibExceptions.QueueIsEmptyException e) {
                            throw e;
                        }
                    } finally {
                        TX.TXend();
                    }
                } catch (TXLibExceptions.NestedAbortException exp) {
                    throw exp;
                } catch (TXLibExceptions.AbortException exp) {
                    if (TX.FLAT_NESTING) {
                        if(TX.DEBUG_MODE_QUEUE) System.out.println(tname + " aborted");
                        counter.nestedAbort();
                        throw exp;
                    }
                    if (TX.CLOSED_NESTING) {
                        counter.nestedAbort();
                        continue;
                    }
                }
                break;
            }
        }

        private void innerTransactionL()
        {

            while (true) {
                try {
                    try {
                        TX.TXbegin();
                        counter.nestedTry();
                        innerNotTxL();
                        /*for (int i = 0; i < 3; i++) {
                            int action = rand.nextInt(2);
                            Integer node = rand.nextInt(5000);
                            if ((action & 1) == 0) { // add
                                l.putIfAbsent(node, node);
                            } else { // remove
                                l.remove(node);
                            }
                        }*/
                    } finally {
                        TX.TXend();
                    }
                } catch (TXLibExceptions.NestedAbortException exp) {
                    throw exp;
                } catch (TXLibExceptions.AbortException exp) {
                    if (TX.FLAT_NESTING) {
                        counter.nestedAbort();
                        throw exp;
                    }
                    if (TX.CLOSED_NESTING) {
                        counter.nestedAbort();
//                        if(TX.DEBUG_MODE_QUEUE)
                        continue;
                    }
                }
                break;
            }
        }

        private void innerNotTxL()
        {
            for (int i = 0; i < 3; i++) {
                int action = rand.nextInt(2);
                Integer node = rand.nextInt(50000);
                if ((action & 1) == 0) { // add
                    l.putIfAbsent(node, node);
                } else { // remove
                    l.remove(node);
                }
            }
        }

    }

    protected static void runT2() throws InterruptedException{
        NestingQueuesTest nqt = new NestingQueuesTest();
        CountDownLatch latch = new CountDownLatch(1);
        final int nThreads = 8;
        long start, stop, diff;
        ArrayList<Thread> threads = new ArrayList<>(nThreads);
        ArrayList<Queue> q_array = new ArrayList<>();
        for(int j = 0 ; j <= nThreads ; j ++) q_array.add(new Queue());
//        TX.TXbegin();
        for (int i = 1 ; i < 200 ; i++) {
            for(int j = 0 ; j <= nThreads; j ++) q_array.get(j%nThreads).enqueue(200*j + i);
            for(int j = 0 ; j <3 ; j++) q_array.get(nThreads).enqueue(-1*i - 50*j);
        }
//        TX.TXend();
//        for(Queue q: q_array)
//        {
//            assert q.getSize() >=25:"problem with enq";
//        }
        for (int i = 0 ; i < nThreads ; i++ ) {
            threads.add(new Thread(nqt.new Test2(q_array.get(i),q_array.get(nThreads), latch)));
//            threads.add(new Thread(nqt.new Test2(q_array.get(i%4),q_array.get(3), latch)));
        }
        for (Thread t:threads) {
            if(TX.DEBUG_MODE_QUEUE) System.out.println(t.getName() + " was created");
            t.start();
        }
        start = System.currentTimeMillis();
        latch.countDown();
        for (Thread t:threads) {
            t.join();
        }
        stop = System.currentTimeMillis();
        diff = stop - start;
        String NestingType = TX.CLOSED_NESTING?"Closed":"Flat";
        System.out.println("Done! diff = " + diff);
        try
        {
            String filename= "out_201218.txt";
            FileWriter fw = new FileWriter(filename,true); //the true will append the new data
            fw.write(NestingType+"\t" + nThreads + "\t" + diff +"\n");//appends the string to the file
            fw.close();
        }
        catch(IOException ioe)
        {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }

    protected static void runT1() throws TXLibExceptions.QueueIsEmptyException {
        NestingQueuesTest nqt = new NestingQueuesTest();
        Queue q = new Queue();
        q.enqueue(36);
        q.enqueue(37);
        q.enqueue(38);
        q.enqueue(39);
        q.enqueue(46);
        q.enqueue(47);
        q.enqueue(48);
        q.enqueue(49);
        q.enqueue(36);
        q.enqueue(37);
        q.enqueue(38);
        q.enqueue(39);
        q.enqueue(46);
        q.enqueue(47);
        q.enqueue(48);
        q.enqueue(49);
        assert q.getSize() == 16 : "Enq singletons failed";
        Test1 test1 = nqt.new Test1(q);
        test1.run();
        test1.run();
        TX.TXbegin();
        System.out.println(q.dequeue());
        TX.TXend();
    }


    protected  void test0() throws TXLibExceptions.QueueIsEmptyException {
        Queue q = new Queue();
        /*q.enqueue(36);
        q.enqueue(37);
        q.enqueue(38);
        q.enqueue(39);
        q.enqueue(46);
        q.enqueue(47);
        q.enqueue(48);
        q.enqueue(49);
        q.enqueue(36);
        q.enqueue(37);
        q.enqueue(38);
        q.enqueue(39);
        q.enqueue(46);
        q.enqueue(47);*/
        q.enqueue(48);
        q.enqueue(49);
        //assert q.getSize() == 16 : "Enq singletons failed";*/
        while(true) {
            try {
                try {
                    TX.TXbegin();
                    q.enqueue(12);
                    q.enqueue(17);
                } catch (TXLibExceptions.AbortException exp) {
                    continue;
                } finally {
                    TX.TXend();
                }
            }catch (TXLibExceptions.AbortException exp)
            {
                continue;
            }
            break;
        }

        while(true)
        {
            try {
                try {
                    TX.TXbegin();
                    try{
                        while(true)
                        {
                            try {
                                try {
                                    TX.TXbegin();
                                    System.out.println(q.dequeue());
                                    System.out.println(q.dequeue());
//                                    System.out.println(q.dequeue());
//                                    q.enqueue(15);
//                                    System.out.println(q.dequeue());
//                                    System.out.println(q.dequeue());
                                } catch (TXLibExceptions.AbortException exp) {
                                    continue;
                                } finally {
                                    TX.TXend();
                                }
                            }catch (TXLibExceptions.AbortException exp)
                            {
                                throw exp;
                            }
                            break;
                        }
                    } catch (TXLibExceptions.AbortException exp)
                    {
                        throw exp;
                    }
                    System.out.println(q.dequeue());
                } catch (TXLibExceptions.AbortException exp) {
                    continue;
                } finally {
                    TX.TXend();
                }
            }catch (TXLibExceptions.AbortException exp)
            {
                continue;
            }
            break;
        }
//        TX.TXbegin();
//        System.out.println(q.dequeue());
//        q.enqueue(3);
//        System.out.println(q.dequeue());
//        System.out.println(q.dequeue());
//        TX.TXend();
        System.out.println(q.dequeue());
    }

    private static void runT3() throws InterruptedException {
        Random rand = new Random(1);
        NestingQueuesTest nqt = new NestingQueuesTest();
        CountDownLatch latch = new CountDownLatch(1);
        LinkedList l = new LinkedList();
        final int nThreads = 8;
        long start, stop, diff;
        ArrayList<Thread> threads = new ArrayList<>(nThreads);
        ArrayList<Queue> q_array = new ArrayList<>();
        ArrayList<AbortRateCounter> abortCounters = new ArrayList<>(nThreads);
        AbortRateCounter accumulator = new AbortRateCounter();
        for(int j = 0 ; j < 3 ; j ++) q_array.add(new Queue());
        for (int i = 1 ; i < 2500 ; i++) {
            for(int j = 0 ; j <= nThreads; j ++) {
                q_array.get(j % 3).enqueue(200 * j + i);
                Integer kv = rand.nextInt(50000);
                l.put(kv,kv);
            }
//            for(int j = 0 ; j <3 ; j++) q_array.get(nThreads).enqueue(-1*i - 50*j);
        }
        for (int i = 0 ; i < nThreads ; i++ ) {
            abortCounters.add(new AbortRateCounter());
            threads.add(new Thread(nqt.new Test3(q_array.get(i%3),l, latch, abortCounters.get(i))));
        }
        for (Thread t:threads) {
            t.start();
        }
        start = System.currentTimeMillis();
        latch.countDown();
        for (Thread t:threads) {
            t.join();
        }
        stop = System.currentTimeMillis();
        diff = stop - start;
        for(AbortRateCounter counter:abortCounters)
        {
            accumulator.accumulate(counter);
        }
        handleRes(diff, nThreads, accumulator);
    }

    private static void handleRes(long diff, int nThreads, AbortRateCounter counter)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");

        String NestingType = TX.CLOSED_NESTING?(TEST_BEST_OF_BOTH?"BoB":"Closed"):"Flat";
        System.out.println("Done! diff = " + diff);
        try
        {
            String filename= "out_"+ sdf.format(new Date()) + ".txt";
            File f = new File(filename);
            if(!f.exists()) {
                FileWriter fw = new FileWriter(filename,true); //the true will append the new data
                fw.write("Nesting\tThreads\tTime\tParentAbortRate\tNestedAbortRate\n");//appends the string to the file
                fw.close();
            }
            FileWriter fw = new FileWriter(filename,true); //the true will append the new data
            fw.write(NestingType + "\t" + nThreads + "\t" + diff);
            fw.write("\t" + counter.ParentRate() + "\t" + counter.NestedRate());
            fw.write("\t" + counter.getParentTries() + "\t" + counter.getNestedTries() +"\n");//appends the string to the file
            fw.close();
        }
        catch(IOException ioe)
        {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }

    public static void main(String[] args) throws InterruptedException, TXLibExceptions.QueueIsEmptyException {
//        runT2();
//        runT3();
        TreeMap<Integer, Integer> map = new TreeMap();
//        map.put(1,1);
//        map.put(1,2);
        System.out.println(map.remove(1));
//        NestingQueuesTest nqt = new NestingQueuesTest();
//        nqt.test0();


    }


}
