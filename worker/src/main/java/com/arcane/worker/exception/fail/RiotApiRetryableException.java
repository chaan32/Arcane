package com.arcane.worker.exception.fail;

public class RiotApiRetryableException extends RuntimeException {
    public RiotApiRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
