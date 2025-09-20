package bgu.spl.net.impl.stomp;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.User;

public class ConnectionsImpl implements Connections<StompFrame>{
    
    private ConcurrentHashMap<Integer, ConnectionHandler<StompFrame>> clients;
    private ConcurrentHashMap<String,Set<Integer>> subscribtions; 
    private ConcurrentHashMap<String,User<StompFrame>> users;//maps betweem usernames to their connection handlers of active clients
    /*maps between the channel to all clients subbed to it, handling of unique subid inside connetion handlers */
    private AtomicInteger msgCounter = new AtomicInteger(0);
    
    //constructor
    public ConnectionsImpl(){
        clients = new ConcurrentHashMap<>();
        subscribtions = new ConcurrentHashMap<>();
        users = new ConcurrentHashMap<>();
    }
    
    public boolean send(int connectionId, StompFrame msg){
        ConnectionHandler<StompFrame> client = clients.get(connectionId);
        if(client != null){
            client.send(msg);
            return true;
        }
        return false;    
    }

    public void send(String channel, StompFrame msg){
        Set<Integer> subbed = subscribtions.get(channel);
        for(Integer conId: subbed){
            ConnectionHandler<StompFrame> handler = clients.get(conId);
            int uniqueSubId = handler.getUser().getSubIdByChannel(channel);
            FrameUtil.sendMessageFrame(conId, msg, uniqueSubId, this);
        }
    } 

    public void disconnect(int connectionId){
        this.logout(connectionId);
    }

    public void login(int connectionId,String userName,String pass){
        ConnectionHandler<StompFrame> client = clients.get(connectionId);
        User<StompFrame> user;
        if(!users.containsKey(userName)){//if new username add new user
            user = new User<>(connectionId,userName,pass,client);
            this.users.put(userName, user);
        }
        else{//exsiting username set as active
            user = users.get(userName);
            user.setHandler(client);
            user.setLoggedIn(true);
            user.setConnectionId(connectionId);
        }
        client.setUser(user);
    }

    public void addConnection(int connectionId,ConnectionHandler<StompFrame> handler){
        clients.put(new Integer(connectionId),handler);
    }

    public ConnectionHandler<StompFrame> getHandler(int connectionId) {
        return this.clients.get(connectionId);
    }

    public boolean isLegalInfo(String userName, String password){//returns true if either the user is new or already registerd
        return (!this.users.containsKey(userName)) || (users.get(userName).getPassword().equals(password));
    }
    
    public boolean isUserLoggedIn(String userName){
        return users.containsKey(userName)&&users.get(userName).isLoggedIn();
    }

    public void logout(int connectionId){
        ConnectionHandler<StompFrame> handler = clients.get(connectionId);
        User<StompFrame> user = handler.getUser();
        if(user!=null){// if is logged in
             //unsubscribe from all
            Iterator<String> iter = user.getChannels().values().iterator();
            while(iter.hasNext()){
            String current = iter.next();
            subscribtions.get(current).remove(connectionId);          
        }
        //clear subscriptionsIds in the connection handler itself
        user.getChannels().clear();
        //set user to inactive
        user.setLoggedIn(false);
        user.setConnectionId(-1);
        user.setHandler(null);
        }
    }

    public void subscribe(String channel,int subscriptionId,int connectionId){
        /*
         * Adds the mapping between the channel and the connetion id.
         * Adds the mapping inside the ConnectionHandler between the uniqueSubId and the channel
         */
        subscribtions.computeIfAbsent(channel, k->new HashSet<Integer>());
        subscribtions.get(channel).add(connectionId);  
        (clients.get(connectionId)).getUser().getChannels().put(subscriptionId,channel);
    }

    public void unsubscribe(int subId, int connectionId){
        /*
         * get the channel name based on the subid in the client handler and remove it from the mapping
         * remove the connection id associated with it from the set mapped to channel name
         */
        String channel = (String)((ConnectionHandler<StompFrame>)clients.get(connectionId)).getUser().getChannels().remove(subId);
        subscribtions.get(channel).remove(connectionId);
    }

    public boolean isChannel(String channel){
        return subscribtions.containsKey(channel);
    }

    public boolean isHandlerSubbed(String channel,int connectionId){
        return (clients.get(connectionId)).getUser().getChannels().contains(channel);
    }

    public int getAndIncMessageCounter(){
        return this.msgCounter.getAndIncrement();
    }
    
    public void closeConnection(int connectionId){
        ConnectionHandler<StompFrame> handler = clients.remove(connectionId);
        handler.submitTask(()->{
            try {
                handler.close();
            }
            catch(IOException x){
                x.printStackTrace();
            }
        });
    }
}

