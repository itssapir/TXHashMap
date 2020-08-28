public class Detector implements Consumer<Packet>, Runnable{

    Log l2Err,fwErr,sigErr, okPkts;
    int nPkts, nPktsPerTx;
    PacketInspector.PKI pki;
    PacketInspector.Firewall firewall;
    ProducerConsumerPool<Packet> packetProducerConsumerPool;
    final boolean LOG_NESTED = true;

    public Detector(Log l2Err, Log fwErr, Log sigErr, Log okPkts, int nPkts, int nPktsPerTx, PacketInspector.PKI pki, PacketInspector.Firewall firewall, ProducerConsumerPool<Packet> packetProducerConsumerPool) {
        this.l2Err = l2Err;
        this.fwErr = fwErr;
        this.sigErr = sigErr;
        this.okPkts = okPkts;
        this.nPkts = nPkts;
        this.nPktsPerTx = nPktsPerTx;
        this.pki = pki;
        this.firewall = firewall;
        this.packetProducerConsumerPool = packetProducerConsumerPool;
    }

    @Override
    public void consume(Packet pkt) {

        if(pkt.l2Header.checkSum != Packet.getCheckSum(pkt.pld.message))
        {
            if(Log.DBG_LOG) System.out.println("logging " + pkt.id + "to l2");
            logWrapper(l2Err, pkt.id);
            return;
        }

        if(!firewall.checkRule(pkt))
        {
            if(Log.DBG_LOG) System.out.println("logging " + pkt.id + "to fw");
            logWrapper(fwErr, pkt.id);
            return;
        }

        try {
            for(int i=0 ; i < 5 ; i ++)
                if(!pki.verify(pkt.pld))
                {
                    if(Log.DBG_LOG) System.out.println("logging " + pkt.id + "to sig");
                    logWrapper(sigErr, pkt.id);
                    return;
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(Log.DBG_LOG) System.out.println("logging " + pkt.id + "to ok");
        logWrapper(okPkts, pkt.id);
    }

    private void logWrapper(Log log, Long id) {
        LocalStorage localStorage = TX.lStorage.get();
        if(TX.CLOSED_NESTING  && LOG_NESTED)
        {
            if(Log.DBG_LOG) System.out.println("log nested " + id);
            logNested(log,id);
        }
        else {
            if(Log.DBG_LOG) System.out.println("log unnested " + id);
            logUnNested(log, id);
        }
    }

    public void logNested(Log log, Long idx)
    {
        while(true)
        {
            try{
                try {
                    TX.TXbegin();
                    log.append(idx);
                } catch (TXLibExceptions.AbortException exp) {
                    if(TX.DEBUG_MODE_TX) System.out.println("abort");
                } finally {
                    TX.TXend();
                }
            }catch (TXLibExceptions.AbortException exp) {
                if(TX.DEBUG_MODE_TX) System.out.println("abort");
                continue;
            }
            break;
        }
    }

    public void logUnNested(Log log, Long idx)
    {
        log.append(idx);
    }

    @Override
    public void run() {
        int k = nPkts-nPkts%nPktsPerTx;
        for(int i = 0; i < k ; i+=nPktsPerTx)
        {
            while(true)
            {
                try{
                    try {
                        TX.TXbegin();
                        for(int j = 0 ; j < nPktsPerTx ; j++)
                            packetProducerConsumerPool.consume(this);
                    } catch (TXLibExceptions.AbortException exp) {
                        if(TX.DEBUG_MODE_TX) System.out.println("abort");
                    } finally {
                        TX.TXend();
                    }
                }catch (TXLibExceptions.AbortException exp) {
                    if(TX.DEBUG_MODE_TX) System.out.println("abort");
                    continue;
                }
                break;
            }


        }
        if(k != nPkts)
        {
            while(true)
            {
                try{
                    try {
                        TX.TXbegin();
                        for(int j = 0 ; j < nPkts - k ; j++)
                            packetProducerConsumerPool.consume(this);
                    } catch (TXLibExceptions.AbortException exp) {
                        if(TX.DEBUG_MODE_TX) System.out.println("abort");
                    } finally {
                        TX.TXend();
                    }
                }catch (TXLibExceptions.AbortException exp) {
                    if(TX.DEBUG_MODE_TX) System.out.println("abort");
                    continue;
                }
                break;
            }
        }

    }
}
