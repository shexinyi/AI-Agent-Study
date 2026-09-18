package com.agentbook.ch03;

public record StopPolicy(int maxSteps, double timeoutSeconds, int maxSameToolInARow) {
    public static StopPolicy defaults() {
        return new StopPolicy(12, 5.0, 4);
    }
}
