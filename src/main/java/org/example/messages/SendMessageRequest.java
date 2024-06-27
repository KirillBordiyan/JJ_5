package org.example.messages;

public class SendMessageRequest extends AbstractRequest{
    public static final String TYPE = "sendMessage";
    private String recipient;
    private String clientFrom;
    private String message;

    public SendMessageRequest() {
        setType(TYPE);
    }

    public String getClientFrom() {
        return clientFrom;
    }

    public void setClientFrom(String clientFrom) {
        this.clientFrom = clientFrom;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
