package com.agentbook.ch07;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProductionExecutor {
    private final Map<String, ToolSpec> specs;
    private final Set<String> grantedWrite;
    public final List<AuditEvent> audit = new ArrayList<>();
    private final Map<String, ToolResult> idemCache = new LinkedHashMap<>();

    public ProductionExecutor(Map<String, ToolSpec> specs, Set<String> grantedWrite) {
        this.specs = specs;
        this.grantedWrite = grantedWrite;
    }

    public ToolResult execute(ToolCall call) {
        long started = System.nanoTime();
        ToolSpec spec = specs.get(call.name());
        if (spec == null) {
            return finish(call, null, ToolResult.fail("UNKNOWN_TOOL"), started, false);
        }
        if (("write".equals(spec.risk()) || "money".equals(spec.risk())) && !grantedWrite.contains(call.name())) {
            return finish(call, spec, ToolResult.fail("PERMISSION_DENIED: " + call.name()), started, false);
        }
        for (String key : spec.required()) {
            if (!call.arguments().containsKey(key)) {
                return finish(call, spec, ToolResult.fail("SCHEMA_ERROR: 缺少参数 " + key), started, false);
            }
        }
        String cacheKey = spec.name() + ":" + (call.idempotencyKey() == null ? call.arguments() : call.idempotencyKey());
        if (spec.idempotent() && idemCache.containsKey(cacheKey)) {
            ToolResult cached = idemCache.get(cacheKey);
            return finish(call, spec, new ToolResult(cached.ok(), cached.data(), cached.error(), true), started, true);
        }
        ExecutorService pool = Executors.newSingleThreadExecutor();
        ToolResult result;
        try {
            Map<String, Object> data = pool.submit(() -> spec.handler().apply(call.arguments()))
                    .get((long) (spec.timeoutSeconds() * 1000), TimeUnit.MILLISECONDS);
            result = ToolResult.ok(data);
            if (spec.idempotent()) {
                idemCache.put(cacheKey, result);
            }
        } catch (TimeoutException e) {
            result = ToolResult.fail("TIMEOUT: " + spec.timeoutSeconds() + "s");
        } catch (ExecutionException e) {
            result = ToolResult.fail("HANDLER_ERROR: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = ToolResult.fail("HANDLER_ERROR: interrupted");
        } finally {
            pool.shutdownNow();
        }
        return finish(call, spec, result, started, false);
    }

    private ToolResult finish(ToolCall call, ToolSpec spec, ToolResult result, long started, boolean replayed) {
        long ms = (System.nanoTime() - started) / 1_000_000;
        audit.add(new AuditEvent(
                call.name(),
                result.ok(),
                spec == null ? "unknown" : spec.risk(),
                ms,
                call.idempotencyKey(),
                result.error(),
                replayed || result.replayed()
        ));
        return result;
    }
}
