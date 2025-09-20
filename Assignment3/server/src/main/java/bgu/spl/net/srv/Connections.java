package bgu.spl.net.srv;

public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void send(String channel, T msg);

    void disconnect(int connectionId);

    ConnectionHandler<T> getHandler(int connectionId);

    void addConnection(int connectionId,ConnectionHandler<T> handler);

    void login(int connectionId,String userName,String pass);

    boolean isLegalInfo(String userName, String password);

    boolean isUserLoggedIn(String userName);

    void subscribe(String channel,int subscriptionId,int connectionId);

    void unsubscribe(int subId, int connectionId);

    boolean isChannel(String channel);

    boolean isHandlerSubbed(String channel,int connectionId);

    void closeConnection(int connectionId);

    int getAndIncMessageCounter();
}
