package bgu.spl.net.impl.stomp;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        if(args.length!=2){
            System.out.println("You must give two arguments, the port [1], the desired type of server (reactor/tpc) [2]");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        Connections<StompFrame> connections = new ConnectionsImpl();
        String type = args[1];
        if(type.equals("tpc")){
            Server.threadPerClient(port, ()->{
                return new StompMessagingProtocolImpl();
            }, ()->{
                return new STOMPEncoderDecoder();
            },connections).serve();
        }
        else if(type.equals("reactor")){
            Server.reactor(Runtime.getRuntime().availableProcessors(), port, ()->{
                return new StompMessagingProtocolImpl();
            }, ()->{
                return new STOMPEncoderDecoder();
            },connections).serve();
        }
        else{
            System.out.println("no available server type supplied");
            System.exit(1);
        }
    }
}
