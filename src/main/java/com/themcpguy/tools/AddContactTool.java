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
import java.util.NoSuchElementException;

public final class AddContactTool {

    private static final Logger log = LoggerFactory.getLogger(AddContactTool.class);

    private static final Duration BACKEND_TIMEOUT = Duration.ofSeconds(10);

    private static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "customerId": {
                  "type": "string",
                  "description": "Id of the customer this person works for, e.g. 'CUST-2'"
                },
                "name": {
                  "type": "string",
                  "description": "The person's name, e.g. 'Dana Wu'"
                },
                "email": {
                  "type": "string",
                  "format": "email",
                  "description": "The person's email address"
                }
              },
              "required": ["customerId", "name", "email"]
            }
            """;

    private final McpJsonMapper jsonMapper;
    private final CustomerRepository repository;

    public AddContactTool(McpJsonMapper jsonMapper, CustomerRepository repository) {
        this.jsonMapper = jsonMapper;
        this.repository = repository;
    }

    public AsyncToolSpecification spec() {
        Tool definition = Tool.builder("add_contact", jsonMapper, SCHEMA)
                .description("""
                        Add a person as a contact at an existing customer, and return the
                        created record. Customers are companies; contacts are the people
                        who work there.
                        Find the customer with search_customers first, because this needs
                        that customer's id. It does not create customers, and it does not
                        check for duplicates: calling it twice adds the same person twice.
                        """)
                .annotations(ToolAnnotations.builder()
                        .readOnlyHint(false)
                        .destructiveHint(false)
                        .idempotentHint(false)
                        .openWorldHint(false)
                        .build())
                .build();

        return AsyncToolSpecification.builder()
                .tool(definition)
                .callHandler((exchange, request) -> add(request.arguments()))
                .build();
    }

    Mono<CallToolResult> add(Map<String, Object> arguments) {
        Object rawId = arguments.get("customerId");
        if (!(rawId instanceof String customerId) || customerId.isBlank()) {
            return Mono.just(Results.error("'customerId' is required and must be a non-blank string"));
        }
        Object rawName = arguments.get("name");
        if (!(rawName instanceof String name) || name.isBlank()) {
            return Mono.just(Results.error("'name' is required and must be a non-blank string"));
        }
        Object rawEmail = arguments.get("email");
        if (!(rawEmail instanceof String email) || email.isBlank()) {
            return Mono.just(Results.error("'email' is required and must be a non-blank string"));
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            return Mono.just(Results.error("'" + email + "' is not a valid email address"));
        }

        return Mono.fromFuture(() -> repository.addContactAsync(customerId.strip(), name.strip(), email.strip()))
                .timeout(BACKEND_TIMEOUT)
                .map(created -> {
                    log.info("add_contact created {} at {}", created.id(), created.customerId());
                    try {
                        return CallToolResult.builder()
                                .addTextContent(jsonMapper.writeValueAsString(created))
                                .build();
                    } catch (IOException e) {
                        return Results.error("Contact was created, but could not be serialised: "
                                + e.getMessage());
                    }
                })
                .onErrorResume(NoSuchElementException.class, e -> Mono.just(Results.error(
                        "No customer with id '" + customerId + "'. Use search_customers to find the right one.")))
                .onErrorResume(e -> {
                    log.error("Unexpected error in add_contact", e);
                    return Mono.just(Results.error("Could not add the contact: " + e.getMessage()));
                });
    }
}
