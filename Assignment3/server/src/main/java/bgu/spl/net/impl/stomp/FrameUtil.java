package bgu.spl.net.impl.stomp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.Connections;

public class FrameUtil {
    public static final String STOMP_VERSION = "1.2";
    public static final Object HOST = "stomp.cs.bgu.ac.il";

    /*Response Frames*/
    //ReceiptFrame handeling
    public static void sendReceiptFrame(int connectionId,Connections<StompFrame> connections,String receiptId){
        connections.send(connectionId,makeReceiptFrame(receiptId));
    }
    private static StompFrame makeReceiptFrame(String receiptId){
        StompFrame re = new StompFrame("RECEIPT");
        re.addHeader("receipt-id", receiptId);
        return re;
    }
    //ErrorFrame handling
    public static void sendErrorFrame(StompFrame causedError,int connectionId,Connections<StompFrame> connections, String errorDesc){
        String[] error = errorDesc.split(":",2);
        connections.send(connectionId, makeErrorFrame(causedError,error[0],error[1]));

    }
    public static StompFrame makeErrorFrame(StompFrame causedError,String errorHeader,String errorBody){
        StompFrame result = new StompFrame("ERROR", createHeadersErr(errorHeader), createErrBody(causedError, errorBody));
        if(result.getReceiptId()!=null){
            result.addHeader("receipt", result.getReceiptId());
        }
        return result;
    }
    private static Map<String, String> createHeadersErr(String errorHeader){
        Map<String, String> errorHeaders = new ConcurrentHashMap<>();
        errorHeaders.put("message ",errorHeader);
        return errorHeaders;
    }
    private static String createErrBody(StompFrame frame, String errorExplain) {
        String errBody = "The message: \n";
        errBody += "----- \n";
        errBody += frame.toString();
        errBody += "----- \n";
        errBody += errorExplain;
        return errBody;
     }
    //ConnectedFrame handling
    public static void sendConnectedFrame(int connectionId,Connections<StompFrame> connections,String version){
        connections.send(connectionId, makeConnectedFrame(version));
    }
    private static StompFrame makeConnectedFrame(String version){
        StompFrame re = new StompFrame("CONNECTED");
        re.addHeader("version", version);
        return re;
    }
    //MessageFrame handling
    public static void sendMessageFrame(int connectionId,StompFrame msgToSend,int subId,Connections<StompFrame> connections){
        connections.send(connectionId, makeMessageFrame(msgToSend, subId,connections));
    }
    private static StompFrame makeMessageFrame(StompFrame msgToSend,int subId,Connections<StompFrame> connections){
        StompFrame msg = new StompFrame("MESSAGE");
        msg.setBody(msgToSend.getBody());
        msg.addHeader("message-id", String.valueOf(connections.getAndIncMessageCounter()));
        msg.addHeader("subscription", String.valueOf(subId));
        msg.addHeader("destination", msgToSend.getHeaderValueByName("destination"));
        return msg;
    }
}
