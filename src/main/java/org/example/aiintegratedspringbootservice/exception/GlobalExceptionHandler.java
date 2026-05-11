package org.example.aiintegratedspringbootservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(EmptyResponseException.class)
    public ErrorResponse handleEmptyResponse(EmptyResponseException e) {
        log.error("Empty response error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(FailedToGetAiResponseException.class)
    public ErrorResponse handleFailedToGetResponse(FailedToGetAiResponseException e) {
        log.error("AI service error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(InvalidResponseFormatException.class)
    public ErrorResponse handleInvalidResponseFormat(InvalidResponseFormatException e) {
        log.error("Invalid response error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGeneralError(Exception e) {
        log.error("Unexpected error", e);
        return new ErrorResponse("Unexpected error occurred.");
    }

    public record ErrorResponse(String message) {}
}
