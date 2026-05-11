package org.example.aiintegratedspringbootservice.exception;

public class FailedToGetAiResponseException extends RuntimeException {
    public FailedToGetAiResponseException(String message) {
        super(message);
    }

    public FailedToGetAiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
