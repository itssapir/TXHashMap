import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

class Packet {
    L2Header l2Header;
    L3Header l3Header;
    L4Header l4Header;
    L5PLD pld;
    Long id;
    PacketGenerator packetGenerator;

    public Packet(L2Header l2Header, L3Header l3Header, L4Header l4Header, L5PLD pld, Long id) {
        this.l2Header = l2Header;
        this.l3Header = l3Header;
        this.l4Header = l4Header;
        this.pld = pld;
        this.id = id;
        packetGenerator = null;
    }

    public Packet(int seed, int cs, int sig)
    {
        this.l2Header = null;
        this.l3Header = null;
        this.l4Header = null;
        this.pld = null;
        this.id = null;
        packetGenerator = new PacketGenerator(seed,cs,sig);
    }

    public Packet()
    {
        this.l2Header = null;
        this.l3Header = null;
        this.l4Header = null;
        this.pld = null;
        this.id = null;
        packetGenerator = new PacketGenerator();
    }


    static Byte getCheckSum(byte[] msg)
    {
        Byte res = 0;
        for(byte b : msg)
        {
            res = (byte)(res ^ b);
        }
        return res;

    }

    public byte[] getSrcIp()
    {
        return l3Header.getSrcIP();
    }


    protected class PacketGenerator{
        int randomSeed = 123456;
        Random random;
        int csErrProb, sigErrProb; // percentage
        AtomicLong packetId = new AtomicLong(0);

        PacketGenerator(){
            random = new Random(randomSeed);
            csErrProb = sigErrProb = 50;
        }

        PacketGenerator(int seed, int cs, int sig){
            randomSeed = seed;
            random = new Random(seed);
            csErrProb = cs;
            sigErrProb = sig;
        }

        public int getRandomSeed() {
            return randomSeed;
        }

        public Packet getRandomPacket(PacketInspector.PKI pki) throws Exception{ //TODO: get scoreboard as well
            // Create L5 packet
            byte[] msg = new byte[256];
            int id = random.nextInt(pki.getSize());
            boolean sigerr = random.nextInt(100) < sigErrProb;
            random.nextBytes(msg);
            long pid = packetId.getAndIncrement();

            byte[] signature = pki.sign(id,msg);
            if(sigerr) {
                signature[0] = (byte) (signature[0] ^ (byte) 0xff);
            }

            L5PLD pld = new L5PLD(msg,id,signature,sigerr);

            // Create L4 packet
            L4Header l4 = new L4Header(random.nextBoolean()?"TCP":"UDP", random.nextInt(), random.nextInt());
            byte[] src = new byte[4];
            byte[] dest = new byte[4];
            random.nextBytes(src);
            random.nextBytes(dest);
            L3Header l3 = new L3Header(src,dest);
            byte cs = getCheckSum(msg);
            if( random.nextInt(100) < csErrProb) cs ^= (byte)1;
            L2Header l2Header = new L2Header(cs);
            return new Packet(l2Header,l3Header,l4Header,pld, pid);
        }



    }


    class L2Header{
        Byte checkSum;

        L2Header(Byte cs) {
           checkSum = cs;
        }

        public boolean checkSum(byte[] msg) {
            byte res = getCheckSum(msg);
            return checkSum.equals(res);
        }

    }

    class L3Header{
        byte[] srcIP;
        byte[] destIP;


        public L3Header()
        {
            srcIP = new byte[4];
            destIP = new byte[4];
            for(int i = 0 ; i < 4 ; i++)
            {
                srcIP[i] = (byte)0;
                destIP[i] = (byte)255;
            }
        }

        public L3Header(byte[] src , byte[] dest)
        {
            srcIP = src;
            destIP = dest;
        }

        public byte[] getSrcIP() {
            return srcIP;
        }
        public byte[] getDestIP() {
            return destIP;
        }
    }

    public class L4Header{
        String protocol;
        int srcPort , destPort;

        public L4Header()
        {
            protocol = "TCP";
            srcPort = destPort = 0;
        }

        public L4Header(String prot, int src , int dest)
        {
            protocol = prot;
            srcPort = src ;
            destPort = dest;
        }

        public String getProtocol() {
            return protocol;
        }
    }

    class L5PLD{
        byte[] message;
        int senderId;
        byte[] signature;
        boolean sigErr;

        public L5PLD(byte[] msg, int id, byte[] signature, boolean sigErr){
            this.senderId = id;
            this.message = msg;
            this.signature = signature;
            this.sigErr = sigErr;
        }

    }
}
