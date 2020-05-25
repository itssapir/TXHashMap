import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NIDS {


    public static void main(String[] args)
    {
        NIDS nids = new NIDS();
        Log l2Err = new Log(),fwErr = new Log(),sigErr= new Log(), okPkts= new Log();
        ExecutorService producerPool = Executors.newFixedThreadPool(1);
        ExecutorService consumerPool = Executors.newFixedThreadPool(4);
        ProducerConsumerPool<Packet> packetProducerConsumerPool = new ProducerConsumerPool<>(1000);
        Packet p = new Packet(1234,0,0);
        PacketInspector inspector = new PacketInspector(5);
        PacketInspector.PKI pki = inspector.pki;
//        PacketInspector.Firewall = inspector.new Firewall(true);
        Packet.PacketGenerator packetGenerator = p.packetGenerator;
        ArrayList<Packet> tbp= new ArrayList<>();
        try {
            for(int i = 0 ; i < 1000 ; i++ )
                tbp.add(packetGenerator.getRandomPacket(pki));
        } catch (Exception e) {
            e.printStackTrace();
        }
        (new Inbound(tbp,packetProducerConsumerPool)).run();
//        producerPool.execute(new Detector(l2Err,fwErr,sigErr, okPkts,1,10,pki,inspector.new Firewall(true),packetProducerConsumerPool));
        long start = System.currentTimeMillis(), stop, diff;
        for(int i=0 ; i < 4 ; i++)
            consumerPool.execute(new Detector(l2Err,fwErr,sigErr, okPkts,250,2,pki,inspector.new Firewall(true),packetProducerConsumerPool));
//        consumerPool.execute(new Detector(l2Err,fwErr,sigErr, okPkts,1,10,pki,inspector.new Firewall(true),packetProducerConsumerPool));
//        producerPool.shutdown();

        consumerPool.shutdown();
        try {
            consumerPool.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stop = System.currentTimeMillis();
        diff = stop - start;
        System.out.println("diff = " + diff);
        /*System.out.println("1111");
        printAll(l2Err);
        System.out.println("1111");
        printAll(okPkts);
        System.out.println("1111");
        printAll(sigErr);
        System.out.println("1111");
        printAll(fwErr);*/

//        Inbound ib = new Inbound()
    }

    private static void printAll(Log log) {
        if(!log.isEmpty())
        {
            int i = 0;
            do{
                System.out.println(log.read(i));

            }while(log.hasNext(i++));
        }
    }

}
