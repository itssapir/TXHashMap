import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class SyntheticForPPoPP {
    Integer ext_ops;
    Integer keyRange;
    Integer nListOps;
    Integer nTXs;
    Integer qOps;
    Integer nThreads;
    Boolean BoB;

    public SyntheticForPPoPP(Integer ext_ops, Integer keyRange, Integer nListOps, Integer qOps,Integer nTXs, Integer nThreads, Boolean boB) {
        this.ext_ops = ext_ops;
        this.keyRange = keyRange;
        this.nListOps = nListOps;
        this.qOps = qOps;
        this.nTXs = nTXs;
        this.nThreads = nThreads;
        this.BoB = boB;
    }


    protected class Test1 implements Runnable{

        LinkedList list;
        Queue q;
        CountDownLatch latch;
        Random rand;
        AbortRateCounter counter;
        Integer nTXs;

        public Test1(LinkedList list, Queue q, CountDownLatch latch, Random rand, AbortRateCounter counter, Integer nTXs) {
            this.list = list;
            this.q = q;
            this.latch = latch;
            this.rand = rand;
            this.counter = counter;
            this.nTXs = nTXs;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println("InterruptedException");
            }
            for (int i = 0; i < nTXs; i++) {
                while (true) {
                    try {
                        try {
                            TX.TXbegin();
                            for (int j = 0; j < ext_ops; j++) {
                                counter.parentTry();
//                            for (int j = 0; j < 10; j++) {
                                if (TX.FLAT_NESTING || (BoB && TX.CLOSED_NESTING)) // flat when (almost) no contention
                                    listOp();
                                else
                                    innerTransactionL();
//                            }
//                            for (int j = 0; j < qOps; j++)
//                            {
                               /* if(TX.DEBUG_MODE_QUEUE) {
                                    System.out.println("starting inner " + j + " @ " + Thread.currentThread().getName());
                                }*/
                                if (TX.CLOSED_NESTING) {
                                    innerTransactionQ();
                                } else {
                                    qOp();
                                }
//                            }

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
                            qOp();
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
                        listOp();
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

        private void listOp()
        {
            for (int i = 0; i < nListOps; i++) {
                int action = rand.nextInt(3);
                Integer node = rand.nextInt(keyRange);
                switch (action)
                {
                    case 0:
                        list.putIfAbsent(node, node);
                        break;
                    case 1:
                        list.remove(node);
                        break;
                    default: list.containsKey(node);
                }
                /*if ((action & 1) == 0) { // add
                    list.putIfAbsent(node, node);
                } else { // remove
                    list.remove(node);
                }*/
            }
        }

        private void qOp() throws TXLibExceptions.QueueIsEmptyException {
            for(int i=0 ; i <qOps ; i++) {
                if (rand.nextBoolean()) {
                    q.dequeue();
                } else {
                    q.enqueue(rand.nextInt(keyRange));
                }
//                q.enqueue(rand.nextInt(keyRange));
            }
        }

    }

    private void init(Queue q, LinkedList list, Random random)
    {
        int val;
        for(int i = 0 ; i < ext_ops*nTXs*qOps ; i++)
            q.enqueue(i);
        for(int i = 0 ; i < ext_ops*nTXs ; i++)
        {
            val = random.nextInt(keyRange);
            list.putIfAbsent(val,val);
        }
    }

    private void runTest(int seed) throws InterruptedException {
        Random rand = new Random(seed);
        CountDownLatch latch = new CountDownLatch(1);
        LinkedList l = new LinkedList();
        Queue q = new Queue();
        init(q,l,rand);
        long start, stop, diff;
        ArrayList<Thread> threads = new ArrayList<>(nThreads);
        ArrayList<AbortRateCounter> abortCounters = new ArrayList<>(nThreads);
        AbortRateCounter accumulator = new AbortRateCounter();
        int TxpT = nTXs / nThreads, add;
        for (int i = 0 ; i < nThreads ; i++ ) {
            abortCounters.add(new AbortRateCounter());
            add = (i < nTXs % nThreads)?1:0;
            threads.add(new Thread(new Test1(l,q, latch, new Random(seed+i), abortCounters.get(i),TxpT+add)));
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
        handleRes(diff,accumulator);
    }

    private void handleRes(long diff, AbortRateCounter counter)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
        String NestingType = TX.CLOSED_NESTING?(BoB?"BoB":"Closed"):"Flat";
        System.out.println("Done! diff = " + diff);
        try
        {
            String filename= "out_"+ sdf.format(new Date())+"_" + NestingType + ".txt";
            File f = new File(filename);
            if(!f.exists()) {
                FileWriter fw = new FileWriter(filename,true); //the true will append the new data
                fw.write("Nesting\tThreads\tTime\tParentAbortRate\tNestedAbortRate\tParentTry\tNestedTry\n");//appends the string to the file
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

    public static void main(String[] Args) throws InterruptedException {
        Integer ext_ops = 50;
        Integer keyRange = 50000;
        Integer nListOps = 10;
        Integer qOps = 5;
        Integer nThreads = 2;
        Integer nTXs = 10000;
        Boolean BoB = false;
        int seed = 2709;

        int i=0;
        String arg;
        while ( i< Args.length) {
            if(Args[i].charAt(0) == '-') {
                arg = Args[i++];
                //check options
                if(arg.equals("-t")) {
                    nThreads = Integer.parseInt(Args[i++]);
                }
                else if(arg.equals("-k")) {
                    keyRange = Integer.parseInt(Args[i++]);
                }
                else if(arg.equals("-n")) {
                    nTXs = Integer.parseInt(Args[i++]);
                }
                else if(arg.equals("-q")) {
                    qOps = Integer.parseInt(Args[i++]);
                }
                else if(arg.equals("-l")) {
                    nListOps = Integer.parseInt(Args[i++]);
                }
                else if(arg.equals("-m")) {
                    ext_ops = Integer.parseInt(Args[i++]);
                }
                else if(arg.equals("-b")) {
                    BoB = Boolean.parseBoolean(Args[i++]);
                }
                else if(arg.equals("-s")) {
                    seed = Integer.parseInt(Args[i++]);                }
                else {
                    System.out.println("Non-option argument: " + Args[i]);
                    System.exit(-1);
                }
            }
        }
        SyntheticForPPoPP sfp = new SyntheticForPPoPP(ext_ops, keyRange, nListOps, qOps,nTXs, nThreads, BoB);
        /*for(int k=0 ; k <10 ; k++)*/sfp.runTest(seed);

    }
}
