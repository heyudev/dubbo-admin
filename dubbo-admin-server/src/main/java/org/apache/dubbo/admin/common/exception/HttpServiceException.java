package org.apache.dubbo.admin.common.exception;

/**
 *
 */
public class HttpServiceException extends RuntimeException {

    private int code;
    private String message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public HttpServiceException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public HttpServiceException(String message, int code, String message1) {
        super(message);
        this.code = code;
        this.message = message1;
    }

    public HttpServiceException(String message, Throwable cause, int code, String message1) {
        super(message, cause);
        this.code = code;
        this.message = message1;
    }

    public HttpServiceException(Throwable cause, int code, String message) {
        super(cause);
        this.code = code;
        this.message = message;
    }

    public HttpServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int code, String message1) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
        this.message = message1;
    }
}
