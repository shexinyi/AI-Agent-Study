package com.agentbook.ch01;

import java.util.Map;

public record Observation(boolean ok, Map<String, Object> data, String message) {
}
