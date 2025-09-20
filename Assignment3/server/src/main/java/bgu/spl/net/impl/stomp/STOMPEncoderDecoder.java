package bgu.spl.net.impl.stomp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class STOMPEncoderDecoder implements MessageEncoderDecoder<StompFrame> {
    private enum DecodeState {
        READING_COMMAND,
        READING_HEADERS,
        READING_BODY
    }
    //members
    private DecodeState state; // the state of the decoder
    private StompFrame currentFrame;//the frame that will be generated
    //array for reading
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0; //length of bytes
    //constructor
    public STOMPEncoderDecoder(){
        this.state = DecodeState.READING_COMMAND;
    }
    //methods
    private String currentHeader;
    //private Map<String,String> map;
    @Override
    public StompFrame decodeNextByte(byte nextByte) {
        switch (state) {
            case READING_COMMAND:
                if(nextByte=='\n'){
                    currentFrame = new StompFrame(popString());
                    state = DecodeState.READING_HEADERS;
                }
                else{
                    pushByte(nextByte);
                }
                break;
            case READING_HEADERS:
                if(nextByte=='\n'){
                    if(len>0){
                        // If there's something in currentString, treat it as a header
                        currentFrame.addHeader(currentHeader, popString());
                    }
                    else // If currentString is empty, this is the blank line, so move to BODY
                        state = DecodeState.READING_BODY; 
                }
                else if(nextByte==':'){
                    //start accumulating the value insted of the header, save the value of the currentstring as headername
                    currentHeader = popString();
                }
                else{
                    pushByte(nextByte);;
                }
                break;
            case READING_BODY:
                if(nextByte == '\u0000'){
                    currentFrame.setBody(popString());
                    state = DecodeState.READING_COMMAND; //reset
                    return currentFrame;
                }
                else{
                    pushByte(nextByte);
                }
                break;
            default:
                break;
        }
        return null;
    }

    @Override
    public byte[] encode(StompFrame message) {
        System.out.println(message.toString());
        return message.toString().getBytes();
    }
    //bytearray functions from example
    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }

    private String popString() {
        //notice that we explicitly requesting that the string will be decoded from UTF-8
        //this is not actually required as it is the default encoding in java.
        String result = new String(bytes, 0, len, StandardCharsets.UTF_8);
        len = 0;
        return result;
    }
}
