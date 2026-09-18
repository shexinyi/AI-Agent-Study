package com.agentbook.ch06;

import java.util.Map;

public class PromptContract {
    public static void main(String[] args) {
        String text = "我是林秋，下周一到周三请假去上海办事，帮我走流程。";
        dump(Specs.VAGUE, text);
        dump(Specs.GOOD, text);
    }

    static void dump(PromptSpec spec, String utterance) {
        System.out.println("=".repeat(60));
        System.out.println(spec.name() + " @ " + spec.version());
        System.out.println("=".repeat(60));
        for (Message item : spec.messages(Map.of("utterance", utterance))) {
            System.out.println("[" + item.role() + "]\n" + item.content() + "\n");
        }
    }
}
