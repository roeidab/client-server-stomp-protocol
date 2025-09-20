package bgu.spl.net.impl.stomp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StompFrame {

	private String command; // e.g., CONNECT, SEND, SUBSCRIBE, etc.
	private Map<String, String> headersToValue;
	private String body;

	// Constructor
	public StompFrame(String command) {
    	this.command = command;
        this.headersToValue = new ConcurrentHashMap<>();
		this.body = "";
	}
	public StompFrame(String command,Map<String, String> headersToValue,String body){
		this.command = command;
		this.headersToValue = new ConcurrentHashMap<>(headersToValue);
		this.body = body;
	}
    //Add mapping for header/value
    public void addHeader(String header,String value){
        headersToValue.put(header, value);
    }
	public String getHeaderValueByName(String header){
		return headersToValue.get(header);
	}

	// Set the body
	public void setBody(String body) {
    	this.body = body;
	}

	// Get the body
	public String getBody() {
    	return body;
	}
    
    public String getCommand() {
    	return command;
	}
	public String toString(){
		String out = command+'\n';
		for(Map.Entry<String, String> entry : headersToValue.entrySet() ){
			out+=entry.getKey()+':'+entry.getValue()+'\n';
		}
		out+=body+'\n';
		out+='\u0000';
		return out;
	}

	public String getReceiptId (){
		if (headersToValue.containsKey("receipt")) {
			return headersToValue.get("receipt");	
		}
		else
			return null;
	}
}
