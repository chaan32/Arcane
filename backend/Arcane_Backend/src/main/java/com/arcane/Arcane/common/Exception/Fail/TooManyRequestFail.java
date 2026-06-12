package com.arcane.Arcane.common.Exception.Fail;

public class TooManyRequestFail extends RuntimeException {
    public TooManyRequestFail(String message) {
        super(message);
    }
}
