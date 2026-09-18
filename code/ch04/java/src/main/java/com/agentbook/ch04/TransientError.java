package com.agentbook.ch04;

public class TransientError extends RuntimeException {
    public TransientError(String message) {
        super(message);
    }
}
