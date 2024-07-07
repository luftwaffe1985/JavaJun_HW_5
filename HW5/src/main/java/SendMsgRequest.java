//  "type": "sendMessage",
//  "recipient: "luftwaffe1985",
//  "message": "text to luftwaffe1985"

public class SendMsgRequest extends AbstractRequest {

    public static final String TYPE = "sendMessage";

    private String recipient;
    private String message;

    public SendMsgRequest() {
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