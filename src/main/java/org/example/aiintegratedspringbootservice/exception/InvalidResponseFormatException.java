package org.example.aiintegratedspringbootservice.exception;

public class InvalidResponseFormatException extends RuntimeException {
    public InvalidResponseFormatException(String message) {
        super(message);
    }
}
