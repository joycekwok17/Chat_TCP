import java.io.Serializable;

public class ChatMessage implements Serializable {
    private String message;

    private int type;

    static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2;

    public ChatMessage() {
    }

    public ChatMessage(String message, int type) {
        this.message = message;
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
