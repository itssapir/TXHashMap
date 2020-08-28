import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class RBTest {

    private class Test1 implements Runnable{

        RBTree t;
        CountDownLatch latch;
        private Test1(RBTree t, CountDownLatch latch) {
            this.t = t;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println("InterruptedException");
            }
            for (int i = 0; i < 1; i++) {
                while (true) {
                    try {
                        try {
                            TX.TXbegin();
                            System.out.println(t.get(2));

                            innerTransaction(t, 0);
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


    private static void innerTransaction(RBTree t, Integer key) {
        String tname = Thread.currentThread().getName();
        while(true){
            try {
                try {
                    TX.TXbegin();

                    t.put(key+2,key+2);

//                            q.enqueue(3);

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

    public static void main(String[] Args) throws InterruptedException {
        RBTree tree = new RBTree();
        CountDownLatch latch = new CountDownLatch(1);
        RBTest test = new RBTest();
        final int nThreads = 32;
        long start, stop, diff;
        ArrayList<Thread> threads = new ArrayList<>(nThreads);
        for (int i = 0 ; i < nThreads ; i++ ) {
            threads.add(new Thread(test.new Test1(tree, latch)));
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
        System.out.println("Done! diff = " + diff);
//        t.put(1,1);
//        System.out.println(t.get(1));
//        t.put(1,2);
//        System.out.println(t.get(1));
//        System.out.println(t.contains(1));
//        t.remove(1);
//        System.out.println(t.get(1));
//        System.out.println(t.contains(1));
        /*while(true){
            try {
                TX.TXbegin();
//                TX.TXbegin();
                tree.put(1,1);
//                t.remove(1);
//                TX.TXend();
                innerTransaction(tree,1);
                System.out.println(tree.get(1));
            } catch(TXLibExceptions.AbortException exp) {
                continue;
            }
            finally {
                TX.TXend();
            }
            break;
        }
        while(true){
            try {
                TX.TXbegin();
                tree.put(1,2);
//                t.remove(1);
                System.out.println(tree.get(1));
                System.out.println(tree.get(3));
            } catch(TXLibExceptions.AbortException exp) {
                continue;
            }
            finally {
                TX.TXend();
            }
            break;
        }
//        while(true){
//            try {
//                TX.TXbegin();
////                t.put(1,1);
//                t.remove(1);
//                System.out.println(t.get(1));
//            } catch(TXLibExceptions.AbortException exp) {
//                continue;
//            }
//            finally {
//                TX.TXend();
//            }
//            break;
//        }

        System.out.println(tree.get(1));*/
    }

}
