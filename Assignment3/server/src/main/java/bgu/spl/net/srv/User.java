package bgu.spl.net.srv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class User<T> {
    private int connectionId;
    private String username;
    private String password;
    private boolean isLoggedIn;
    private ConnectionHandler<T> handler;
    private final ConcurrentHashMap<Integer,String> idToChannel;
    
    public User(int connectionId,String username,String password,ConnectionHandler<T> handler){
        this.connectionId = connectionId;
        this.username = username;
        this.password = password;
        this.isLoggedIn = true;
        this.handler = handler;
        this.idToChannel = new ConcurrentHashMap<>();
    }

    public boolean isLoggedIn(){
        return this.isLoggedIn;
    }
    
    public void setLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    public ConnectionHandler<T> getHandler() {
        return handler;
    }

    public void setHandler(ConnectionHandler<T> handler) {
        this.handler = handler;
    }

    public ConcurrentHashMap<Integer, String> getChannels(){
        return this.idToChannel;
    }

    public int getConnectionId() {
        return connectionId;
    }
    
    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getSubIdByChannel(String channel){
        for(Map.Entry<Integer,String> s:this.idToChannel.entrySet()){
            if(s.getValue().equals(channel)){
                return s.getKey();
            }
        }
        return -1;
    }
}
