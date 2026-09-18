package com.agentbook.ch08;

public record LeaveExtract(boolean ok, LeaveSlots slots, String error) {
    public void validate() {
        if (ok && slots == null) {
            throw new IllegalArgumentException("ok=true 时必须带 slots");
        }
        if (!ok && (error == null || error.isBlank())) {
            throw new IllegalArgumentException("ok=false 时必须带 error");
        }
        if (slots != null) {
            slots.validate();
        }
    }
}
