import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class SyntheticScenario {
    private static Random rand = new Random(1);

    public class TaskType1 implements Runnable{
        Queue q;
        CountDownLatch latch;

        public TaskType1(Queue q_ext, CountDownLatch cd){
            q = q_ext;
            latch = cd;
        }
        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println("InterruptedException");
            }
            for (int i = 0; i < 3; i++) {
                while (true) {
                    try {
                        try {
                            TX.TXbegin();
                            q.enqueue(3);
                            q.enqueue(5);
                            q.enqueue(7);
//                            innerTransaction();
                            try{
                                q.dequeue();
                                innerTransaction();
                                q.dequeue();
                            }
                            catch (TXLibExceptions.QueueIsEmptyException e){
                                System.out.println("Q is empty");
                            }
                        } finally {
                            TX.TXend();
                        }
                    } catch (TXLibExceptions.AbortException exp) {
//                        System.out.println("Thread: " + Thread.currentThread().getName() + " is aborting");
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
                try{
                    q.dequeue();
                    q.enqueue(1);

                }
                catch (TXLibExceptions.QueueIsEmptyException e){
                    System.out.println("Q is empty");
                }
                } finally {
                TX.TXend();
                }
            }
            catch(TXLibExceptions.AbortException exp) {
            System.out.println("Thread: " + Thread.currentThread().getName() + " is aborting");
            }
        }


    }

    public class TaskType2 implements Runnable{
        CountDownLatch latch;
        Queue q1 , q2;

        public TaskType2(Queue q1_ext, Queue q2_ext, CountDownLatch cd){
            q1 = q1_ext;
            q2 = q2_ext;
            latch = cd;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println(Thread.currentThread().getName() + ": InterruptedException");
            }
            while (true){
                try{
                    try{
                        int sum = 0;
                        TX.TXbegin();
                        for (int i = 0 ; i < 15 ; i++ ) q1.enqueue(rand.nextInt(90));
                        System.out.println("beginning outer TX @ " + Thread.currentThread().getName());
                        System.out.println("q1 len_out = " + q1.getSize());
                        for(int i= 0 ; i < 1 ; i ++ )
                        {
                            sum += innerTransaction1();
                        }
                        innerTransaction2(sum);
                    }finally {
                        System.out.println("finishing outer TX @ " + Thread.currentThread().getName());
                        TX.TXend();
                    }
                }catch (TXLibExceptions.AbortException exp) {
                    continue;
                }
                System.out.println("Finished TX @ " + Thread.currentThread().getName());
                break;
            }
        }
        int innerTransaction1(){
            int temp;
            while (true) {
                try {
                    try {
                        System.out.println("trying inner TX 1 @ " + Thread.currentThread().getName());
                        TX.TXbegin();
                        System.out.println("q1 len_in = " + q1.getSize());
                        if (q1.isEmpty()) {
                            System.out.println("queue is empty");
                        }
                        temp =  (Integer) q1.dequeue() + (Integer) q1.dequeue();
                    } finally {
//                        System.out.println("finishing inner TX @ " + threadName);
                        TX.TXend();
                    }
                } catch (TXLibExceptions.AbortException | TXLibExceptions.QueueIsEmptyException exp) {
                    continue;
                }
                break;
            }
            return temp;
        }

        void innerTransaction2(Integer num)
        {
            try{
                try {
                    System.out.println("trying inner TX 2 @ " + Thread.currentThread().getName());
                    TX.TXbegin();
                    try{
                        q2.enqueue(num);
                        q2.dequeue();
                    }
                    catch (TXLibExceptions.QueueIsEmptyException e){
                        System.out.println("Q is empty");
                    }
                } finally {
                    TX.TXend();
                }
            }
            catch(TXLibExceptions.AbortException exp) {
                System.out.println("Thread: " + Thread.currentThread().getName() + " is aborting @ iT2");
                throw exp;
            }
        }
    }

        void verifyEnqueue(Queue q1, Queue q2) throws TXLibExceptions.QueueIsEmptyException {
            String threadName = Thread.currentThread().getName();
            String a = threadName + "-a";
            String b = threadName + "-b";
            String c = threadName + "-c";
            assertEquals(a, q1.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(a, Q1.dequeue())");
            }
            assertEquals(b, q1.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(b, Q1.dequeue())");
            }
            assertEquals(c, q1.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(c, Q1.dequeue())");
            }
            assertEquals(a, q2.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(a, Q2.dequeue())");
            }
            assertEquals(b, q2.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(b, Q2.dequeue())");
            }
            assertEquals(c, q2.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(c, Q2.dequeue())");
            }
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("finished dequeue in multi test");
            }
        }


    public class TaskType3 implements Runnable {
        CountDownLatch latch;
        Queue q;

        public TaskType3(Queue q1_ext, CountDownLatch cd) {
            q = q1_ext;
            latch = cd;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println(Thread.currentThread().getName() + ": InterruptedException");
            }
            while (true){
                try{
                    try{
                        TX.TXbegin();
                        System.out.println("beginning outer TX @ " + Thread.currentThread().getName());
                        for(int i= 0 ; i < 2 ; i ++ )
                        {
                            innerTransaction();
                        }
                    }finally {
                        System.out.println("finishing outer TX @ " + Thread.currentThread().getName());
                        TX.TXend();
                    }
                }catch (TXLibExceptions.AbortException exp) {
                        continue;
                    }
                break;
                }


            }

            /*String threadName = Thread.currentThread().getName();
            String a = threadName + "-a";
            String b = threadName + "-b";
            String c = threadName + "-c";
            while (true) {
                try {
                    try {
                        TX.TXbegin();
                        if (!q.isEmpty()) {
                            System.out.println(threadName + ": queue is not empty");
                        }
                        q.enqueue(a);
                        q.enqueue(b);
                        q.enqueue(c);
                        q.enqueue(a);
                        q.enqueue(b);
                        q.enqueue(c);
                        try {
                            assertEquals(a, q.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(a, Q1.dequeue())");
                            }
                            assertEquals(b, q.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(b, Q1.dequeue())");
                            }
                            assertEquals(c, q.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(c, Q1.dequeue())");
                            }
                            assertEquals(a, q.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(a, Q2.dequeue())");
                            }
                            assertEquals(b, q.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(b, Q2.dequeue())");
                            }
                            assertEquals(c, q.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(c, Q2.dequeue())");
                            }
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("finished dequeue in multi test");
                            }
                        } catch (TXLibExceptions.QueueIsEmptyException exp) {
                            System.out.println(threadName + ": queue is empty exception");
                        }
                    } finally {
                        TX.TXend();
                    }
                } catch (TXLibExceptions.AbortException exp) {
                    continue;
                }
                break;
            }*/


        void innerTransaction() {
//            while (true) {
//                try {
                    String threadName = Thread.currentThread().getName();
                    String a = threadName + "-a";
                    String b = threadName + "-b";
                    String c = threadName + "-c";
                    while (true) {
                        try {
                            try {
                                TX.TXbegin();
                                System.out.println("beginning inner TX @ " + threadName);
                                if (!q.isEmpty() && TX.DEBUG_MODE_QUEUE) {
                                    System.out.println(threadName + ": queue is not empty");
                                }
                                q.enqueue(a);
                                q.enqueue(b);
                                q.enqueue(c);
                                q.enqueue(a);
                                q.enqueue(b);
                                q.enqueue(c);
                                try {
                                    verifyEnqueue(q);
                                } catch (TXLibExceptions.QueueIsEmptyException exp) {
                                    System.out.println(threadName + ": queue is empty exception");
                                }
                            } finally {
                                System.out.println("finishing inner TX @ " + threadName);
                                TX.TXend();
                            }
                        } catch (TXLibExceptions.AbortException exp) {
                            if (TX.FLAT_NESTING) {
                                throw exp;
                            }
                            if (TX.CLOSED_NESTING) {
                                continue;
                            }
                        }
//                            continue;
//                        }

                        break;
                    }
                    /*try {
                        TX.TXbegin();
                        for (int i = 0; i < 10; i++) {
                            int action = rand.nextInt(2);
                            Integer node = rand.nextInt(18);
                            if ((action & 1) == 0) { // add
                                ll.putIfAbsent(node, node);
                            } else { // remove
                                ll.remove(node);
                            }
                        }
                    } finally {
                        TX.TXend();
                    }
                } catch (TXLibExceptions.NestedAbortException exp) {
                    throw exp;
//                } catch(TXLibExceptions.AbortException exp){
//                    if (TX.FLAT_NESTING) {
//                        throw exp;
//                    }
//                    if (TX.CLOSED_NESTING) {
//                        continue;
//                    }
//                }
                        break;
                    }
                }
            }*/
        }

        protected void verifyEnqueue(Queue q) throws TXLibExceptions.QueueIsEmptyException {
            String threadName = Thread.currentThread().getName();
            String a = threadName + "-a";
            String b = threadName + "-b";
            String c = threadName + "-c";
            assertEquals(a, q.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(a, Q.dequeue())");
            }
            assertEquals(b, q.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(b, Q.dequeue())");
            }
            assertEquals(c, q.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(c, Q.dequeue())");
            }
            assertEquals(a, q.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(a, Q.dequeue())");
            }
            assertEquals(b, q.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(b, Q.dequeue())");
            }
            assertEquals(c, q.dequeue());
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("assertEquals(c, Q.dequeue())");
            }
            if(TX.DEBUG_MODE_QUEUE){
                System.out.println("finished dequeue in multi test");
            }
        }
    }

    public class TaskType4 implements Runnable{
        CountDownLatch latch;
        Queue q1 , q2;
        String threadName;

        public TaskType4(Queue q1_ext, Queue q2_ext, CountDownLatch cd){
            q1 = q1_ext;
            q2 = q2_ext;
            latch = cd;

        }


        @Override
        public void run() {
            threadName = Thread.currentThread().getName();
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println(threadName + ": InterruptedException");
            }

            String a = threadName + "-a";
            String b = threadName + "-b";
            String c = threadName + "-c";

            for (int i = 0; i < 1; i++) {
                while (true) {
                    try {
                        try {
                            TX.TXbegin();
//                            System.out.println("Thread:" + Thread.currentThread().getName() + " TxBegin");
                            for (int j = 0; j < 3 ; j++) {
                                innerTransaction();
                            }
                        } finally {
//                            System.out.println("Thread:" + Thread.currentThread().getName() + " TxEnd");
                            TX.TXend();
//                            break;
                        }
                    } catch (TXLibExceptions.AbortException exp) {
//                        System.out.println("abort");
                        continue;
                    }
                    break;
                }
            }

        }

        private void innerTransaction() {
            while (true) {
                try {
                    try {
                        TX.TXbegin();
                        for (int i = 0; i < 1; i++) {
                            int action = rand.nextInt(2);
                            Integer node = rand.nextInt(15);
                            if ((action & 1) == 0) { // add
                                for(int j = 1 ; j < 8 ; j ++ ){
                                    int sel = rand.nextInt(2);
                                    if (((sel & 1) == 0)) {
                                        System.out.println("s != 1");
                                        q1.enqueue(node);
                                    } else {
                                        System.out.println("s == 1");
                                        q2.enqueue(node);
                                    }
                                }
                            } else { // remove
                                for(int j = 0 ; j < 3 ; j++) {
                                    int sel = rand.nextInt(2);
                                    try {
                                        if (sel == 0) {
                                            q1.dequeue();
                                        }
                                        else
                                            q2.dequeue();
                                    }catch (TXLibExceptions.QueueIsEmptyException ignore){
                                        System.out.println("ignored");
                                }
                                }
                            }
                        }
                    } finally {
                        TX.TXend();
                    }
//                } catch (TXLibExceptions.NestedAbortException exp) {
//                    throw exp;
                }
                catch (TXLibExceptions.AbortException exp) {
                    if (TX.FLAT_NESTING) {
                        throw exp;
                    }
                    if (TX.CLOSED_NESTING) {
                        System.out.println("inner aborted");
                        try {
                            Integer d = rand.nextInt(10);
                            TimeUnit.MILLISECONDS.sleep(d);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                }
                System.out.println("inner done @ " + threadName);
                break;
            }

        }
    }

    public class TaskType5 implements Runnable {
        CountDownLatch latch;
        Queue q1, q2;
        String threadName;

        public TaskType5(Queue q1_ext, Queue q2_ext, CountDownLatch cd) {
            q1 = q1_ext;
            q2 = q2_ext;
            latch = cd;

        }

        @Override
        public void run() {
            threadName = Thread.currentThread().getName();
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println(threadName + ": InterruptedException");
            }

//            try{
                for(int i = 0 ; i < 25 ; i++)
                {
                    int count = 0;
                    while(true)
                    {
                        count++;
                        try{
                            try{
                                TX.TXbegin();
                                for(int j = 0 ; j < 300 ; j++){
                                    innerTransaction();
                                }
                            }finally {
                                TX.TXend();
                            }
                        }catch (TXLibExceptions.AbortException exp) {
                            continue;
                        }
                        System.out.println("ext count = " + count);
                        break;
                    }
                }
//            }
        }

        void innerTransaction()
        {
            String a = threadName + "-a";
            String b = threadName + "-b";
            String c = threadName + "-c";
            while (true) {
                try {
                    try {
                        TX.TXbegin();
//                        if (!q1.isEmpty()) {
//                            System.out.println(threadName + ": queue is not empty");
//                        }
                        q1.enqueue(a);
//                        q1.enqueue(b);
//                        q1.enqueue(c);
//                        q2.enqueue(a);
//                        q2.enqueue(b);
                        q2.enqueue(c);
                        if(TX.DEBUG_MODE_QUEUE){
                            System.out.println("finished enqueue in multi test");
                        }
                        /*try {
                            assertEquals(a, q1.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(a, Q1.dequeue())");
                            }
                            assertEquals(b, q1.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(b, Q1.dequeue())");
                            }
                            assertEquals(c, q1.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(c, Q1.dequeue())");
                            }
                            assertEquals(a, q2.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(a, Q2.dequeue())");
                            }
                            assertEquals(b, q2.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(b, Q2.dequeue())");
                            }
                            assertEquals(c, q2.dequeue());
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("assertEquals(c, Q2.dequeue())");
                            }
                            if(TX.DEBUG_MODE_QUEUE){
                                System.out.println("finished dequeue in multi test");
                            }
                        } catch (TXLibExceptions.QueueIsEmptyException exp) {
                            System.out.println(threadName + ": queue is empty exception");
                        }*/
                    } finally {
                        TX.TXend();
                    }
                } catch (TXLibExceptions.AbortException exp) {
                    continue;
                }
                break;
            }
//            try {
//                assertEquals(true, q1.isEmpty());
//            } catch (TXLibExceptions.AbortException exp) {
//                fail("singleton should not abort");
//            }
        }
            /////////
//            int count = 0;
//            while(true) {
//                count++;
//                try {
//                    try {
//                        TX.TXbegin();
//                        for (int i = 0; i < 1; i++) {
//                            q1.enqueue(new Integer(1));
//                            q2.enqueue(new Integer(2));
//                            q1.dequeue();
//                            q1.enqueue(q2.dequeue()); // this is legal
//                        }
//                    } finally {
//                        TX.TXend();
//                    }
//                }
//                catch (TXLibExceptions.NestedAbortException exp) {
//                    throw exp;
//                }
//                catch (TXLibExceptions.QueueIsEmptyException e) {
//                    e.printStackTrace();
//                }
//                catch (TXLibExceptions.AbortException exp) {
//                    if (TX.FLAT_NESTING) {
//                        throw exp;
//                    }
//                    if (TX.CLOSED_NESTING) {
//                        System.out.println("continuing inner, count = " + count);
//                        continue;
//                    }
//                }
//                System.out.println("count = " + count);
//                break;
//            }
//        }
    }

    public class TaskType7 implements Runnable{
        LinkedList l1, l2;
        CountDownLatch latch ;
        String threadName;

        public TaskType7(LinkedList l1_ext, LinkedList l2_ext, CountDownLatch cdl)
        {
            l1 = l1_ext;
            l2 = l2_ext;
            latch = cdl;
        }
        @Override
        public void run() {
            threadName = Thread.currentThread().getName();
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println("InterruptedException");
            }
            for (int i = 0; i < 10 ; i++) {
                while (true) {
                    try {
                        try {
                            TX.TXbegin();
                            innerTransaction();
                        }
                        finally {
                        TX.TXend();
                        }
                } catch (TXLibExceptions.AbortException exp) {
                    System.out.println("Thread: " + Thread.currentThread().getName() + " is aborting");
//                        throw exp;
                    continue;
                }
                break;
                }
            }
        }

        private void innerTransaction() {
            while (true) {
                try {
                    try {
                        TX.TXbegin();
                        for (int j = 0; j < 3; j++) {
//                                int key = rand.nextInt(30);
                            int action = rand.nextInt(2);
                            Integer node = rand.nextInt(8);
                            if ((action & 1) == 0) { // add
//                                    System.out.println("putting:" + node);
                                l1.putIfAbsent(node, node);
                            } else { // remove
//                                    System.out.println("removing:" + node);
                                l1.remove(node);
                            }
                            Integer n = rand.nextInt(15);
                            l1.put(n, n);
                            l1.put(n + 3, n + 3);
                            l1.put(n + 6, n + 6);
                            l2.put(n + 9, n + 9);
                            l1.put(n + 12, n + 12);
                            l1.put(n + 15, n + 15);
                            l1.put(n + 7, n + 7);
                            l2.put(n + 11, n + 11);
                            l1.remove(n + 7);
                            l2.remove(n + 9);
                            l2.remove(n + 11);
                            l1.remove(n + 15);
                        }
                    } finally {
                        TX.TXend();
                    }
                } catch (TXLibExceptions.AbortException exp) {
                    System.out.println("Thread: " + Thread.currentThread().getName() + " is aborting");
//                        throw exp;
                    continue;
                }
                break;
            }
        }
    }

    public class TaskType8 implements Runnable{
        LinkedList l1, l2;
        CountDownLatch latch ;
        String threadName;

        public TaskType8(LinkedList l1_ext, LinkedList l2_ext, CountDownLatch cdl)
        {
            l1 = l1_ext;
            l2 = l2_ext;
            latch = cdl;
        }
        @Override
        public void run() {
            threadName = Thread.currentThread().getName();
            try {
                latch.await();
            } catch (InterruptedException exp) {
                System.out.println("InterruptedException");
            }
            for (int i = 0; i < 3 ; i++) {
                while (true) {
                    try {
                        try {
                            TX.TXbegin();
                            innerTransaction();
                        }
                        finally {
                            TX.TXend();
                        }
                    } catch (TXLibExceptions.AbortException exp) {
                        System.out.println("Thread: " + Thread.currentThread().getName() + " is aborting");
//                        throw exp;
                        continue;
                    }
                    break;
                }
            }
        }

        private void innerTransaction() {
            while (true) {
                try {
                    try {
                        TX.TXbegin();
                        for (int j = 0; j < 3; j++) {
//                                int key = rand.nextInt(30);
                            LinkedList l = l2;
                            if(j % 2 == 0) l = l1;

                            int action = rand.nextInt(2);
                            Integer node = rand.nextInt(15000);
                            if ((action & 1) == 0) { // add
//                                    System.out.println("putting:" + node);
                                l.putIfAbsent(node, node);
                            } else { // remove
//                                    System.out.println("removing:" + node);
                                l.remove(node);
                            }
                        }
                    } finally {
                        TX.TXend();
                    }
                } catch (TXLibExceptions.AbortException exp) {
                    System.out.println("Thread: " + Thread.currentThread().getName() + " is aborting");
//                        throw exp;
                    continue;
                }
                break;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, TXLibExceptions.QueueIsEmptyException {
        SyntheticScenario s = new SyntheticScenario();
        CountDownLatch latch = new CountDownLatch(1);
        final int nThreads = 4;
        long start, stop, diff;
        ArrayList<Thread> threads = new ArrayList<>(nThreads);
        Queue q1 = new Queue(), q2 = new Queue(), q3 = new Queue(), q4 = new Queue();
        ArrayList<Queue> q_array = new ArrayList<>();
        q_array.add(q1);
        q_array.add(q2);
        q_array.add(q3);
        q_array.add(q4);
        for (int i = 0 ; i < nThreads ; i++ ) {
            threads.add(new Thread(s.new TaskType2(q_array.get(i%3),q_array.get(3), latch)));
        }
        for (Thread t:threads) {
            t.start();
        }
        latch.countDown();
        for (Thread t:threads) {
            t.join();
        }
        System.out.println(q1.dequeue());
        if(1 == 1)
            return;
        LinkedList ll1 = new LinkedList(), ll2 = new LinkedList();
        ArrayList<Integer> i_list = new ArrayList<>();
        for(int i = 0 ; i < 7000 ; i++)
        {
            i_list.add(i);
        }
        Collections.shuffle(i_list,rand);
        TX.TXbegin();
        for(int i = 0 ; i < 3201 ; i ++ ) {
            ll1.putIfAbsent(i_list.get(i), i_list.get(i));
            ll2.putIfAbsent(i_list.get(i+3201), i_list.get(i+3201));
        }
        TX.TXend();
//        Queue q1 = new Queue(), q2 = new Queue(), q3 = new Queue(), q4 = new Queue();

//        for(int i = 0 ; i < 5 ; i++) threads.add(new Thread(s.new TaskType1(ll, q, latch)));
//        for(int i = 0 ; i < 1 ; i++) threads.add(new Thread(s.new TaskType5(q2, q2, latch)));
//            threads.add(new Thread(s.new TaskType5(q2, q1, latch)));
//            threads.add(new Thread(s.new TaskType5(q2, q3, latch)));
//            threads.add(new Thread(s.new TaskType5(q2, q4, latch)));

//        for(int i = 0 ; i < 1 ; i++) threads.add(new Thread(s.new TaskType5(q1, q4, latch)));
//        for(int i = 0 ; i < nThreads/5 ; i++) threads.add(new Thread(s.new TaskType2(q3, q4, latch)));
//        for(int i = 0 ; i < 2*nThreads/5 ; i++) threads.add(new Thread(s.new TaskType3(q4, latch)));
//        threads.add(new Thread(s.new TaskType1(ll, latch)));
        for(int i = 0 ; i < 4 ; i++){
            System.out.println(i_list.get(i));
            System.out.println(i_list.get(i+3201));
        }
        for(int i = 0 ; i < 4 ; i++) threads.add(new Thread(s.new TaskType8(ll1, ll2, latch)));
        for(int i = 0 ; i < 4; i++) threads.get(i).start();
        latch.countDown();
        start = System.currentTimeMillis();
        for(int i = 0 ; i < 4 ; i++) threads.get(i).join();
        stop = System.currentTimeMillis();
        diff = stop - start;
//        System.out.println("validating...");
//        for(int i = 0 ; i < 5 ; i++) System.out.println(ll.get(i));
        System.out.println("Took: " + diff + " ms.");

    }
}

