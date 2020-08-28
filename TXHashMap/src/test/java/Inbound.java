import java.util.ArrayList;

public class Inbound implements Runnable{
//        int numPkts;
    ArrayList<Packet> packets;
    ProducerConsumerPool<Packet> packetProducerConsumerPool;

    public Inbound(ArrayList<Packet> packets, ProducerConsumerPool<Packet> packetProducerConsumerPool) {
        this.packets = packets;
        this.packetProducerConsumerPool = packetProducerConsumerPool;
    }

    @Override
    public void run() {
        for(Packet p : packets)
        {
            while(true)
            {
                try{
                    try {
                        TX.TXbegin();
                        packetProducerConsumerPool.produce(p);
                    } catch (TXLibExceptions.AbortException exp) {
                        if(TX.DEBUG_MODE_TX) System.out.println("abort");
                    } finally {
                        TX.TXend();
                    }
                }catch (TXLibExceptions.AbortException exp) {
                    if(TX.DEBUG_MODE_TX) System.out.println("abort");
                    continue;
                }
//                System.out.println("committed inb");
                break;
            }
        }
    }
}
