package bgu.spl.net.impl.stomp;

import java.io.IOException;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<StompFrame>{
    private int connectionId;
    private Connections<StompFrame> connections;
    private boolean shouldTerminate;
    @Override
    public void start(int connectionId, Connections<StompFrame> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
        this.shouldTerminate=false;
    }

    @Override
    public void process(StompFrame message) {
        String cmd = message.getCommand();
        System.err.println(message.toString());
        switch (cmd) {
            case "CONNECT":{
                handleConnect(message);
            break;}
            case "SEND":{
                handleSend(message);
                break;}
            case "SUBSCRIBE":{
                handleSubscribe(message);
                break;}
            case "UNSUBSCRIBE":{
                handleUnsubscribe(message);
                break;}
            case "DISCONNECT":{
                handleDisconnect(message);
                break;}
            default:
                handleError(message, "Unrecognized frame:Non supported frame supplied or error parsing");
                break;
        }
        
    }

    @Override
    public boolean shouldTerminate() {
        return this.shouldTerminate;
    }
    private void handleReceipt(StompFrame message){
        if(message.getReceiptId()!=null){
            FrameUtil.sendReceiptFrame(connectionId, connections, message.getReceiptId());
        }
    }
    //Connect Frame methods
    private void handleConnect(StompFrame message){
        String accept_version = message.getHeaderValueByName("accept-version");
        String host = message.getHeaderValueByName("host");
        String login = message.getHeaderValueByName("login");
        String passcode = message.getHeaderValueByName("passcode");
        boolean validLogin = true;
        try{
            checkAcceptVersion(accept_version);
            checkHost(host);
            checkLogin(login,passcode);
        }
        catch(IOException x){
            validLogin = false;
            handleError(message, x.getMessage());
        }
        if(validLogin){
            connections.login(connectionId,login, passcode);
            FrameUtil.sendConnectedFrame(connectionId, connections, accept_version);
            handleReceipt(message);
        }
    }
    private void checkHost(String host)throws IOException  {
        if (host==null) {
            throw new IOException("Frame missing host header:CONNECT frame must use host "+ FrameUtil.HOST);
        }else if (!host.equals(FrameUtil.HOST)) {
            throw new IOException("Frame host version does not match :CONNECT frame host version must be "+ FrameUtil.HOST);
        }
    }
    private void checkLogin(String login,String passcode) throws IOException{
        if(login!=null&&passcode!=null){
            if(!isLegaInfo(login,passcode)){
                throw new IOException("Wrong password:try other credenitals");
            }
            else if(isUserLogedIn(login)){
                throw new IOException("User already logged in:User " + login + " is logged in");
            }

        }
        else{
            throw new IOException("Username or passorwed header missing:You must provide both");
        }
    }
    private boolean isUserLogedIn(String userName) {
        return this.connections.isUserLoggedIn(userName);
    }
    private boolean isLegaInfo(String userName, String password) {
        return this.connections.isLegalInfo(userName, password);
    }
    private void checkAcceptVersion(String accept_version) throws IOException {
      if (accept_version==null) {
        throw new IOException("Frame missing accept-version header:CONNECT frame must use version 1.2");
      } else if (!accept_version.equals("1.2")) {
         throw new IOException("Frame's accept version doesnt match the server's :CONNECT frame must use version 1.2");
      }
    }
    //Error Frame Methods 
    public void handleError(StompFrame causedError,String errorDesc){
        FrameUtil.sendErrorFrame(causedError, connectionId, connections, errorDesc);
        connections.disconnect(connectionId);//logs out the user.
        connections.closeConnection(connectionId);
        this.shouldTerminate = true;
    }
    //Subscribe Frame methods
    private void handleSubscribe(StompFrame message){
        String dest = message.getHeaderValueByName("destination");
        if(dest.charAt(0)=='/')//for handling the channel name in case of a leading /
            dest = dest.substring(1);//the message is not modified only this string
        String id = message.getHeaderValueByName("id");
        boolean isValidSubscribe=true;
        try{
            checkDestination(dest);
            checkUniqueId(id);
        }
        catch(IOException x){
            isValidSubscribe = false;
            handleError(message, x.getMessage());
        }
        if(isValidSubscribe){
            connections.subscribe(dest, Integer.parseInt(id), connectionId);
            handleReceipt(message);
        }
    }
    private void checkDestination(String dest) throws IOException{
        if(dest == null){
            throw new IOException("Frame missing destination header:You must include the channel name which you want to subscribe to");
        }
    }
    private void checkUniqueId(String uId) throws IOException{//true if no other id in the client matches
        if(uId == null){
            throw new IOException("Frame missing id header:You must include the id of the subscription");
        }
        else if(connections.getHandler(connectionId).getUser().getChannels().containsKey(Integer.parseInt(uId))){
            throw new IOException("id is not unique:You are already subscribed to this channel");
        }
    }
    //Unsubscribe Frame methods
    private void handleUnsubscribe(StompFrame message){
        String id = message.getHeaderValueByName("id");
        boolean validUnsub = true;
        try{
            checkIdExistsInClient(id);
        }
        catch(IOException x){
            validUnsub = false;
            handleError(message, x.getMessage());
        }
        if(validUnsub){
            connections.unsubscribe(Integer.parseInt(id), connectionId);
            handleReceipt(message);
        }
    }
    private void checkIdExistsInClient(String id)throws IOException{//true if client indeed is subbed by this id to a channel
        if(id == null){
            throw new IOException("Frame missing Id header:You must specify which connection id to unsubscribe from");
        }
        else if(!connections.getHandler(connectionId).getUser().getChannels().containsKey(Integer.parseInt(id))){
            throw new IOException("You are not subscribed to this channel:You can only unsubscribe from channels you subsctibed to");
        }

    }
    //Send Frame Methods
    private void handleSend(StompFrame message){
        String dest = message.getHeaderValueByName("destination");
        if(dest.charAt(0)=='/')//for handling the channel name in case of a leading /
            dest = dest.substring(1);//the message is not modified only this string
        boolean validSend = true;
        try {
            checkDestination2(dest);
        } catch (IOException x) {
            validSend = false;
            handleError(message, x.getMessage());
        }
        if(validSend){
            connections.send(dest, message);
            handleReceipt(message);
        }
    }
    private void checkDestination2(String destination) throws IOException{
        if(destination==null){
            throw new IOException("Frame missing destination header:You must provide a channel name to send the message to");
        }
        else if(!this.connections.isChannel(destination)){
            throw new IOException("Channel does not exist:Send a message to an exsiting channel");
        }
        else if(!this.connections.isHandlerSubbed(destination, this.connectionId)){
            throw new IOException("Not subscribed to channel:You must be subscribed to a channel to send messages to it");
        }
    }
    //Disconnect Frame methods
    private void handleDisconnect(StompFrame message){
        if(message.getReceiptId()!=null){
            FrameUtil.sendReceiptFrame(this.connectionId, this.connections, message.getReceiptId());
            this.connections.disconnect(this.connectionId);
        }
        else{
            handleError(message, "Frame missing receipt header:You must provide a receipt header when trying to disconnect");
        }
    }
}
