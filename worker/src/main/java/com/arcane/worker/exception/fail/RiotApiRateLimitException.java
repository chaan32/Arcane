package com.arcane.worker.exception.fail;

public class RiotApiRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public RiotApiRateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
