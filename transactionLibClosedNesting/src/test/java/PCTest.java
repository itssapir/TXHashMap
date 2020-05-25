public class PCTest {

    static class Printer implements Consumer<String>
    {

        @Override
        public void consume(String val) {
            System.out.println(val);
        }
    }

    public static void f1()
    {
        ProducerConsumerPool<String> pool = new ProducerConsumerPool<>(5);
        Printer p = new Printer();
        while(true)
        {
            try{
                try {
                    TX.TXbegin();
                    pool.produce("hello");
                    pool.produce("hello from the outside");
                    pool.consume(p);
//                    pool.consume(p);
                    pool.produce("hello from the outside2");
                    pool.produce("hello from the outside3");
                    pool.produce("hello from the outside4");

                } catch (TXLibExceptions.AbortException exp) {
                    System.out.println("abort");
                } finally {
                    TX.TXend();
                }
            }catch (TXLibExceptions.AbortException exp) {
                System.out.println("abort");
                continue;
            }
            break;
        }

        while(true)
        {
            try{
                try {
                    TX.TXbegin();
                    pool.consume(p);
                    pool.consume(p);

                } catch (TXLibExceptions.AbortException exp) {
                    System.out.println("abort");
                } finally {
                    TX.TXend();
                }
            }catch (TXLibExceptions.AbortException exp) {
                System.out.println("abort");
                continue;
            }
            break;
        }
        pool.consume(p);
        pool.consume(p);
//        pool.consume(p);
    }

    public static void f2()
    {
        Log l = new Log();
        if(!l.isEmpty())
        {
            int i=0;
            do{
                System.out.println(l.read(i));

            }while (l.hasNext(i++));
        }
        while(true)
        {
            try{
                try {
                    TX.TXbegin();
                    l.append("hello");
                    l.append("hello1");
                    l.append("hello2");
                    l.append("hello3");
                    innerForLog(l);
                    System.out.println("End of nested " + l.read(6));
                } catch (TXLibExceptions.AbortException exp) {
                    System.out.println("abort");
                } finally {
                    TX.TXend();
                }
            }catch (TXLibExceptions.AbortException exp) {
                System.out.println("abort");
                continue;
            }
            break;
        }
        if(!l.isEmpty()) {
            int i = 0;
            do {
                while (true) {
                    try {
                        try {
                            TX.TXbegin();
                            System.out.println(l.read(i));

                        } catch (TXLibExceptions.AbortException exp) {
                            System.out.println("abort");
                        } finally {
                            TX.TXend();
                        }
                    } catch (TXLibExceptions.AbortException exp) {
                        System.out.println("abort");
                        continue;
                    }
                    break;
                }
            } while (l.hasNext(i++));
        }

    }

    public static void innerForLog(Log l)
    {
        while(true){
            try {
                try {
                    TX.TXbegin();
                    for(int j = 0 ; j < 3 ; j++)
                    {
                        System.out.println("inner");
                        System.out.println(l.read(j));
                        l.append(j);
                    }
                    } finally {
                    TX.TXend();
                }
            } catch (TXLibExceptions.NestedAbortException exp) {
                throw exp;
            } catch (TXLibExceptions.AbortException exp) {
                if (TX.FLAT_NESTING) {
                    throw exp;
                }
                if (TX.CLOSED_NESTING) {
                    continue;
                }
            }
            break;
        }
    }

    public static void main(String[] args) {
        f1();
        f2();

    }
}
