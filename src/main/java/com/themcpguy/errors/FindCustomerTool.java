package com.themcpguy.errors;

import com.themcpguy.tools.CustomerRepository;
import com.themcpguy.tools.Results;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

/**
 * Finds exactly one customer, which is where the emptiness trap lives: findFirst()
 * gives an Optional, Mono.justOrEmpty turns "absent" into an empty Mono, and an empty
 * Mono means the handler emits nothing at all.
 * <p>
 * Registered twice by ErrorsMcpServer, once with defaultIfEmpty and once without, so
 * the difference is visible on the wire.
 */
public final class FindCustomerTool {

    private static final Logger log = LoggerFactory.getLogger(FindCustomerTool.class);

    private static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "customerId": {
                  "type": "string",
                  "description": "Exact customer id, for example CUST-2"
                }
              },
              "required": ["customerId"]
            }
            """;

    private final McpJsonMapper jsonMapper;
    private final CustomerRepository repository;
    private final boolean handleEmpty;

    public FindCustomerTool(McpJsonMapper jsonMapper, CustomerRepository repository, boolean handleEmpty) {
        this.jsonMapper = jsonMapper;
        this.repository = repository;
        this.handleEmpty = handleEmpty;
    }

    public AsyncToolSpecification spec() {
        String name = handleEmpty ? "find_customer" : "find_customer_broken";
        Tool definition = Tool.builder(name, jsonMapper, SCHEMA)
                .description(handleEmpty
                        ? "Look up one customer by exact id. Reports clearly when there is no such customer."
                        : "The same lookup with no defaultIfEmpty, kept only to show what a silent handler does.")
                .build();

        return AsyncToolSpecification.builder()
                .tool(definition)
                .callHandler((exchange, request) -> find(request.arguments()))
                .build();
    }

    Mono<CallToolResult> find(Map<String, Object> arguments) {
        Object raw = arguments.get("customerId");
        if (!(raw instanceof String customerId) || customerId.isBlank()) {
            return Mono.just(Results.error("'customerId' is required and must be a non-blank string"));
        }

        Mono<CallToolResult> found = Mono.fromFuture(() -> repository.searchAsync(customerId, 1))
                // findFirst() is an Optional, and an absent Optional becomes an EMPTY Mono.
                .flatMap(matches -> Mono.justOrEmpty(matches.stream()
                        .filter(c -> c.id().equalsIgnoreCase(customerId))
                        .findFirst()))
                .map(this::toResult);

        if (handleEmpty) {
            // Without this line the handler completes without emitting, the SDK writes no
            // response, and the client waits until its own timeout expires.
            found = found.defaultIfEmpty(Results.error("No customer with id '" + customerId + "'"));
        }

        return found.onErrorResume(e -> {
            log.error("find_customer failed for {}", customerId, e);
            return Mono.just(Results.error("Could not reach the customer database. " + e.getMessage()));
        });
    }

    private CallToolResult toResult(CustomerRepository.Customer customer) {
        try {
            return CallToolResult.builder()
                    .addTextContent(jsonMapper.writeValueAsString(customer))
                    .build();
        } catch (IOException e) {
            return Results.error("Could not serialise the customer: " + e.getMessage());
        }
    }
}
