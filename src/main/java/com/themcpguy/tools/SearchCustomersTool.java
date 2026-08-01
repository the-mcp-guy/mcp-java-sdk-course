package com.themcpguy.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public final class SearchCustomersTool {

    private static final Logger log = LoggerFactory.getLogger(SearchCustomersTool.class);

    private static final Duration BACKEND_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_LIMIT = 10;

    private static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "query": {
                  "type": "string",
                  "description": "Search term (matches name, email, or customer ID)"
                },
                "limit": {
                  "type": "integer",
                  "description": "Maximum number of results",
                  "minimum": 1,
                  "maximum": 50,
                  "default": 10
                }
              },
              "required": ["query"]
            }
            """;

    private final McpJsonMapper jsonMapper;
    private final CustomerRepository repository;

    public SearchCustomersTool(McpJsonMapper jsonMapper, CustomerRepository repository) {
        this.jsonMapper = jsonMapper;
        this.repository = repository;
    }

    public AsyncToolSpecification spec() {
        Tool definition = Tool.builder("search_customers", jsonMapper, SCHEMA)
                .description("""
                        Search the customer database by name, email, or customer ID.
                        Returns a JSON array of matching customers with id, name, email
                        and accountStatus. Use for customer lookup, not for bulk export:
                        the limit caps at %d.
                        """.formatted(MAX_LIMIT))
                .annotations(ToolAnnotations.builder()
                        .readOnlyHint(true)
                        .openWorldHint(false)
                        .build())
                .build();

        return AsyncToolSpecification.builder()
                .tool(definition)
                .callHandler((exchange, request) -> search(request.arguments()))
                .build();
    }

    Mono<CallToolResult> search(Map<String, Object> arguments) {
        Object raw = arguments.get("query");
        if (!(raw instanceof String query) || query.isBlank()) {
            return Mono.just(Results.error("'query' is required and must be a non-blank string"));
        }

        int limit = arguments.get("limit") instanceof Number n
                ? Math.clamp(n.intValue(), 1, MAX_LIMIT)
                : DEFAULT_LIMIT;

        return Mono.fromFuture(() -> repository.searchAsync(query, limit))
                .timeout(BACKEND_TIMEOUT)
                .map(customers -> {
                    try {
                        return CallToolResult.builder()
                                .addTextContent(jsonMapper.writeValueAsString(customers))
                                .build();
                    } catch (IOException e) {
                        return Results.error("Could not serialise the results: " + e.getMessage());
                    }
                })
                .onErrorResume(TimeoutException.class, e -> Mono.just(
                        Results.error("Backend timed out after " + BACKEND_TIMEOUT.toSeconds() + "s")))
                .onErrorResume(e -> {
                    log.error("Unexpected error in search_customers", e);
                    return Mono.just(Results.error("Internal error: " + e.getMessage()));
                });
    }
}
