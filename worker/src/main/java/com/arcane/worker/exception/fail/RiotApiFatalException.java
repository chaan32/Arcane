package com.arcane.worker.exception.fail;

public class RiotApiFatalException extends RuntimeException {
    public RiotApiFatalException(String message, Throwable cause) {
        super(message, cause);
    }
}
