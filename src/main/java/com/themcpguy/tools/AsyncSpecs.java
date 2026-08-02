package com.themcpguy.tools;

import io.modelcontextprotocol.server.McpServerFeatures.AsyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Turns a SyncToolSpecification into an AsyncToolSpecification, so that one
 * McpServer.async(...) can host both kinds of tool.
 * <p>
 * The SDK has its own AsyncToolSpecification.fromSync, but it is package-private,
 * so we build the same thing here. The sync body runs on the bounded-elastic
 * scheduler, which is the pool Reactor reserves for blocking work.
 */
public final class AsyncSpecs {

    private AsyncSpecs() {
    }

    public static AsyncToolSpecification asAsync(SyncToolSpecification sync) {
        var handler = sync.callHandler();
        return AsyncToolSpecification.builder()
                .tool(sync.tool())
                .callHandler((exchange, request) -> Mono
                        .fromCallable(() -> handler.apply(new McpSyncServerExchange(exchange), request))
                        .subscribeOn(Schedulers.boundedElastic()))
                .build();
    }

    /** The same wrapper for prompts, added in Class 5. AsyncPromptSpecification has no builder. */
    public static AsyncPromptSpecification asAsync(SyncPromptSpecification sync) {
        var handler = sync.promptHandler();
        return new AsyncPromptSpecification(sync.prompt(), (exchange, request) -> Mono
                .fromCallable(() -> handler.apply(new McpSyncServerExchange(exchange), request))
                .subscribeOn(Schedulers.boundedElastic()));
    }
}
