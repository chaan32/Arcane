package com.arcane.worker.exception.fail;

public class TooManyRequestFail extends RuntimeException {
    public TooManyRequestFail(String message) {
        super(message);
    }
}
