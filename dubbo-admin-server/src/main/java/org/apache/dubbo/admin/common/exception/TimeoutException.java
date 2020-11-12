package org.apache.dubbo.admin.common.exception;


public class TimeoutException extends RuntimeException {

    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TimeoutException() {
    }

    public TimeoutException(String message) {
        this.message = message;
    }

    public TimeoutException(String message, String message1) {
        super(message);
        this.message = message1;
    }

    public TimeoutException(String message, Throwable cause, String message1) {
        super(message, cause);
        this.message = message1;
    }

    public TimeoutException(Throwable cause, String message) {
        super(cause);
        this.message = message;
    }

    public TimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, String message1) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.message = message1;
    }
}
