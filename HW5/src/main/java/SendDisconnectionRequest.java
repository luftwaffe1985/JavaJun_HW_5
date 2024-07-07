public class SendDisconnectionRequest extends AbstractRequest {

    public static final String TYPE = "disconnectMessage";

    public SendDisconnectionRequest() {
        setType(TYPE);
    }
}