package app.common.network;

import java.io.Serializable;

/**
 * Объект ответа, отправляемый сервером клиенту
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ResponseStatus status;
    private final String message;
    private final Serializable payload;

    public Response(ResponseStatus status, String message, Serializable payload) {
        this.status = status;
        this.message = message;
        this.payload = payload;
    }

    public Response(ResponseStatus status, String message) {
        this(status, message, null);
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Serializable getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Response[" + status + ", message='" + message + "']";
    }
}