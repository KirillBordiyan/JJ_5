package org.example.messages;

public class BroadcastMessageRequest extends AbstractRequest{
    public static final String TYPE = "sendToAll";
    private String clientFrom;
    private String message;

    public BroadcastMessageRequest() {
        setType(TYPE);
    }

    public String getClientFrom() {
        return clientFrom;
    }

    public void setClientFrom(String clientFrom) {
        this.clientFrom = clientFrom;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
