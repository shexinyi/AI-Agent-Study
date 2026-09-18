package com.agentbook.ch08;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParseUtil {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final Pattern FENCE = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```", Pattern.DOTALL);

    private ParseUtil() {
    }

    public static String extractJsonText(String raw) {
        Matcher fenced = FENCE.matcher(raw);
        if (fenced.find()) {
            return fenced.group(1);
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        throw new IllegalArgumentException("输出里找不到 JSON 对象");
    }

    public static LeaveExtract parse(String raw) throws Exception {
        LeaveExtract extracted = MAPPER.readValue(extractJsonText(raw), LeaveExtract.class);
        extracted.validate();
        return extracted;
    }
}
