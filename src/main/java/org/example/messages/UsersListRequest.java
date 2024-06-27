package org.example.messages;

public class UsersListRequest extends AbstractRequest {
    public static final String TYPE = "getUsers";

    private String message;
    private String recipient;

    public UsersListRequest() {
        setType(TYPE);
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
