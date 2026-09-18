package com.agentbook.ch02;

public record StopPolicy(int maxSteps, double timeoutSeconds, int maxSameToolInARow) {
    public static StopPolicy defaults() {
        return new StopPolicy(8, 5.0, 2);
    }
}
