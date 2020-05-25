import java.security.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class PacketInspector {

    enum FW_MATCH{ALLOW,DENY,NO_MATCH}
    PKI pki;
    ScoreBoard scoreBoard;

    public PacketInspector(int k) {
        this.pki = new PKI(k);
        this.scoreBoard = new ScoreBoard();
    }

    class PKI{
        Integer size;
        ArrayList<KeyPair> pki;

        public PKI(int K)
        {
            size = K;
            KeyPairGenerator keyPairGen;
            pki = new ArrayList<>();
            try {
                keyPairGen = KeyPairGenerator.getInstance("DSA");
                keyPairGen.initialize(2048);
                for (int i = 0 ; i < K ; i ++)
                {
                    KeyPair pair = keyPairGen.generateKeyPair();
                    pki.add(i,pair);
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        public KeyPair get(int idx)
        {
            return pki.get(idx);
        }

        public Integer getSize() {
            return size;
        }


        public byte[] sign(int id, byte[] message) throws Exception{
            KeyPair pair = pki.get(id);
            PrivateKey privKey = pair.getPrivate();
            Signature sign = Signature.getInstance("SHA256withDSA");
            sign.initSign(privKey);
            sign.update(message);
            return sign.sign();
        }

        public boolean verify (Packet.L5PLD msg) throws Exception
        {
            Signature sign = Signature.getInstance("SHA256withDSA");
//            byte[] signature = sign.sign();
            KeyPair pair = pki.get(msg.senderId);
            //Initializing the signature
            sign.initVerify(pair.getPublic());
            sign.update(msg.message);
            //Verifying the signature
            return sign.verify(msg.signature);
        }

    }

    class Firewall{

        ArrayList<Rule> firewalll;
        Rule defaultRule;

        public Firewall(boolean def) {
            this.firewalll = new ArrayList<>();
            this.defaultRule = new Rule(null,null,0,0,null,def);
        }

        class Rule{
            byte[] srcIpAddress, destIpAddress;
            int maskFrom, maskTo;
            String protocol;
            boolean action; // allow or deny


            public Rule(byte[] srcIpAddress, byte[] destIpAddress, int maskFrom, int maskTo, String protocol, boolean action) {
                this.srcIpAddress = srcIpAddress;
                this.destIpAddress = destIpAddress;
                this.maskFrom = maskFrom;
                this.maskTo = maskTo;
                this.protocol = protocol;
                this.action = action;
            }

            public FW_MATCH checkAgainstRule(byte[] pktSrcIpAddress, byte[] pktDestIpAddress, String protocol){
                if(maskFrom == 0 && maskTo == 0)
                    return (this.action)?FW_MATCH.ALLOW:FW_MATCH.DENY;
                if(matchAddress(pktSrcIpAddress,srcIpAddress,maskFrom))
                {
                    if(matchAddress(pktDestIpAddress,destIpAddress,maskTo))
                    {
                        if(protocol.equals(this.protocol))
                            return (this.action)?FW_MATCH.ALLOW:FW_MATCH.DENY;
                    }
                }
                return FW_MATCH.NO_MATCH;
            }

            private boolean matchAddress(byte[] packet_address, byte[] rule_address, int mask ){
                String adr = "", rule="";
                for(int i = 0 ; i < 4 ; i++)
                {
                    adr = adr.concat(String.format("%8s",Integer.toBinaryString(packet_address[i] & 0xff)).replace(' ', '0'));
                    rule = rule.concat(String.format("%8s",Integer.toBinaryString(rule_address[i] & 0xff)).replace(' ', '0'));
                }
                for(int i = 0 ; i < mask ; i++)
                {
                    if(adr.charAt(i)!=rule.charAt(i)) return false;
                }
                return true;
            }


        }


        public boolean checkRule(Packet packet)
        {
            if(1==1) return true;
            byte[] from, to;
            from = packet.l3Header.getSrcIP();
            to = packet.l3Header.getDestIP();
            String prot = packet.l4Header.getProtocol();
            for(Rule r: firewalll)
            {
                FW_MATCH res = r.checkAgainstRule(from,to,prot);
                switch (res) {
                    case ALLOW:
                        return true;
                    case DENY:
                        return false;
                    default: continue;
                }

            }
            return (defaultRule.checkAgainstRule(from,to,prot).equals(FW_MATCH.ALLOW));
        }


    }

    class ScoreBoard{
        HashSet<Long> okPackets = new HashSet<>();
        HashSet<Long> l2Err = new HashSet<>();
        HashSet<Long> l4Err = new HashSet<>();
        HashSet<Long> sigErr = new HashSet<>();
    }

    static boolean verifyLog(Log log, HashSet<Long> packets)
    {
        if(!log.isEmpty())
        {
            int i=0;
            do{
                if(!packets.remove(((Packet)log.read(i)).id))
                    return false;
            }while (log.hasNext(i++));
        }
        return packets.isEmpty();
    }
}
